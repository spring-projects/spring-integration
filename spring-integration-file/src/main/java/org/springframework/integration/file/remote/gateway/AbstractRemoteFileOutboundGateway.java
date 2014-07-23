/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;
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

	private final RemoteFileTemplate<F> remoteFileTemplate;

	protected final Command command;

	/**
	 * Enumeration of commands supported by the gateways.
	 */
	public static enum Command {

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

		private Command(String command) {
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
	public static enum Option {

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
		RECURSIVE("-R");

		private String option;

		private Option(String option) {
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

	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, String command,
			String expression) {
		Assert.notNull(sessionFactory, "'sessionFactory' cannot be null");
		this.remoteFileTemplate = new RemoteFileTemplate<F>(sessionFactory);
		this.command = Command.toCommand(command);
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
	}

	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, Command command,
			String expression) {
		Assert.notNull(sessionFactory, "'sessionFactory' cannot be null");
		this.remoteFileTemplate = new RemoteFileTemplate<F>(sessionFactory);
		this.command = command;
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
	}

	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate, String command,
			String expression) {
		Assert.notNull(remoteFileTemplate, "'remoteFileTemplate' cannot be null");
		this.remoteFileTemplate = remoteFileTemplate;
		this.command = Command.toCommand(command);
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
	}

	public AbstractRemoteFileOutboundGateway(RemoteFileTemplate<F> remoteFileTemplate, Command command,
			String expression) {
		Assert.notNull(remoteFileTemplate, "'remoteFileTemplate' cannot be null");
		this.remoteFileTemplate = remoteFileTemplate;
		this.command = command;
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
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

	public void setRenameExpression(String expression) {
		Assert.notNull(expression, "'expression' cannot be null");
		this.renameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
				new SpelExpressionParser().parseExpression(expression));
	}

	public void setLocalFilenameGeneratorExpression(Expression localFilenameGeneratorExpression) {
		Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
		this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
	}


	@Override
	protected void doInit() {
		Assert.notNull(this.command, "command must not be null");
		if (Command.RM.equals(this.command) ||
				Command.GET.equals(this.command)) {
			Assert.isNull(this.filter, "Filters are not supported with the rm and get commands");
		}
		if (Command.GET.equals(this.command)
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
					"Cannot use " + Option.SUBDIRS.toString() + " when using 'mget' use " + Option.RECURSIVE.toString() +
							" to obtain files in subdirectories");
		}
		if (this.getBeanFactory() != null) {
			this.fileNameProcessor.setBeanFactory(this.getBeanFactory());
			this.renameProcessor.setBeanFactory(this.getBeanFactory());
			this.remoteFileTemplate.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
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
		default:
			return null;
		}
	}

	private Object doLs(Message<?> requestMessage) {
		String dir = this.fileNameProcessor.processMessage(requestMessage);
		if (!dir.endsWith(this.remoteFileTemplate.getRemoteFileSeparator())) {
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
		final String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		final String remoteFilename = this.getRemoteFilename(remoteFilePath);
		final String remoteDir = this.getRemoteDirectory(remoteFilePath, remoteFilename);
		File payload = this.remoteFileTemplate.execute(new SessionCallback<F, File>() {

			@Override
			public File doInSession(Session<F> session) throws IOException {
				return AbstractRemoteFileOutboundGateway.this.get(requestMessage, session, remoteDir, remoteFilePath,
						remoteFilename, true);

			}
		});
		return this.getMessageBuilderFactory().withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doMget(final Message<?> requestMessage) {
		final String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		final String remoteFilename = this.getRemoteFilename(remoteFilePath);
		final String remoteDir = this.getRemoteDirectory(remoteFilePath, remoteFilename);
		List<File> payload = this.remoteFileTemplate.execute(new SessionCallback<F, List<File>>() {

			@Override
			public List<File> doInSession(Session<F> session) throws IOException {
				return AbstractRemoteFileOutboundGateway.this.mGet(requestMessage, session, remoteDir, remoteFilename);
			}
		});
		return this.getMessageBuilderFactory().withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doRm(Message<?> requestMessage) {
		final String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = this.getRemoteFilename(remoteFilePath);
		String remoteDir = this.getRemoteDirectory(remoteFilePath, remoteFilename);
		boolean payload = this.remoteFileTemplate.remove(remoteFilePath);
		return this.getMessageBuilderFactory().withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doMv(Message<?> requestMessage) {
		String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = this.getRemoteFilename(remoteFilePath);
		String remoteDir = this.getRemoteDirectory(remoteFilePath, remoteFilename);
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
		String path = this.remoteFileTemplate.send(requestMessage, subDirectory);
		if (path == null) {
			throw new MessagingException(requestMessage, "No local file found for " + requestMessage);
		}
		return path;
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
			List<String> replies = this.putLocalDirectory(requestMessage, file, null);
			return replies;
		}
	}

	private List<String> putLocalDirectory(Message<?> requestMessage, File file, String subDirectory) {
		File[] files = file.listFiles();
		List<File> filteredFiles = this.filterMputFiles(files);
		List<String> replies = new ArrayList<String>();
		for (File filteredFile : filteredFiles) {
			if (!filteredFile.isDirectory()) {
				String path = this.doPut(this.getMessageBuilderFactory().withPayload(filteredFile)
						.copyHeaders(requestMessage.getHeaders())
						.build(), subDirectory);
				if (path == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("File " + filteredFile.getAbsolutePath() + " removed before transfer; ignoring");
					}
				}
				else {
					replies.add(path);
				}
			}
			else if (this.options.contains(Option.RECURSIVE)){
				String newSubDirectory = (StringUtils.hasText(subDirectory) ?
						subDirectory + this.remoteFileTemplate.getRemoteFileSeparator() : "")
					+ filteredFile.getName();
				replies.addAll(this.putLocalDirectory(requestMessage, filteredFile, newSubDirectory));
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
		F[] files = session.list(directory + subDirectory);
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
	protected File get(Message<?> message, Session<F> session, String remoteDir, String remoteFilePath, String remoteFilename, boolean lsFirst)
			throws IOException {
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
		File localFile = new File(this.generateLocalDirectory(message, remoteDir), this.generateLocalFileName(message, remoteFilename));
		if (!localFile.exists()) {
			String tempFileName = localFile.getAbsolutePath() + this.remoteFileTemplate.getTemporaryFileSuffix();
			File tempFile = new File(tempFileName);
			BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			try {
				session.read(remoteFilePath, outputStream);
			}
			catch (Exception e) {
				/* Some operation systems acquire exclusive file-lock during file processing
				and the file can't be deleted without closing streams before.
				*/
				outputStream.close();
				tempFile.delete();

				if (e instanceof RuntimeException){
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
			if (!tempFile.renameTo(localFile)) {
				throw new MessagingException("Failed to rename local file");
			}
			if (lsFirst && this.options.contains(Option.PRESERVE_TIMESTAMP)) {
				localFile.setLastModified(getModified(files[0]));
			}
			return localFile;
		}
		else {
			throw new MessagingException("Local file " + localFile + " already exists");
		}
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
		String path = this.generateFullPath(remoteDirectory, remoteFilename);
		String[] fileNames = session.listNames(path);
		if (fileNames == null) {
			fileNames = new String[0];
		}
		if (fileNames.length == 0 && this.options.contains(Option.EXCEPTION_WHEN_EMPTY)) {
			throw new MessagingException("No files found at " + remoteDirectory
					+ " with pattern " + remoteFilename);
		}
		List<File> files = new ArrayList<File>();
		String remoteFileSeparator = this.remoteFileTemplate.getRemoteFileSeparator();
		for (String fileName : fileNames) {
			File file;
			if (fileName.contains(remoteFileSeparator) &&
					fileName.startsWith(remoteDirectory)) { // the server returned the full path
				file = this.get(message, session, remoteDirectory, fileName,
						fileName.substring(fileName.lastIndexOf(remoteFileSeparator)), false);
			}
			else {
				file = this.get(message, session, remoteDirectory,
						this.generateFullPath(remoteDirectory, fileName), fileName, false);
			}
			files.add(file);
		}
		return files;
	}

	private List<File> mGetWithRecursion(Message<?> message, Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {
		List<File> files = new ArrayList<File>();
		@SuppressWarnings("unchecked")
		List<AbstractFileInfo<F>> fileNames = (List<AbstractFileInfo<F>>) this.ls(session, remoteDirectory);
		if (fileNames.size() == 0 && this.options.contains(Option.EXCEPTION_WHEN_EMPTY)) {
			throw new MessagingException("No files found at " + remoteDirectory
					+ " with pattern " + remoteFilename);
		}
		for (AbstractFileInfo<F> lsEntry : fileNames) {
			String fullFileName = remoteDirectory + this.getFilename(lsEntry);
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
		return files;
	}

	private String getRemoteDirectory(String remoteFilePath, String remoteFilename) {
		String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf(remoteFilename));
		if (remoteDir.length() == 0) {
			remoteDir = this.remoteFileTemplate.getRemoteFileSeparator();
		}
		return remoteDir;
	}

	private String generateFullPath(String remoteDirectory, String remoteFilename) {
		String path;
		String remoteFileSeparator = this.remoteFileTemplate.getRemoteFileSeparator();
		if (remoteFileSeparator.equals(remoteDirectory)) {
			path = remoteFilename;
		}
		else if (remoteDirectory.endsWith(remoteFileSeparator)) {
			path = remoteDirectory + remoteFilename;
		}
		else {
			path = remoteDirectory + remoteFileSeparator + remoteFilename;
		}
		return path;
	}

	/**
	 * @param remoteFilePath The remote file path.
	 * @return The remote file name.
	 */
	protected String getRemoteFilename(String remoteFilePath) {
		String remoteFileName;
		int index = remoteFilePath.lastIndexOf(this.remoteFileTemplate.getRemoteFileSeparator());
		if (index < 0) {
			remoteFileName = remoteFilePath;
		}
		else {
			remoteFileName = remoteFilePath.substring(index + 1);
		}
		return remoteFileName;
	}

	private File generateLocalDirectory(Message<?> message, String remoteDirectory) {
		EvaluationContext evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		evaluationContext.setVariable("remoteDirectory", remoteDirectory);
		File localDir = this.localDirectoryExpression.getValue(evaluationContext, message, File.class);
		if (!localDir.exists()) {
			Assert.isTrue(localDir.mkdirs(), "Failed to make local directory: " + localDir);
		}
		return localDir;
	}

	private String generateLocalFileName(Message<?> message, String remoteFileName){
		if (this.localFilenameGeneratorExpression != null){
			EvaluationContext evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
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
