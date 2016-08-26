/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.PartialSuccessException;
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
 * @since 2.1
 */
public abstract class AbstractRemoteFileOutboundGateway<F> extends AbstractReplyProducingMessageHandler {

	protected final RemoteFileTemplate<F> remoteFileTemplate;

	protected final Command command;

	/**
	 * Enumeration of commands supported by the gateways.
	 */
	public enum Command {

		/**
		 * List remote files.
		 */
		LS("ls"),

		/**
		 * Retrieve a remote file.
		 */
		GET("get"),

		/**
		 * Remove a remote file (path - including wildcards).
		 */
		RM("rm"),

		/**
		 * Retrieve multiple files matching a wildcard path.
		 */
		MGET("mget"),

		/**
		 * Move (rename) a remote file.
		 */
		MV("mv"),

		/**
		 * Put a local file to the remote system.
		 */
		PUT("put"),

		/**
		 * Put multiple local files to the remote system.
		 */
		MPUT("mput");

		private String command;

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
		 * Don't return full file information; just the name (ls).
		 */
		NAME_ONLY("-1"),

		/**
		 * Include files beginning with {@code .}, including directories {@code .} and {@code ..} in the results (ls).
		 */
		ALL("-a"),

		/**
		 * Do not sort the results (ls with NAME_ONLY).
		 */
		NOSORT("-f"),

		/**
		 * Include directories in the results (ls).
		 */
		SUBDIRS("-dirs"),

		/**
		 * Include links in the results (ls).
		 */
		LINKS("-links"),

		/**
		 * Preserve the server timestamp (get, mget).
		 */
		PRESERVE_TIMESTAMP("-P"),

		/**
		 * Throw an exception if no files returned (mget).
		 */
		EXCEPTION_WHEN_EMPTY("-x"),

		/**
		 * Recursive (ls, mget)
		 */
		RECURSIVE("-R"),

		/**
		 * Streaming 'get' (returns InputStream); user must call {@link Session#close()}.
		 */
		STREAM("-stream");

		private String option;

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

	private final ExpressionEvaluatingMessageProcessor<String> fileNameProcessor;

	private final MessageSessionCallback<F, ?> messageSessionCallback;

	private volatile ExpressionEvaluatingMessageProcessor<String> renameProcessor =
			new ExpressionEvaluatingMessageProcessor<String>(
					new SpelExpressionParser().parseExpression("headers." + FileHeaders.RENAME_TO));

	protected volatile Set<Option> options = new HashSet<Option>();

	private volatile Expression localDirectoryExpression;

	private volatile boolean autoCreateLocalDirectory = true;

	/**
	 * A {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	private volatile FileListFilter<F> filter;

	/**
	 * A {@link FileListFilter} that runs against the <em>local</em> file system view when
	 * using MPUT.
	 */
	private volatile FileListFilter<File> mputFilter;

	private volatile Expression localFilenameGeneratorExpression;

	private volatile FileExistsMode fileExistsMode;

	private volatile Integer chmod;

	/**
	 * Construct an instance using the provided session factory and callback for
	 * performing operations on the session.
	 * @param sessionFactory the session factory.
	 * @param messageSessionCallback the callback.
	 */
	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory,
			MessageSessionCallback<F, ?> messageSessionCallback) {
		this(new RemoteFileTemplate<F>(sessionFactory), messageSessionCallback);
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
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, String command,
			String expression) {
		this(sessionFactory, Command.toCommand(command), expression);
	}

