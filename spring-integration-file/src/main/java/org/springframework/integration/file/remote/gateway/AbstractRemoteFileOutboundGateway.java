/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.RemoteFileUtils;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Outbound Gateways that perform remote file operations.
 *
 * @author Gary Russell
 * @since 2.1
 */
public abstract class AbstractRemoteFileOutboundGateway<F> extends AbstractReplyProducingMessageHandler {

	protected final SessionFactory<F> sessionFactory;

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
		MV("mv");

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
		 * Include directories {@code .} and {@code ..} in the results (ls).
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
		EXCEPTION_WHEN_EMPTY("-x");

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

	private volatile String remoteFileSeparator = "/";

	private volatile File localDirectory;

	private volatile boolean autoCreateLocalDirectory = true;

	private volatile String temporaryFileSuffix = ".writing";

	/**
	 * An {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	private volatile FileListFilter<F> filter;


	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, String command,
			String expression) {
		this.sessionFactory = sessionFactory;
		this.command = Command.toCommand(command);
		this.fileNameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
	}

	public AbstractRemoteFileOutboundGateway(SessionFactory<F> sessionFactory, Command command,
			String expression) {
		this.sessionFactory = sessionFactory;
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
		this.remoteFileSeparator = remoteFileSeparator;
	}

	/**
	 * @param localDirectory the localDirectory to set
	 */
	public void setLocalDirectory(File localDirectory) {
		this.localDirectory = localDirectory;
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
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
	}

