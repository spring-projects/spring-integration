/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.remote.gateway;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.RemoteFileUtils;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.PartialSuccessException;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Outbound Gateways that perform remote file operations.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public abstract class AbstractRemoteFileOutboundGateway<F> extends AbstractReplyProducingMessageHandler {

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private final Command command;

	private final Set<Option> options = new HashSet<>();

	private final ExpressionEvaluatingMessageProcessor<String> fileNameProcessor;

	private final MessageSessionCallback<F, ?> messageSessionCallback;

	private ExpressionEvaluatingMessageProcessor<String> renameProcessor =
			new ExpressionEvaluatingMessageProcessor<>(
					new FunctionExpression<Message<?>>(m ->
							m.getHeaders().get(FileHeaders.RENAME_TO)));

	private Expression localDirectoryExpression;

	private boolean autoCreateLocalDirectory = true;

	/**
	 * A {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	private FileListFilter<F> filter;

	private boolean filterAfterEnhancement;

	/**
	 * A {@link FileListFilter} that runs against the <em>local</em> file system view when
	 * using MPUT.
	 */
	private FileListFilter<File> mputFilter;

	private Expression localFilenameGeneratorExpression;

	private FileExistsMode fileExistsMode;

	private Integer chmod;

	private boolean remoteFileTemplateExplicitlySet;

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory,
			MessageSessionCallback<F, ?> messageSessionCallback) {

		this(new RemoteFileTemplate<>(sessionFactory), messageSessionCallback);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template and callback
	 * for performing operations on the session.
	 * @param remoteFileTemplate the remote file template.
	 * @param messageSessionCallback the callback.
	 */
	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate,
			MessageSessionCallback<F, ?> messageSessionCallback) {

		Assert.notNull(remoteFileTemplate, "'remoteFileTemplate' cannot be null");
		Assert.notNull(messageSessionCallback, "'messageSessionCallback' cannot be null");
		this.remoteFileTemplate = remoteFileTemplate;
		this.messageSessionCallback = messageSessionCallback;
		this.fileNameProcessor = null;
		this.command = null;
		remoteFileTemplateExplicitlySet(true);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, String command,
			@Nullable String expression) {

		this(sessionFactory, Command.toCommand(command), expression);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, Command command,
			@Nullable String expression) {

		this(new RemoteFileTemplate<>(sessionFactory), command, expression);
		remoteFileTemplateExplicitlySet(false);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate, String command,
			@Nullable String expression) {

		this(remoteFileTemplate, Command.toCommand(command), expression);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expressionArg the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate, Command command,
			@Nullable String expressionArg) {

		Assert.notNull(remoteFileTemplate, "'remoteFileTemplate' cannot be null");
		this.remoteFileTemplate = remoteFileTemplate;
		this.command = command;
		String expression = expressionArg;
		boolean expressionNeeded = !(Command.LS.equals(this.command)
				|| Command.NLST.equals(this.command)
				|| Command.PUT.equals(this.command)
				|| Command.MPUT.equals(this.command));
		if (!StringUtils.hasText(expression) && expressionNeeded) {
			expression = "payload";
		}
		if (!StringUtils.hasText(expression)) {
			this.fileNameProcessor = null;
		}
		else {
			Expression parsedExpression = new SpelExpressionParser().parseExpression(expression);
			this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<>(parsedExpression);
			setPrimaryExpression(parsedExpression);
		}
		this.messageSessionCallback = null;
		remoteFileTemplateExplicitlySet(true);
	}

	protected final void remoteFileTemplateExplicitlySet(boolean remoteFileTemplateExplicitlySet) {
		this.remoteFileTemplateExplicitlySet = remoteFileTemplateExplicitlySet;
	}

	protected void assertRemoteFileTemplateMutability(String propertyName) {
		Assert.state(!this.remoteFileTemplateExplicitlySet,
				() -> "The '" + propertyName + "' must be set on the externally provided: " + this.remoteFileTemplate);
	}

	/**
	 * Specify the options for various gateway commands as a space-delimited string.
	 * @param options the options to set
	 * @see Option
	 */
	public void setOptions(String options) {
		Assert.hasText(options, "'options' must not be empty.");
		this.options.clear();
		Arrays.stream(options.split("\\s"))
				.filter(StringUtils::hasText)
				.map(s -> Option.toOption(s.trim()))
				.forEach(this.options::add);

	}

	/**
	 * Specify the array of options for various gateway commands.
	 * @param options the {@link Option} array to use.
	 * @since 5.0
	 * @see Option
	 */
	public void setOption(Option... options) {
		Assert.notNull(options, "'options' must not be null");
		Assert.noNullElements(options, "'options' cannot contain null element");

		this.options.clear();

		Collections.addAll(this.options, options);
	}

	/**
	 * Set the file separator when dealing with remote files; default '/'.
	 * @param remoteFileSeparator the separator.
	 * @see RemoteFileTemplate#setRemoteFileSeparator(String)
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		assertRemoteFileTemplateMutability("remoteFileSeparator");
		this.remoteFileTemplate.setRemoteFileSeparator(remoteFileSeparator);
	}

	/**
	 * Specify a directory path where remote files will be transferred to.
	 * @param localDirectory the localDirectory to set
	 */
	public void setLocalDirectory(File localDirectory) {
		if (localDirectory != null) {
			this.localDirectoryExpression = new ValueExpression<>(localDirectory);
		}
	}

	/**
	 * Specify a SpEL expression to evaluate the directory path to which remote files will
	 * be transferred.
	 * @param localDirectoryExpression the SpEL to determine the local directory.
	 */
	public void setLocalDirectoryExpression(Expression localDirectoryExpression) {
		this.localDirectoryExpression = localDirectoryExpression;
	}

	/**
	 * Specify a SpEL expression to evaluate the directory path to which remote files will
	 * be transferred.
	 * @param localDirectoryExpression the SpEL to determine the local directory.
	 * @since 5.0
	 */
	public void setLocalDirectoryExpressionString(String localDirectoryExpression) {
		this.localDirectoryExpression = EXPRESSION_PARSER.parseExpression(localDirectoryExpression);
	}

	/**
	 * A {@code boolean} flag to identify if local directory should be created automatically.
	 * Defaults to {@code true}.
	 * @param autoCreateLocalDirectory the autoCreateLocalDirectory to set
	 */
	public void setAutoCreateLocalDirectory(boolean autoCreateLocalDirectory) {
		this.autoCreateLocalDirectory = autoCreateLocalDirectory;
	}

	/**
	 * Set the temporary suffix to use when transferring files to the remote system.
	 * Default {@code .writing}.
	 * @param temporaryFileSuffix the temporaryFileSuffix to set
	 * @see RemoteFileTemplate#setTemporaryFileSuffix(String)
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		assertRemoteFileTemplateMutability("temporaryFileSuffix");
		this.remoteFileTemplate.setTemporaryFileSuffix(temporaryFileSuffix);
	}

	/**
	 * Determine whether the remote directory should automatically be created when
	 * sending files to the remote system.
	 * @param autoCreateDirectory true to create the directory.
	 * @since 5.2
	 * @see RemoteFileTemplate#setAutoCreateDirectory(boolean)
	 */
	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		assertRemoteFileTemplateMutability("autoCreateDirectory");
		this.remoteFileTemplate.setAutoCreateDirectory(autoCreateDirectory);
	}

	/**
	 * Set the remote directory expression used to determine the remote directory to which
	 * files will be sent.
	 * @param remoteDirectoryExpression the remote directory expression.
	 * @since 5.2
	 * @see RemoteFileTemplate#setRemoteDirectoryExpression
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		assertRemoteFileTemplateMutability("remoteDirectoryExpression");
		this.remoteFileTemplate.setRemoteDirectoryExpression(remoteDirectoryExpression);
	}

	/**
	 * Set a temporary remote directory expression; used when transferring files to the remote
	 * system. After a successful transfer the file is renamed using the
	 * {@link #setRemoteDirectoryExpression(Expression) remoteDirectoryExpression}.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression.
	 * @since 5.2
	 * @see RemoteFileTemplate#setTemporaryRemoteDirectoryExpression
	 */
	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		assertRemoteFileTemplateMutability("temporaryRemoteDirectoryExpression");
		this.remoteFileTemplate.setTemporaryRemoteDirectoryExpression(temporaryRemoteDirectoryExpression);
	}

	/**
	 * Set the file name expression to determine the full path to the remote file.
	 * @param fileNameExpression the file name expression.
	 * @since 5.2
	 * @see RemoteFileTemplate#setFileNameExpression
	 */
	public void setFileNameExpression(Expression fileNameExpression) {
		assertRemoteFileTemplateMutability("fileNameExpression");
		this.remoteFileTemplate.setFileNameExpression(fileNameExpression);
	}

	/**
	 * Set whether a temporary file name is used when sending files to the remote system.
	 * @param useTemporaryFileName true to use a temporary file name.
	 * @since 5.2
	 * @see RemoteFileTemplate#setUseTemporaryFileName
	 */
	public void setUseTemporaryFileName(boolean useTemporaryFileName) {
		assertRemoteFileTemplateMutability("useTemporaryFileName");
		this.remoteFileTemplate.setUseTemporaryFileName(useTemporaryFileName);
	}

	/**
	 * Set the file name generator used to generate the remote filename to be used when transferring
	 * files to the remote system.
	 * @param fileNameGenerator the file name generator.
	 * @since 5.2
	 * @see RemoteFileTemplate#setFileNameGenerator
	 */
	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		assertRemoteFileTemplateMutability("fileNameGenerator");
		this.remoteFileTemplate.setFileNameGenerator(fileNameGenerator);
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the
	 * remote file. Default {@code UTF-8}.
	 * @param charset the charset.
	 * @since 5.2
	 * @see RemoteFileTemplate#setCharset
	 */
	public void setCharset(String charset) {
		assertRemoteFileTemplateMutability("charset");
		this.remoteFileTemplate.setCharset(charset);
	}

	/**
	 * Set a {@link FileListFilter} to filter remote files.
	 * @param filter the filter to set
	 */
	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
		this.filterAfterEnhancement = filter != null
				&& filter.isForRecursion()
				&& filter.supportsSingleFileFiltering()
				&& this.options.contains(Option.RECURSIVE);
		if (filter != null && !filter.isForRecursion()) {
			this.logger.warn("When using recursion, you will normally want to set the filter's "
					+ "'forRecursion' property; otherwise files added deep into the "
					+ "directory tree may not be detected");
		}
	}

	/**
	 * A {@link FileListFilter} that runs against the <em>local</em> file system view when
	 * using {@code MPUT} command.
	 * @param filter the filter to set
	 */
	public void setMputFilter(FileListFilter<File> filter) {
		this.mputFilter = filter;
	}

	/**
	 * Specify a SpEL expression for files renaming during transfer.
	 * @param renameExpression the expression to use.
	 * @since 4.3
	 */
	public void setRenameExpression(Expression renameExpression) {
		this.renameProcessor = new ExpressionEvaluatingMessageProcessor<>(renameExpression);
	}

	/**
	 * Specify a SpEL expression for files renaming during transfer.
	 * @param renameExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setRenameExpressionString(String renameExpression) {
		Assert.hasText(renameExpression, "'renameExpression' cannot be empty");
		setRenameExpression(EXPRESSION_PARSER.parseExpression(renameExpression));
	}

	/**
	 * Specify a SpEL expression for local files renaming after downloading.
	 * @param localFilenameGeneratorExpression the expression to use.
	 * @since 3.0
	 */
	public void setLocalFilenameGeneratorExpression(Expression localFilenameGeneratorExpression) {
		Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
		this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
	}

	/**
	 * Specify a SpEL expression for local files renaming after downloading.
	 * @param localFilenameGeneratorExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setLocalFilenameGeneratorExpressionString(String localFilenameGeneratorExpression) {
		Assert.hasText(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be empty");
		this.localFilenameGeneratorExpression = EXPRESSION_PARSER.parseExpression(localFilenameGeneratorExpression);
	}

	/**
	 * Determine the action to take when using GET and MGET operations when the file
	 * already exists locally, or PUT and MPUT when the file exists on the remote
	 * system.
	 * @param fileExistsMode the fileExistsMode to set.
	 * @since 4.2
	 */
	public void setFileExistsMode(FileExistsMode fileExistsMode) {
		this.fileExistsMode = fileExistsMode;
		if (FileExistsMode.APPEND.equals(fileExistsMode)) {
			this.remoteFileTemplate.setUseTemporaryFileName(false);
		}
	}

	/**
	 * String setter for Spring XML convenience.
	 * @param chmod permissions as an octal string e.g "600";
	 * @see #setChmod(int)
	 * @since 4.3
	 */
	public void setChmodOctal(String chmod) {
		Assert.notNull(chmod, "'chmod' cannot be null");
		setChmod(Integer.parseInt(chmod, 8)); // NOSONAR octal radix
	}

	/**
	 * Set the file permissions after uploading, e.g. 0600 for
	 * owner read/write.
	 * @param chmod the permissions.
	 * @since 4.3
	 */
	public void setChmod(int chmod) {
		Assert.isTrue(isChmodCapable(), "chmod operations not supported");
		this.chmod = chmod;
	}

	public boolean isChmodCapable() {
		return false;
	}

	protected final RemoteFileTemplate<F> getRemoteFileTemplate() {
		return this.remoteFileTemplate;
	}

	@Override
	protected void doInit() {
		Assert.state(this.command != null || this.messageSessionCallback != null,
				"'command' or 'messageSessionCallback' must be specified.");
		if (Command.RM.equals(this.command) ||
				Command.GET.equals(this.command)) {
			Assert.isNull(this.filter, "Filters are not supported with the rm and get commands");
		}

		if ((Command.GET.equals(this.command) && !this.options.contains(Option.STREAM))
				|| Command.MGET.equals(this.command)) {
			Assert.notNull(this.localDirectoryExpression, "localDirectory must not be null");
			if (this.localDirectoryExpression instanceof ValueExpression) {
				setupLocalDirectory();
			}
		}

		if (Command.MGET.equals(this.command)) {
			Assert.isTrue(!(this.options.contains(Option.SUBDIRS)),
					() -> "Cannot use " + Option.SUBDIRS.toString() + " when using 'mget' use " +
							Option.RECURSIVE.toString() + " to obtain files in subdirectories");
		}

		populateBeanFactoryIntoComponentsIfAny();
		if (!this.remoteFileTemplateExplicitlySet) {
			this.remoteFileTemplate.afterPropertiesSet();
		}
	}

	private void populateBeanFactoryIntoComponentsIfAny() {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			if (this.fileNameProcessor != null) {
				this.fileNameProcessor.setBeanFactory(beanFactory);
			}
			this.renameProcessor.setBeanFactory(beanFactory);
			this.remoteFileTemplate.setBeanFactory(beanFactory);
		}
	}

	private void setupLocalDirectory() {
		File localDirectory =
				ExpressionUtils.expressionToFile(this.localDirectoryExpression,
						ExpressionUtils.createStandardEvaluationContext(getBeanFactory()), null,
						"localDirectoryExpression");
		if (!localDirectory.exists()) {
			try {
				if (this.autoCreateLocalDirectory) {
					logger.debug(() -> "The '" + localDirectory + "' directory doesn't exist; Will create.");
					if (!localDirectory.mkdirs()) {
						throw new IOException("Failed to make local directory: " + localDirectory);
					}
				}
				else {
					throw new FileNotFoundException(localDirectory.getName());
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}
	}

	@Override
	protected Object handleRequestMessage(final Message<?> requestMessage) {
		if (this.command != null) {
			switch (this.command) {
				case LS:
					return doLs(requestMessage);
				case NLST:
					return doNlst(requestMessage);
				case GET:
					return doGet(requestMessage);
				case MGET:
					return doMget(requestMessage);
				case RM:
					return doRm(requestMessage);
				case MV:
					return doMv(requestMessage);
				case PUT:
					return doPut(requestMessage);
				case MPUT:
					return doMput(requestMessage);
			}
		}
		return this.remoteFileTemplate.execute(session ->
				AbstractRemoteFileOutboundGateway.this.messageSessionCallback.doInSession(session, requestMessage));
	}

	private Object doLs(Message<?> requestMessage) {
		String dir = obtainRemoteDir(requestMessage);
		return this.remoteFileTemplate.execute(session -> {
			List<?> payload = ls(requestMessage, session, dir);
			return getMessageBuilderFactory()
					.withPayload(payload)
					.setHeader(FileHeaders.REMOTE_DIRECTORY, dir)
					.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort());
		});

	}

	private Object doNlst(Message<?> requestMessage) {
		String dir = obtainRemoteDir(requestMessage);
		return this.remoteFileTemplate.execute(session -> {
			List<?> payload = nlst(requestMessage, session, dir);
			return getMessageBuilderFactory()
					.withPayload(payload)
					.setHeader(FileHeaders.REMOTE_DIRECTORY, dir)
					.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort());
		});
	}

	private String obtainRemoteDir(Message<?> requestMessage) {
		String dir = this.fileNameProcessor != null
				? this.fileNameProcessor.processMessage(requestMessage)
				: null;
		if (dir != null && !dir.endsWith(this.remoteFileTemplate.getRemoteFileSeparator())) {
			dir += this.remoteFileTemplate.getRemoteFileSeparator();
		}
		return dir;
	}

	/**
	 * List remote files names for the provided directory.
	 * The message can be consulted for some context related to the current request;
	 * isn't used in the default implementation.
	 * @param message the message related to the current request
	 * @param session the session to perform list file names command
	 * @param dir the remote directory to list file names
	 * @return the list of file/directory names in the provided dir
	 * @throws IOException the IO exception during performing remote command
	 * @since 5.0
	 */
	protected List<String> nlst(Message<?> message, Session<F> session, String dir) throws IOException {
		String remoteDirectory = buildRemotePath(dir, "");
		List<String> fileNames = Arrays.asList(session.listNames(remoteDirectory));
		if (!this.options.contains(Option.NOSORT)) {
			Collections.sort(fileNames);
		}
		return fileNames;
	}

	private Object doGet(final Message<?> requestMessage) {
		String remoteFilePath = obtainRemoteFilePath(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);
		Session<F> session;
		Object payload;
		if (this.options.contains(Option.STREAM)) {
			session = this.remoteFileTemplate.getSessionFactory().getSession();
			try {
				payload = session.readRaw(remoteFilePath);
				return getMessageBuilderFactory()
						.withPayload(payload)
						.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
						.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
						.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort())
						.setHeader(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, session);
			}
			catch (IOException e) {
				throw new MessageHandlingException(requestMessage,
						"Error handling message in the [" + this
								+ "]. Failed to get the remote file [" + remoteFilePath + "] as a stream", e);
			}
		}
		else {
			return this.remoteFileTemplate.execute(session1 -> {
				Object getPayload = get(requestMessage, session1, remoteDir, remoteFilePath, remoteFilename, null);
				return getMessageBuilderFactory()
						.withPayload(getPayload)
						.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
						.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
						.setHeader(FileHeaders.REMOTE_HOST_PORT, session1.getHostPort());
			});
		}
	}

	private Object doMget(final Message<?> requestMessage) {
		String remoteFilePath = obtainRemoteFilePath(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);
		return this.remoteFileTemplate.execute(session -> {
					List<File> payload = mGet(requestMessage, session, remoteDir, remoteFilename);
					return getMessageBuilderFactory()
							.withPayload(payload)
							.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
							.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
							.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort());
				}
		);
	}

	private Object doRm(Message<?> requestMessage) {
		String remoteFilePath = obtainRemoteFilePath(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);

		return this.remoteFileTemplate.execute(session -> {
			boolean payload = rm(requestMessage, session, remoteFilePath);
			return getMessageBuilderFactory()
					.withPayload(payload)
					.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
					.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
					.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort());
		});
	}

	/**
	 * Perform remote delete for the provided path.
	 * The message can be consulted to determine some context;
	 * isn't used in the default implementation.
	 * @param message the request message related to the path to remove
	 * @param session the remote protocol session to perform remove command
	 * @param remoteFilePath the remote path to remove
	 * @return true or false as a result of the remote removal
	 * @throws IOException the IO exception during performing remote command
	 * @since 5.0
	 */
	protected boolean rm(Message<?> message, Session<F> session, String remoteFilePath) throws IOException {
		return session.remove(remoteFilePath);
	}

	private Object doMv(Message<?> requestMessage) {
		String remoteFilePath = obtainRemoteFilePath(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);
		String remoteFileNewPath = this.renameProcessor.processMessage(requestMessage);
		Assert.hasLength(remoteFileNewPath, "New filename cannot be empty");

		return this.remoteFileTemplate.execute(session -> {
					Boolean result = mv(requestMessage, session, remoteFilePath, remoteFileNewPath);
					return getMessageBuilderFactory()
							.withPayload(result)
							.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
							.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
							.setHeader(FileHeaders.RENAME_TO, remoteFileNewPath)
							.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort());
				}
		);
	}

	private String obtainRemoteFilePath(Message<?> requestMessage) {
		String remoteFilePath = this.fileNameProcessor.processMessage(requestMessage);
		Assert.state(remoteFilePath != null,
				() -> "The 'fileNameProcessor' evaluated to null 'remoteFilePath' from message: " + requestMessage);
		return remoteFilePath;
	}

	/**
	 * Move one remote path to another.
	 * The message can be consulted to determine some context;
	 * isn't used in the default implementation.
	 * @param message the request message related to this move command
	 * @param session the remote protocol session to perform move command
	 * @param remoteFilePath the source remote path
	 * @param remoteFileNewPath the target remote path
	 * @return true or false as a result of the operation
	 * @throws IOException the IO exception during performing remote command
	 * @since 5.0
	 */
	protected boolean mv(Message<?> message, Session<F> session, String remoteFilePath, String remoteFileNewPath)
			throws IOException {

		int lastSeparator = remoteFileNewPath.lastIndexOf(this.remoteFileTemplate.getRemoteFileSeparator());
		if (lastSeparator > 0) {
			String remoteFileDirectory = remoteFileNewPath.substring(0, lastSeparator + 1);
			RemoteFileUtils.makeDirectories(remoteFileDirectory, session,
					this.remoteFileTemplate.getRemoteFileSeparator(), this.logger.getLog());
		}
		session.rename(remoteFilePath, remoteFileNewPath);
		return true;
	}

	private String doPut(Message<?> requestMessage) {
		return doPut(requestMessage, null);
	}

	private String doPut(Message<?> requestMessage, String subDirectory) {
		return this.remoteFileTemplate.invoke(template ->
				put(requestMessage, template.getSession(), subDirectory));
	}

	/**
	 * Put the file based on the message to the remote server.
	 * The message can be consulted to determine some context.
	 * The session argument isn't used in the default implementation.
	 * @param message the request message related to this put command
	 * @param session the remote protocol session related to this invocation context
	 * @param subDirectory the target sub directory to put
	 * @return The remote path, or null if no local file was found.
	 * @since 5.0
	 */
	protected String put(Message<?> message, Session<F> session, String subDirectory) {
		String path = this.remoteFileTemplate.send(message, subDirectory, this.fileExistsMode);
		if (path == null) {
			throw new MessagingException(message, "No local file found for " + message);
		}
		if (this.chmod != null && isChmodCapable()) {
			doChmod(this.remoteFileTemplate, path, this.chmod);
		}
		return path;

	}

	/**
	 * Set the mode on the remote file after transfer; the default implementation does
	 * nothing.
	 * @param remoteFileOperations the remote file template.
	 * @param path the path.
	 * @param chmodToSet the chmod to set.
	 * @since 4.3
	 */
	protected void doChmod(RemoteFileOperations<F> remoteFileOperations, String path, int chmodToSet) {
		// no-op
	}

	private Object doMput(Message<?> requestMessage) {
		File file = null;
		Object payload = requestMessage.getPayload();
		if (payload instanceof File) {
			file = (File) payload;
		}
		else if (payload instanceof String) {
			file = new File((String) payload);
		}
		else if (!(payload instanceof Collection)) {
			throw new IllegalArgumentException(
					"Only File or String payloads (or Collection of File/String) allowed for 'mput', received: "
							+ payload.getClass());
		}
		if ((payload instanceof Collection)) {
			return ((Collection<?>) payload).stream()
					.map(p -> doMput(new MutableMessage<>(p, requestMessage.getHeaders())))
					.collect(Collectors.toList());
		}
		else if (!file.isDirectory()) {
			return doPut(requestMessage);
		}
		else {
			File localDir = file;
			return this.remoteFileTemplate.invoke(t -> mPut(requestMessage, t.getSession(), localDir));
		}
	}

	/**
	 * Put files from the provided directory to the remote server recursively.
	 * The message can be consulted to determine some context.
	 * The session argument isn't used in the default implementation.
	 * @param message the request message related to this mPut command
	 * @param session the remote protocol session for this invocation context
	 * @param localDir the local directory to mput to the server
	 * @return The list of remote paths for sent files
	 * @since 5.0
	 */
	protected List<String> mPut(Message<?> message, Session<F> session, File localDir) {
		return putLocalDirectory(message, localDir, null);
	}

	private List<String> putLocalDirectory(Message<?> requestMessage, File file, String subDirectory) {
		List<File> filteredFiles = filterMputFiles(file.listFiles());
		List<String> replies = new ArrayList<>();
		try {
			for (File filteredFile : filteredFiles) {
				if (!filteredFile.isDirectory()) {
					String path = doPut(new MutableMessage<>(filteredFile, requestMessage.getHeaders()), subDirectory);
					if (path != null) {
						replies.add(path);
					}
					else {
						logger.debug(() ->
								"File " + filteredFile.getAbsolutePath() + " removed before transfer; ignoring");
					}
				}
				else if (this.options.contains(Option.RECURSIVE)) {
					String newSubDirectory =
							(StringUtils.hasText(subDirectory) ?
									subDirectory + this.remoteFileTemplate.getRemoteFileSeparator()
									: "") + filteredFile.getName();
					replies.addAll(putLocalDirectory(requestMessage, filteredFile, newSubDirectory));
				}
			}
		}
		catch (RuntimeException ex) {
			throw handlePutException(requestMessage, subDirectory, filteredFiles, replies, ex);
		}
		return replies;
	}

	private RuntimeException handlePutException(Message<?> requestMessage, String subDirectory,
			List<File> filteredFiles, List<String> replies, RuntimeException ex) {

		if (replies.size() > 0 || ex instanceof PartialSuccessException) {
			return new PartialSuccessException(requestMessage,
					"Partially successful 'mput' operation" +
							(subDirectory == null ? "" : (" on " + subDirectory)), ex, replies, filteredFiles);
		}
		else {
			return ex;
		}
	}

	/**
	 * List remote files to local representation.
	 * The message can be consulted for some context for the current request;
	 * isn't used in the default implementation.
	 * @param message the message related to the list request
	 * @param session the session to perform list command
	 * @param dir the remote directory to list content
	 * @return the list of remote files
	 * @throws IOException the IO exception during performing remote command
	 */
	protected List<?> ls(Message<?> message, Session<F> session, String dir) throws IOException {
		List<F> lsFiles = listFilesInRemoteDir(session, dir, "");
		if (!this.options.contains(Option.LINKS)) {
			purgeLinks(lsFiles);
		}
		if (!this.options.contains(Option.ALL)) {
			purgeDots(lsFiles);
		}
		if (this.options.contains(Option.NAME_ONLY)) {
			List<String> results = new ArrayList<>();
			for (F file : lsFiles) {
				results.add(getFilename(file));
			}
			if (!this.options.contains(Option.NOSORT)) {
				Collections.sort(results);
			}
			return results;
		}
		else {
			List<AbstractFileInfo<F>> canonicalFiles = this.asFileInfoList(lsFiles);
			for (AbstractFileInfo<F> file : canonicalFiles) {
				file.setRemoteDirectory(dir);
			}
			if (!this.options.contains(Option.NOSORT)) {
				Collections.sort(canonicalFiles);
			}
			return canonicalFiles;
		}
	}

	private List<F> listFilesInRemoteDir(Session<F> session, String directory, String subDirectory)
			throws IOException {

		List<F> lsFiles = new ArrayList<>();
		String remoteDirectory = buildRemotePath(directory, subDirectory);

		F[] list = session.list(remoteDirectory);
		List<F> files;
		if (!this.filterAfterEnhancement) {
			files = filterFiles(list);
		}
		else {
			files = Arrays.asList(list);
		}
		if (!ObjectUtils.isEmpty(files)) {
			for (F file : files) {
				if (file != null) {
					processFile(session, directory, subDirectory, lsFiles, this.options.contains(Option.RECURSIVE),
							file);
				}
			}
		}
		return lsFiles;
	}

	private String buildRemotePath(String parent, String child) {
		String remotePath = null;
		if (parent != null) {
			remotePath = (parent + child);
		}
		else if (StringUtils.hasText(child)) {
			remotePath = '.' + this.remoteFileTemplate.getRemoteFileSeparator() + child;
		}
		return remotePath;
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	protected final F filterFile(F file) {
		if (this.filter.accept(file)) {
			return file;
		}
		else {
			return null;
		}
	}

	private void processFile(Session<F> session, String directory, String subDirectory, // NOSONAR - complexity
			List<F> lsFiles, boolean recursion, F file) throws IOException {

		F fileToAdd = file;
		if (recursion && StringUtils.hasText(subDirectory)) {
			fileToAdd = enhanceNameWithSubDirectory(file, subDirectory);
		}
		if (this.filterAfterEnhancement && !this.filter.accept(fileToAdd)) {
			return;
		}
		String fileName = getFilename(fileToAdd);
		final boolean isDirectory = isDirectory(file);
		boolean isDots = hasDots(fileName);
		if ((this.options.contains(Option.SUBDIRS) || !isDirectory)
				&& (!isDots || this.options.contains(Option.ALL))) {

			lsFiles.add(fileToAdd);
		}

		if (recursion && isDirectory && !isDots) {
			lsFiles.addAll(listFilesInRemoteDir(session, directory,
					fileName + this.remoteFileTemplate.getRemoteFileSeparator()));
		}
	}

	private boolean hasDots(String fileName) {
		String fileSeparator = this.remoteFileTemplate.getRemoteFileSeparator();
		return ".".equals(fileName)
				|| "..".equals(fileName)
				|| fileName.endsWith(fileSeparator + ".")
				|| fileName.endsWith(fileSeparator + "..");
	}

	protected final List<File> filterMputFiles(File[] files) {
		if (files == null) {
			return Collections.emptyList();
		}
		return (this.mputFilter != null) ? this.mputFilter.filterFiles(files) : Arrays.asList(files);
	}

	protected void purgeLinks(List<F> lsFiles) {
		lsFiles.removeIf(this::isLink);
	}

	protected void purgeDots(List<F> lsFiles) {
		lsFiles.removeIf(f -> getFilename(f).startsWith("."));
	}

	/**
	 * Copy a remote file to the configured local directory.
	 * @param message the message.
	 * @param session the session.
	 * @param remoteDir the remote directory.
	 * @param remoteFilePath the remote file path.
	 * @param remoteFilename the remote file name.
	 * @param fileInfoParam the remote file info; if null we will execute an 'ls' command
	 * first.
	 * @return The file.
	 * @throws IOException Any IOException.
	 */
	protected File get(Message<?> message, Session<F> session, String remoteDir, // NOSONAR complexity
			String remoteFilePath, String remoteFilename, F fileInfoParam) throws IOException {

		F fileInfo = fileInfoParam;
		if (fileInfo == null) {
			F[] files = session.list(remoteFilePath);
			if (files == null) {
				throw new MessagingException("Session returned null when listing " + remoteFilePath);
			}
			if (files.length != 1 || files[0] == null || isDirectory(files[0]) || isLink(files[0])) {
				throw new MessagingException(remoteFilePath + " is not a file");
			}
			fileInfo = files[0];
		}
		final File localFile =
				new File(generateLocalDirectory(message, remoteDir), generateLocalFileName(message, remoteFilename));
		FileExistsMode existsMode = this.fileExistsMode;
		boolean appending = FileExistsMode.APPEND.equals(existsMode);
		boolean exists = localFile.exists();
		boolean replacing = FileExistsMode.REPLACE.equals(existsMode)
				|| (exists && FileExistsMode.REPLACE_IF_MODIFIED.equals(existsMode)
				&& localFile.lastModified() != getModified(fileInfo));
		if (!exists || appending || replacing) {
			OutputStream outputStream;
			String tempFileName = localFile.getAbsolutePath() + this.remoteFileTemplate.getTemporaryFileSuffix();
			File tempFile = new File(tempFileName);
			if (appending) {
				outputStream = new BufferedOutputStream(new FileOutputStream(localFile, true));
			}
			else {
				outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			}
			if (replacing && !localFile.delete()) {
				this.logger.warn(() -> "Failed to delete " + localFile);
			}
			try {
				session.read(remoteFilePath, outputStream);
			}
			catch (Exception ex) {
				/* Some operation systems acquire exclusive file-lock during file processing
				   and the file can't be deleted without closing streams before.
				*/
				outputStream.close();
				if (!tempFile.delete()) {
					this.logger.warn(() -> "Failed to delete tempFile " + tempFile);
				}

				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
						() -> "Failure occurred while copying from remote to local directory", ex);
			}
			finally {
				try {
					outputStream.close();
				}
				catch (@SuppressWarnings("unused") Exception ignored2) {
					//Ignore it
				}
			}
			if (!appending && !tempFile.renameTo(localFile)) {
				throw new MessagingException("Failed to rename local file");
			}
			if ((this.options.contains(Option.PRESERVE_TIMESTAMP)
					|| FileExistsMode.REPLACE_IF_MODIFIED.equals(existsMode))
					&& (!localFile.setLastModified(getModified(fileInfo)))) {

				logger.warn(() -> "Failed to set lastModified on " + localFile);
			}
			if (this.options.contains(Option.DELETE)) {
				boolean result = session.remove(remoteFilePath);
				if (!result) {
					logger.error("Failed to delete: " + remoteFilePath);
				}
				else {
					logger.debug(() -> remoteFilePath + " deleted");
				}
			}
		}
		else if (FileExistsMode.REPLACE_IF_MODIFIED.equals(existsMode)) {
			logger.debug(() -> "Local file '" + localFile + "' has the same modified timestamp, ignored");
			if (this.command.equals(Command.MGET)) {
				return null;
			}
		}
		else if (!FileExistsMode.IGNORE.equals(existsMode)) {
			throw new MessageHandlingException(message,
					"Error handling message in the [" + this + "]. Local file " + localFile + " already exists");
		}
		else {
			logger.debug(() -> "Existing file skipped: " + localFile);
			if (this.command.equals(Command.MGET)) {
				return null;
			}
		}
		return localFile;
	}

	protected List<File> mGet(Message<?> message, Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {

		if (this.options.contains(Option.RECURSIVE)) {
			if (!("*".equals(remoteFilename))) {
				logger.warn("File name pattern must be '*' when using recursion");
			}
			this.options.remove(Option.NAME_ONLY);
			return mGetWithRecursion(message, session, remoteDirectory, remoteFilename);
		}
		else {
			return mGetWithoutRecursion(message, session, remoteDirectory, remoteFilename);
		}
	}

	private List<File> mGetWithoutRecursion(Message<?> message, Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {

		List<File> files = new ArrayList<>();
		String remotePath = buildRemotePath(remoteDirectory, remoteFilename);
		List<AbstractFileInfo<F>> remoteFiles = lsRemoteFilesForMget(message, session, remoteDirectory,
				remoteFilename, remotePath);
		try {
			for (AbstractFileInfo<F> lsEntry : remoteFiles) {
				if (lsEntry.isDirectory()) {
					continue;
				}
				File file = getRemoteFileForMget(message, session, remoteDirectory, lsEntry);
				if (file != null) {
					files.add(file);
				}
			}
		}
		catch (Exception ex) {
			throw processMgetException(message, remoteDirectory, files, remoteFiles, ex);
		}
		return files;
	}

	private RuntimeException processMgetException(Message<?> message, String remoteDirectory, List<File> files,
			List<AbstractFileInfo<F>> remoteFiles, Exception ex) {

		if (files.size() > 0) {
			return new PartialSuccessException(message,
					"Partially successful recursive 'mget' operation on "
							+ (remoteDirectory != null ? remoteDirectory : "Client Working Directory"),
					ex, files, remoteFiles);
		}
		else if (ex instanceof MessagingException) {
			return (MessagingException) ex;
		}
		else if (ex instanceof IOException) {
			throw new UncheckedIOException((IOException) ex);
		}
		else {
			return new MessagingException("Failed to process MGET", ex);
		}
	}

	private List<File> mGetWithRecursion(Message<?> message, Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {

		List<File> files = new ArrayList<>();
		List<AbstractFileInfo<F>> fileNames = lsRemoteFilesForMget(message, session, remoteDirectory,
				remoteFilename, remoteDirectory);
		try {
			for (AbstractFileInfo<F> lsEntry : fileNames) {
				File file = getRemoteFileForMget(message, session, remoteDirectory, lsEntry);
				if (file != null) {
					files.add(file);
				}
			}
		}
		catch (Exception ex) {
			throw processMgetException(message, remoteDirectory, files, fileNames, ex);
		}
		return files;
	}

	private List<AbstractFileInfo<F>> lsRemoteFilesForMget(Message<?> message, Session<F> session,
			String remoteDirectory, String remoteFilename, String remotePath) throws IOException {

		@SuppressWarnings("unchecked")
		List<AbstractFileInfo<F>> remoteFiles = (List<AbstractFileInfo<F>>) ls(message, session, remotePath);
		if (remoteFiles.size() == 0 && this.options.contains(Option.EXCEPTION_WHEN_EMPTY)) {
			throw new MessagingException("No files found at "
					+ (remoteDirectory != null ? remoteDirectory : "Client Working Directory")
					+ " with pattern " + remoteFilename);
		}
		return remoteFiles;
	}

	private File getRemoteFileForMget(Message<?> message, Session<F> session, String remoteDirectory,
			AbstractFileInfo<F> lsEntry) throws IOException {

		String fullFileName =
				remoteDirectory != null
						? remoteDirectory + getFilename(lsEntry)
						: getFilename(lsEntry);
		/*
		 * With recursion, the filename might contain subdirectory information
		 * normalize each file separately.
		 */
		String fileName = getRemoteFilename(fullFileName);
		String actualRemoteDirectory = getRemoteDirectory(fullFileName, fileName);
		return get(message, session, actualRemoteDirectory, fullFileName, fileName, lsEntry.getFileInfo());
	}

	private String getRemoteDirectory(String remoteFilePath, String remoteFilename) {
		String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf(remoteFilename));
		if (remoteDir.length() == 0) {
			return null;
		}
		return remoteDir;
	}

	/**
	 * @param remoteFilePath The remote file path.
	 * @return The remote file name.
	 */
	protected String getRemoteFilename(String remoteFilePath) {
		int index = remoteFilePath.lastIndexOf(this.remoteFileTemplate.getRemoteFileSeparator());
		if (index < 0) {
			return remoteFilePath;
		}
		else {
			return remoteFilePath.substring(index + 1);
		}
	}

	private File generateLocalDirectory(Message<?> message, String remoteDirectory) {
		EvaluationContext evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (remoteDirectory != null) {
			evaluationContext.setVariable("remoteDirectory", remoteDirectory);
		}
		File localDir = ExpressionUtils.expressionToFile(this.localDirectoryExpression, evaluationContext, message,
				"Local Directory");
		if (!localDir.exists()) {
			Assert.isTrue(localDir.mkdirs(), () -> "Failed to make local directory: " + localDir);
		}
		return localDir;
	}

	private String generateLocalFileName(Message<?> message, String remoteFileName) {
		if (this.localFilenameGeneratorExpression != null) {
			EvaluationContext evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
			evaluationContext.setVariable("remoteFileName", remoteFileName);
			return this.localFilenameGeneratorExpression.getValue(evaluationContext, message, String.class);
		}
		return remoteFileName;
	}

	protected abstract boolean isDirectory(F file);

	protected abstract boolean isLink(F file);

	protected abstract String getFilename(F file);

	protected abstract String getFilename(AbstractFileInfo<F> file);

	protected abstract long getModified(F file);

	protected abstract List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

	protected abstract F enhanceNameWithSubDirectory(F file, String directory);

	/**
	 * Enumeration of commands supported by the gateways.
	 */
	public enum Command {

		/**
		 * (ls) List remote files.
		 */
		LS("ls"),

		/**
		 * (nlst) List remote file names.
		 */
		NLST("nlst"),

		/**
		 * (get) Retrieve a remote file.
		 */
		GET("get"),

		/**
		 * (rm) Remove a remote file (path - including wildcards).
		 */
		RM("rm"),

		/**
		 * (mget) Retrieve multiple files matching a wildcard path.
		 */
		MGET("mget"),

		/**
		 * (mv) Move (rename) a remote file.
		 */
		MV("mv"),

		/**
		 * (put) Put a local file to the remote system.
		 */
		PUT("put"),

		/**
		 * (mput) Put multiple local files to the remote system.
		 */
		MPUT("mput");

		private final String command;

		Command(String command) {
			this.command = command;
		}

		public String getCommand() {
			return this.command;
		}

		public static Command toCommand(String cmd) {
			for (Command command : values()) {
				if (command.getCommand().equals(cmd)) {
					return command;
				}
			}
			throw new IllegalArgumentException("No Command with value '" + cmd + "'");
		}

	}

	/**
	 * Enumeration of options supported by various commands.
	 *
	 */
	public enum Option {

		/**
		 * (-1) Don't return full file information; just the name (ls).
		 */
		NAME_ONLY("-1"),

		/**
		 * (-a) Include files beginning with {@code .}, including directories {@code .}
		 * and {@code ..} in the results (ls).
		 */
		ALL("-a"),

		/**
		 * (-f) Do not sort the results (ls with NAME_ONLY).
		 */
		NOSORT("-f"),

		/**
		 * (-dirs) Include directories in the results (ls).
		 */
		SUBDIRS("-dirs"),

		/**
		 * (-links) Include links in the results (ls).
		 */
		LINKS("-links"),

		/**
		 * (-P) Preserve the server timestamp (get, mget).
		 */
		PRESERVE_TIMESTAMP("-P"),

		/**
		 * (-x) Throw an exception if no files returned (mget).
		 */
		EXCEPTION_WHEN_EMPTY("-x"),

		/**
		 * (-R) Recursive (ls, mget)
		 */
		RECURSIVE("-R"),

		/**
		 * (-stream) Streaming 'get' (returns InputStream); user must call {@link Session#close()}.
		 */
		STREAM("-stream"),

		/**
		 * (-D) Delete the remote file after successful transfer (get, mget).
		 */
		DELETE("-D");

		private final String option;

		Option(String option) {
			this.option = option;
		}

		public String getOption() {
			return this.option;
		}

		public static Option toOption(String opt) {
			for (Option option : values()) {
				if (option.getOption().equals(opt)) {
					return option;
				}
			}
			throw new IllegalArgumentException("No option with value '" + opt + "'");
		}

	}

}