	/**
	 * Construct an instance with the supplied session factory, a command ('ls', 'get'
	 * etc), and an expression to determine the filename.
	 * @param sessionFactory the session factory.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, Command command, String expression) {
		this(new RemoteFileTemplate<F>(sessionFactory), command, expression);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate, String command,
			String expression) {
		this(remoteFileTemplate, Command.toCommand(command), expression);
	}

	/**
	 * Construct an instance with the supplied remote file template, a command ('ls',
	 * 'get' etc), and an expression to determine the filename.
	 * @param remoteFileTemplate the remote file template.
	 * @param command the command.
	 * @param expression the filename expression.
	 */
	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate, Command command,
			String expression) {
		Assert.notNull(remoteFileTemplate, "'remoteFileTemplate' cannot be null");
		this.remoteFileTemplate = remoteFileTemplate;
		this.command = command;
		Expression parsedExpression = new SpelExpressionParser().parseExpression(expression);
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
			parsedExpression);
		this.messageSessionCallback = null;
		setPrimaryExpression(parsedExpression);
	}

	/**
	 * @param options the options to set
	 */
	public void setOptions(String options) {
		String[] opts = options.split("\\s");
		for (String opt : opts) {
			String trimmedOpt = opt.trim();
			if (StringUtils.hasLength(trimmedOpt)) {
				this.options.add(Option.toOption(trimmedOpt));
			}
		}
	}

	/**
	 * @param remoteFileSeparator the remoteFileSeparator to set
	 * @see RemoteFileTemplate#setRemoteFileSeparator(String)
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		this.remoteFileTemplate.setRemoteFileSeparator(remoteFileSeparator);
	}

	/**
	 * @param localDirectory the localDirectory to set
	 */
	public void setLocalDirectory(File localDirectory) {
		if (localDirectory != null) {
			this.localDirectoryExpression = new LiteralExpression(localDirectory.getAbsolutePath());
		}
	}

	public void setLocalDirectoryExpression(Expression localDirectoryExpression) {
		this.localDirectoryExpression = localDirectoryExpression;
	}

	/**
	 * @param autoCreateLocalDirectory the autoCreateLocalDirectory to set
	 */
	public void setAutoCreateLocalDirectory(boolean autoCreateLocalDirectory) {
		this.autoCreateLocalDirectory = autoCreateLocalDirectory;
	}

	/**
	 * @param temporaryFileSuffix the temporaryFileSuffix to set
	 * @see RemoteFileTemplate#setTemporaryFileSuffix(String)
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.remoteFileTemplate.setTemporaryFileSuffix(temporaryFileSuffix);
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setMputFilter(FileListFilter<File> filter) {
		this.mputFilter = filter;
	}

	/**
	 * @param renameExpression the expression to use.
	 * @since 4.3
	 */
	public void setRenameExpression(Expression renameExpression) {
		this.renameProcessor = new ExpressionEvaluatingMessageProcessor<String>(renameExpression);
	}

	/**
	 * @param renameExpression the String in SpEL syntax.
	 * @since 4.3
	 */
	public void setRenameExpressionString(String renameExpression) {
		Assert.hasText(renameExpression, "'renameExpression' cannot be empty");
		setRenameExpression(EXPRESSION_PARSER.parseExpression(renameExpression));
	}

	/**
	 * @param localFilenameGeneratorExpression the expression to use.
	 * @since 3.0
	 */
	public void setLocalFilenameGeneratorExpression(Expression localFilenameGeneratorExpression) {
		Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
		this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
	}

	/**
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
		setChmod(Integer.parseInt(chmod, 8));
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
			if (this.localDirectoryExpression instanceof LiteralExpression) {
				File localDirectory = new File(this.localDirectoryExpression.getExpressionString());
				try {
					if (!localDirectory.exists()) {
						if (this.autoCreateLocalDirectory) {
							if (logger.isDebugEnabled()) {
								logger.debug("The '" + localDirectory + "' directory doesn't exist; Will create.");
							}
							if (!localDirectory.mkdirs()) {
								throw new IOException("Failed to make local directory: " + localDirectory);
							}
						}
						else {
							throw new FileNotFoundException(localDirectory.getName());
						}
					}
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new MessagingException(
							"Failure during initialization of: " + this.getComponentType(), e);
				}
			}
		}
		if (Command.MGET.equals(this.command)) {
			Assert.isTrue(!(this.options.contains(Option.SUBDIRS)),
					"Cannot use " + Option.SUBDIRS.toString() + " when using 'mget' use "
							+ Option.RECURSIVE.toString() +	" to obtain files in subdirectories");
		}
		if (this.fileNameProcessor != null && getBeanFactory() != null) {
			this.fileNameProcessor.setBeanFactory(this.getBeanFactory());
			this.renameProcessor.setBeanFactory(this.getBeanFactory());
			this.remoteFileTemplate.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(final Message<?> requestMessage) {
		if (this.command != null) {
			switch (this.command) {
			case LS:
				return doLs(requestMessage);
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
		return this.remoteFileTemplate.execute(new SessionCallback<F, Object>() {

			@Override
			public Object doInSession(Session<F> session) throws IOException {
				return AbstractRemoteFileOutboundGateway.this.messageSessionCallback.doInSession(session,
						requestMessage);
			}

		});
	}

	private Object doLs(Message<?> requestMessage) {
		String dir = this.fileNameProcessor.processMessage(requestMessage);
		if (dir != null && !dir.endsWith(this.remoteFileTemplate.getRemoteFileSeparator())) {
			dir += this.remoteFileTemplate.getRemoteFileSeparator();
		}
		final String fullDir = dir;
		List<?> payload = this.remoteFileTemplate.execute(new SessionCallback<F, List<?>>() {

			@Override
			public List<?> doInSession(Session<F> session) throws IOException {
				return AbstractRemoteFileOutboundGateway.this.ls(session, fullDir);
			}
		});
		return this.getMessageBuilderFactory().withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, dir)
			.build();
	}

	private Object doGet(final Message<?> requestMessage) {
		final String remoteFilePath = this.fileNameProcessor.processMessage(requestMessage);
		final String remoteFilename = getRemoteFilename(remoteFilePath);
		final String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);
		Session<F> session = null;
		Object payload;
		if (this.options.contains(Option.STREAM)) {
			session = this.remoteFileTemplate.getSessionFactory().getSession();
			try {
				payload = session.readRaw(remoteFilePath);
			}
			catch (IOException e) {
				throw new MessageHandlingException(requestMessage, "Failed to get the remote file ["
						+ remoteFilePath
						+ "] as a stream", e);
			}
		}
		else {
			payload = this.remoteFileTemplate.execute(new SessionCallback<F, File>() {

				@Override
				public File doInSession(Session<F> session) throws IOException {
					return get(requestMessage, session, remoteDir, remoteFilePath, remoteFilename, true);

				}
			});
		}
		return getMessageBuilderFactory().withPayload(payload)
				.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
				.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
				.setHeader("file_remoteSession", session) // TODO: remove in 5.0
				.setHeader(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, session)
				.build();
	}

	private Object doMget(final Message<?> requestMessage) {
		final String remoteFilePath = this.fileNameProcessor.processMessage(requestMessage);
		final String remoteFilename = getRemoteFilename(remoteFilePath);
		final String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);
		List<File> payload = this.remoteFileTemplate.execute(new SessionCallback<F, List<File>>() {

			@Override
			public List<File> doInSession(Session<F> session) throws IOException {
				return mGet(requestMessage, session, remoteDir, remoteFilename);
			}
		});
		return this.getMessageBuilderFactory().withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doRm(Message<?> requestMessage) {
		final String remoteFilePath = this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);

		boolean payload = this.remoteFileTemplate.remove(remoteFilePath);

		return this.getMessageBuilderFactory().withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doMv(Message<?> requestMessage) {
		String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = getRemoteDirectory(remoteFilePath, remoteFilename);
		String remoteFileNewPath = this.renameProcessor.processMessage(requestMessage);
		Assert.hasLength(remoteFileNewPath, "New filename cannot be empty");

		this.remoteFileTemplate.rename(remoteFilePath, remoteFileNewPath);
		return this.getMessageBuilderFactory().withPayload(Boolean.TRUE)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.setHeader(FileHeaders.RENAME_TO, remoteFileNewPath)
			.build();
	}

	private String doPut(Message<?> requestMessage) {
		return this.doPut(requestMessage, null);
	}

	private String doPut(Message<?> requestMessage, String subDirectory) {
		String path = this.remoteFileTemplate.send(requestMessage, subDirectory, this.fileExistsMode);
		if (path == null) {
			throw new MessagingException(requestMessage, "No local file found for " + requestMessage);
		}
		if (this.chmod != null && isChmodCapable()) {
			doChmod(this.remoteFileTemplate, path, this.chmod);
		}
		return path;
	}

	/**
	 * Set the mode on the remote file after transfer; the default implementation does
	 * nothing.
	 * @param remoteFileTemplate the remote file template.
	 * @param path the path.
	 * @param chmod the chmod to set.
	 * @since 4.3
	 */
	protected void doChmod(RemoteFileTemplate<F> remoteFileTemplate, String path, int chmod) {
		// no-op
	}

	private Object doMput(Message<?> requestMessage) {
		File file = null;
		if (requestMessage.getPayload() instanceof File) {
			file = (File) requestMessage.getPayload();
		}
		else if (requestMessage.getPayload() instanceof String) {
			file = new File((String) requestMessage.getPayload());
		}
		else {
			throw new IllegalArgumentException("Only File or String payloads allowed for 'mput'");
		}
		if (!file.isDirectory()) {
			return this.doPut(requestMessage);
		}
		else {
			return putLocalDirectory(requestMessage, file, null);
		}
	}

	private List<String> putLocalDirectory(Message<?> requestMessage, File file, String subDirectory) {
		File[] files = file.listFiles();
		List<File> filteredFiles = this.filterMputFiles(files);
		List<String> replies = new ArrayList<String>();
		try {
			for (File filteredFile : filteredFiles) {
				if (!filteredFile.isDirectory()) {
					String path = this.doPut(this.getMessageBuilderFactory().withPayload(filteredFile)
							.copyHeaders(requestMessage.getHeaders())
							.build(), subDirectory);
					if (path == null) { //NOSONAR - false positive
						if (logger.isDebugEnabled()) {
							logger.debug("File " + filteredFile.getAbsolutePath() + " removed before transfer; ignoring");
						}
					}
					else {
						replies.add(path);
					}
				}
				else if (this.options.contains(Option.RECURSIVE)) {
					String newSubDirectory = (StringUtils.hasText(subDirectory) ?
							subDirectory + this.remoteFileTemplate.getRemoteFileSeparator() : "")
						+ filteredFile.getName();
					replies.addAll(this.putLocalDirectory(requestMessage, filteredFile, newSubDirectory));
				}
			}
		}
		catch (Exception e) {
			if (replies.size() > 0) {
				throw new PartialSuccessException(requestMessage,
						"Partially successful 'mput' operation" + (subDirectory == null ? "" : (" on " + subDirectory)),
						e, replies, filteredFiles);
			}
			else if (e instanceof PartialSuccessException) {
				throw new PartialSuccessException(requestMessage,
						"Partially successful 'mput' operation" + (subDirectory == null ? "" : (" on " + subDirectory)),
						e, replies, filteredFiles);
			}
			else if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
		}
		return replies;
	}

	protected List<?> ls(Session<F> session, String dir) throws IOException {
		List<F> lsFiles = listFilesInRemoteDir(session, dir, "");
		if (!this.options.contains(Option.LINKS)) {
			purgeLinks(lsFiles);
		}
		if (!this.options.contains(Option.ALL)) {
			purgeDots(lsFiles);
		}
		if (this.options.contains(Option.NAME_ONLY)) {
			List<String> results = new ArrayList<String>();
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

	private List<F> listFilesInRemoteDir(Session<F> session, String directory, String subDirectory) throws IOException {
		List<F> lsFiles = new ArrayList<F>();
		String remoteDirectory = buildRemotePath(directory, subDirectory);

		F[] files = session.list(remoteDirectory);
		boolean recursion = this.options.contains(Option.RECURSIVE);
		if (!ObjectUtils.isEmpty(files)) {
			Collection<F> filteredFiles = this.filterFiles(files);
			for (F file : filteredFiles) {
				String fileName = this.getFilename(file);
				if (file != null) {
					if (this.options.contains(Option.SUBDIRS) || !this.isDirectory(file)) {
						if (recursion && StringUtils.hasText(subDirectory)) {
							lsFiles.add(enhanceNameWithSubDirectory(file, subDirectory));
						}
						else {
							lsFiles.add(file);
						}
					}
					if (recursion && this.isDirectory(file) && !(".".equals(fileName)) && !("..".equals(fileName))) {
						lsFiles.addAll(listFilesInRemoteDir(session, directory,  subDirectory + fileName
								+ this.remoteFileTemplate.getRemoteFileSeparator()));
					}
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
			remotePath = "." + this.remoteFileTemplate.getRemoteFileSeparator() + child;
		}
		return remotePath;
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	protected final List<File> filterMputFiles(File[] files) {
		if (files == null) {
			return Collections.emptyList();
		}
		return (this.mputFilter != null) ? this.mputFilter.filterFiles(files) : Arrays.asList(files);
	}

	protected void purgeLinks(List<F> lsFiles) {
		Iterator<F> iterator = lsFiles.iterator();
		while (iterator.hasNext()) {
			if (this.isLink(iterator.next())) {
				iterator.remove();
			}
		}
	}

	protected void purgeDots(List<F> lsFiles) {
		Iterator<F> iterator = lsFiles.iterator();
		while (iterator.hasNext()) {
			if (getFilename(iterator.next()).startsWith(".")) {
				iterator.remove();
			}
		}
	}

	/**
	 * Copy a remote file to the configured local directory.
	 *
	 *
	 * @param message The message.
	 * @param session The session.
	 * @param remoteDir The remote directory.
	 * @param remoteFilePath The remote file path.
	 * @param remoteFilename The remote file name.
	 * @param lsFirst true to execute an 'ls' command first.
	 * @return The file.
	 * @throws IOException Any IOException.
	 */
	protected File get(Message<?> message, Session<F> session, String remoteDir, String remoteFilePath,
	                   String remoteFilename, boolean lsFirst) throws IOException {
		F[] files = null;
		if (lsFirst) {
			files = session.list(remoteFilePath);
			if (files == null) {
				throw new MessagingException("Session returned null when listing " + remoteFilePath);
			}
			if (files.length != 1 || isDirectory(files[0]) || isLink(files[0])) {
				throw new MessagingException(remoteFilePath + " is not a file");
			}
		}
		File localFile =
				new File(generateLocalDirectory(message, remoteDir), generateLocalFileName(message, remoteFilename));
		FileExistsMode fileExistsMode = this.fileExistsMode;
		boolean appending = FileExistsMode.APPEND.equals(fileExistsMode);
		boolean replacing = FileExistsMode.REPLACE.equals(fileExistsMode);
		if (!localFile.exists() || appending || replacing) {
			OutputStream outputStream;
			String tempFileName = localFile.getAbsolutePath() + this.remoteFileTemplate.getTemporaryFileSuffix();
			File tempFile = new File(tempFileName);
			if (appending) {
				outputStream = new BufferedOutputStream(new FileOutputStream(localFile, true));
			}
			else {
				outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			}
			if (replacing) {
				localFile.delete();
			}
			try {
				session.read(remoteFilePath, outputStream);
			}
			catch (Exception e) {
				/* Some operation systems acquire exclusive file-lock during file processing
				and the file can't be deleted without closing streams before.
				*/
				outputStream.close();
				tempFile.delete();

				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failure occurred while copying from remote to local directory", e);
				}
			}
			finally {
				try {
					outputStream.close();
				}
				catch (Exception ignored2) {
					//Ignore it
				}
			}
			if (!appending && !tempFile.renameTo(localFile)) {
				throw new MessagingException("Failed to rename local file");
			}
			if (lsFirst && this.options.contains(Option.PRESERVE_TIMESTAMP)) {
				localFile.setLastModified(getModified(files[0]));
			}
		}
		else if (FileExistsMode.IGNORE != fileExistsMode) {
			throw new MessageHandlingException(message, "Local file " + localFile + " already exists");
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Existing file skipped: " + localFile);
			}
		}
		return localFile;
	}

	protected List<File> mGet(Message<?> message, Session<F> session, String remoteDirectory,
							  String remoteFilename) throws IOException {
		if (this.options.contains(Option.RECURSIVE)) {
			if (logger.isWarnEnabled() && !("*".equals(remoteFilename))) {
				logger.warn("File name pattern must be '*' when using recursion");
			}
			if (this.options.contains(Option.NAME_ONLY)) {
				this.options.remove(Option.NAME_ONLY);
			}
			return mGetWithRecursion(message, session, remoteDirectory, remoteFilename);
		}
		else {
			return mGetWithoutRecursion(message, session, remoteDirectory, remoteFilename);
		}
	}

	private List<File> mGetWithoutRecursion(Message<?> message, Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {
		List<File> files = new ArrayList<File>();
		String remotePath = buildRemotePath(remoteDirectory, remoteFilename);
		@SuppressWarnings("unchecked")
		List<AbstractFileInfo<F>> remoteFiles = (List<AbstractFileInfo<F>>) ls(session, remotePath);
		if (remoteFiles.size() == 0 && this.options.contains(Option.EXCEPTION_WHEN_EMPTY)) {
			throw new MessagingException("No files found at "
					+ (remoteDirectory != null ? remoteDirectory : "Client Working Directory")
					+ " with pattern " + remoteFilename);
		}
		try {
			for (AbstractFileInfo<F> lsEntry : remoteFiles) {
				if (lsEntry.isDirectory()) {
					continue;
				}
				String fullFileName = remoteDirectory != null
						? remoteDirectory + getFilename(lsEntry)
						: getFilename(lsEntry);
				/*
				 * With recursion, the filename might contain subdirectory information
				 * normalize each file separately.
				 */
				String fileName = this.getRemoteFilename(fullFileName);
				String actualRemoteDirectory = this.getRemoteDirectory(fullFileName, fileName);
				File file = this.get(message, session, actualRemoteDirectory,
						fullFileName, fileName, false);
				files.add(file);
			}
		}
		catch (Exception e) {
			if (files.size() > 0) {
				throw new PartialSuccessException(message,
						"Partially successful recursive 'mget' operation on "
								+ (remoteDirectory != null ? remoteDirectory : "Client Working Directory"),
						e, files, remoteFiles);
			}
			else if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			else if (e instanceof IOException) {
				throw (IOException) e;
			}
		}
		return files;
	}

	private List<File> mGetWithRecursion(Message<?> message, Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {
		List<File> files = new ArrayList<File>();
		@SuppressWarnings("unchecked")
		List<AbstractFileInfo<F>> fileNames = (List<AbstractFileInfo<F>>) ls(session, remoteDirectory);
		if (fileNames.size() == 0 && this.options.contains(Option.EXCEPTION_WHEN_EMPTY)) {
			throw new MessagingException("No files found at "
					+ (remoteDirectory != null ? remoteDirectory : "Client Working Directory")
					+ " with pattern " + remoteFilename);
		}
		try {
			for (AbstractFileInfo<F> lsEntry : fileNames) {
				String fullFileName = remoteDirectory != null
						? remoteDirectory + getFilename(lsEntry)
						: getFilename(lsEntry);
				/*
				 * With recursion, the filename might contain subdirectory information
				 * normalize each file separately.
				 */
				String fileName = this.getRemoteFilename(fullFileName);
				String actualRemoteDirectory = this.getRemoteDirectory(fullFileName, fileName);
				File file = this.get(message, session, actualRemoteDirectory,
							fullFileName, fileName, false);
				files.add(file);
			}
		}
		catch (Exception e) {
			if (files.size() > 0) {
				throw new PartialSuccessException(message,
						"Partially successful recursive 'mget' operation on "
								+ (remoteDirectory != null ? remoteDirectory : "Client Working Directory"),
						e, files, fileNames);
			}
			else if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			else if (e instanceof IOException) {
				throw (IOException) e;
			}
			else {
				throw new MessagingException("Failed to process MGET on first file", e);
			}
		}
		return files;
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
		//TODO see org.springframework.integration.context.CustomConversionServiceFactoryBean
//		File localDir = this.localDirectoryExpression.getValue(evaluationContext, message, File.class);
		String localDirPath = this.localDirectoryExpression.getValue(evaluationContext, message, String.class);
		File localDir = new File(localDirPath);
		if (!localDir.exists()) {
			Assert.isTrue(localDir.mkdirs(), "Failed to make local directory: " + localDir);
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

	abstract protected boolean isDirectory(F file);

	abstract protected boolean isLink(F file);

	abstract protected String getFilename(F file);

	abstract protected String getFilename(AbstractFileInfo<F> file);

	abstract protected long getModified(F file);

	abstract protected List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

	abstract protected F enhanceNameWithSubDirectory(F file, String directory);

}