	public void setRenameExpression(String expression) {
		Assert.notNull(expression, "'expression' cannot be null");
		this.renameProcessor = new ExpressionEvaluatingMessageProcessor<String>(
				new SpelExpressionParser().parseExpression(expression));
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.command, "command must not be null");
		if (Command.RM.equals(this.command) || Command.MGET.equals(this.command) ||
				Command.GET.equals(this.command)) {
			Assert.isNull(this.filter, "Filters are not supported with the rm, get, and mget commands");
		}
		if (Command.GET.equals(this.command)
				|| Command.MGET.equals(this.command)) {
			Assert.notNull(this.localDirectory, "localDirectory must not be null");
			try {
				if (!this.localDirectory.exists()) {
					if (this.autoCreateLocalDirectory) {
						if (logger.isDebugEnabled()) {
							logger.debug("The '" + this.localDirectory + "' directory doesn't exist; Will create.");
						}
						if (!this.localDirectory.mkdirs()) {
							throw new IOException("Failed to make local directory: " + this.localDirectory);
						}
					}
					else {
						throw new FileNotFoundException(this.localDirectory.getName());
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
		if (this.getBeanFactory() != null) {
			this.fileNameProcessor.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Session<F> session = this.sessionFactory.getSession();
		try {
			switch (this.command) {
			case LS:
				return doLs(requestMessage, session);
			case GET:
				return doGet(requestMessage, session);
			case MGET:
				return doMget(requestMessage, session);
			case RM:
				return doRm(requestMessage, session);
			case MV:
				return doMv(requestMessage, session);
			default:
				return null;
			}
		}
		catch (IOException e) {
			throw new MessagingException(requestMessage, e);
		}
		finally {
			session.close();
		}
	}

	private Object doLs(Message<?> requestMessage, Session<F> session) throws IOException {
		String dir = this.fileNameProcessor.processMessage(requestMessage);
		if (!dir.endsWith(this.remoteFileSeparator)) {
			dir += this.remoteFileSeparator;
		}
		List<?> payload = ls(session, dir);
		return MessageBuilder.withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, dir)
			.build();
	}

	private Object doGet(Message<?> requestMessage, Session<F> session) throws IOException {
		String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = remoteFilePath.substring(0, remoteFilePath.indexOf(remoteFilename));
		if (remoteDir.length() == 0) {
			remoteDir = this.remoteFileSeparator;
		}
		File payload = get(session, remoteFilePath, remoteFilename, true);
		return MessageBuilder.withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doMget(Message<?> requestMessage, Session<F> session) throws IOException {
		String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = remoteFilePath.substring(0, remoteFilePath.indexOf(remoteFilename));
		if (remoteDir.length() == 0) {
			remoteDir = this.remoteFileSeparator;
		}
		List<File> payload = mGet(session, remoteDir, remoteFilename);
		return MessageBuilder.withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doRm(Message<?> requestMessage, Session<F> session) throws IOException {
		String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = remoteFilePath.substring(0, remoteFilePath.indexOf(remoteFilename));
		if (remoteDir.length() == 0) {
			remoteDir = this.remoteFileSeparator;
		}
		boolean payload = rm(session, remoteFilePath);
		return MessageBuilder.withPayload(payload)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.build();
	}

	private Object doMv(Message<?> requestMessage, Session<F> session) throws IOException {
		String remoteFilePath =  this.fileNameProcessor.processMessage(requestMessage);
		String remoteFilename = getRemoteFilename(remoteFilePath);
		String remoteDir = remoteFilePath.substring(0, remoteFilePath.indexOf(remoteFilename));
		String remoteFileNewPath = this.renameProcessor.processMessage(requestMessage);
		Assert.hasLength(remoteFileNewPath, "New filename cannot be empty");
		if (remoteDir.length() == 0) {
			remoteDir = this.remoteFileSeparator;
		}
		mv(session, remoteFilePath, remoteFileNewPath);
		return MessageBuilder.withPayload(Boolean.TRUE)
			.setHeader(FileHeaders.REMOTE_DIRECTORY, remoteDir)
			.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
			.setHeader(FileHeaders.RENAME_TO, remoteFileNewPath)
			.build();
	}

	protected List<?> ls(Session<F> session, String dir) throws IOException {
		List<F> lsFiles = new ArrayList<F>();
		F[] files = session.list(dir);
		if (!ObjectUtils.isEmpty(files)) {
			Collection<F> filteredFiles = this.filterFiles(files);
			for (F file : filteredFiles) {
				if (file != null) {
					if (this.options.contains(Option.SUBDIRS) || !isDirectory(file)) {
						lsFiles.add(file);
					}
				}
			}
		}
		else {
			return lsFiles;
		}
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

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
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
	 * @param session
	 * @param remoteFilePath
	 * @throws IOException
	 */
	protected File get(Session<F> session, String remoteFilePath, String remoteFilename, boolean lsFirst)
			throws IOException {
		F[] files = null;
		if (lsFirst) {
			files = session.list(remoteFilePath);
			if (files.length != 1 || isDirectory(files[0]) || isLink(files[0])) {
				throw new MessagingException(remoteFilePath + " is not a file");
			}
		}
		File localFile = new File(this.localDirectory, remoteFilename);
		if (!localFile.exists()) {
			String tempFileName = localFile.getAbsolutePath() + this.temporaryFileSuffix;
			File tempFile = new File(tempFileName);
			FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
			try {
				session.read(remoteFilePath, fileOutputStream);
			}
			catch (Exception e) {
				if (e instanceof RuntimeException){
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failure occurred while copying from remote to local directory", e);
				}
			}
			finally {
				try {
					fileOutputStream.close();
				}
				catch (Exception ignored2) {
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

	protected List<File> mGet(Session<F> session, String remoteDirectory,
			String remoteFilename) throws IOException {
		String path = generateFullPath(remoteDirectory, remoteFilename);
		String[] fileNames = session.listNames(path);
		if (fileNames == null) {
			fileNames = new String[0];
		}
		if (fileNames.length == 0 && this.options.contains(Option.EXCEPTION_WHEN_EMPTY)) {
			throw new MessagingException("No files found at " + remoteDirectory
					+ " with pattern " + remoteFilename);
		}
		List<File> files = new ArrayList<File>();
		for (String fileName : fileNames) {
			File file;
			if (fileName.contains(this.remoteFileSeparator) &&
					fileName.startsWith(remoteDirectory)) { // the server returned the full path
				file = this.get(session, fileName,
						fileName.substring(fileName.lastIndexOf(this.remoteFileSeparator)), false);
			}
			else {
				file = this.get(session, generateFullPath(remoteDirectory, fileName), fileName, false);
			}
			files.add(file);
		}
		return files;
	}

	private String generateFullPath(String remoteDirectory, String remoteFilename) {
		String path;
		if (this.remoteFileSeparator.equals(remoteDirectory)) {
			path = remoteFilename;
		}
		else if (remoteDirectory.endsWith(this.remoteFileSeparator)) {
			path = remoteDirectory + remoteFilename;
		}
		else {
			path = remoteDirectory + this.remoteFileSeparator + remoteFilename;
		}
		return path;
	}

	/**
	 * @param remoteFilePath
	 */
	protected String getRemoteFilename(String remoteFilePath) {
		String remoteFileName;
		int index = remoteFilePath.lastIndexOf(this.remoteFileSeparator);
		if (index < 0) {
			remoteFileName = remoteFilePath;
		}
		else {
			remoteFileName = remoteFilePath.substring(index + 1);
		}
		return remoteFileName;
	}

	protected boolean rm(Session<?> session, String remoteFilePath)
			throws IOException {
		return session.remove(remoteFilePath);
	}

	protected void mv(Session<?> session, String remoteFilePath, String remoteFileNewPath) throws IOException {
		int lastSeparator = remoteFileNewPath.lastIndexOf(this.remoteFileSeparator);
		if (lastSeparator > 0) {
			String remoteFileDirectory = remoteFileNewPath.substring(0, lastSeparator + 1);
			RemoteFileUtils.makeDirectories(remoteFileDirectory, session, this.remoteFileSeparator, this.logger);
		}
		session.rename(remoteFilePath, remoteFileNewPath);
	}

	abstract protected boolean isDirectory(F file);

	abstract protected boolean isLink(F file);

	abstract protected String getFilename(F file);

	abstract protected long getModified(F file);

	abstract protected List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

}
