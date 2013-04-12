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

	protected final String command;

	public static final String COMMAND_LS = "ls";

	public static final String COMMAND_GET = "get";

	public static final String COMMAND_RM = "rm";

	public static final String COMMAND_MGET = "mget";

	public static final String OPTION_NAME_ONLY = "-1";

	public static final String OPTION_ALL = "-a";

	public static final String OPTION_NOSORT = "-f";

	public static final String OPTION_SUBDIRS = "-dirs";

	public static final String OPTION_LINKS = "-links";

	public static final String OPTION_PRESERVE_TIMESTAMP = "-P";

	public static final String OPTION_EXCEPTION_WHEN_EMPTY = "-x";

	private final Set<String> supportedCommands = new HashSet<String>(Arrays.asList(
			COMMAND_LS, COMMAND_GET, COMMAND_RM, COMMAND_MGET));

	private final ExpressionEvaluatingMessageProcessor<String> processor;

	protected volatile Set<String> options = new HashSet<String>();

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
		this.command = command;
		this.processor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
	}


	/**
	 * @param options the options to set
	 */
	public void setOptions(String options) {
		String[] opts = options.split("\\s");
		for (String opt : opts) {
			this.options.add(opt.trim());
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

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.command, "command must not be null");
		Assert.isTrue(
				this.supportedCommands.contains(this.command),
				"command must be one of "
						+ StringUtils
								.collectionToCommaDelimitedString(this.supportedCommands));
		if (COMMAND_RM.equals(this.command) || COMMAND_MGET.equals(this.command) ||
				COMMAND_GET.equals(this.command)) {
			Assert.isNull(this.filter, "Filters are not supported with the rm, get, and mget commands");
		}
		if (COMMAND_GET.equals(this.command)
				|| COMMAND_MGET.equals(this.command)) {
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
			this.processor.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Session<F> session = this.sessionFactory.getSession();
		try {
			if (COMMAND_LS.equals(this.command)) {
				String dir = this.processor.processMessage(requestMessage);
				if (!dir.endsWith(this.remoteFileSeparator)) {
					dir += this.remoteFileSeparator;
				}
				List<?> payload = ls(session, dir);
				return MessageBuilder.withPayload(payload)
					.setHeader(FileHeaders.REMOTE_DIRECTORY, dir)
					.build();
			}
			else if (COMMAND_GET.equals(this.command)) {
				String remoteFilePath =  this.processor.processMessage(requestMessage);
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
			else if (COMMAND_MGET.equals(this.command)) {
				String remoteFilePath =  this.processor.processMessage(requestMessage);
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
			else if (COMMAND_RM.equals(this.command)) {
				String remoteFilePath =  this.processor.processMessage(requestMessage);
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
			else {
				return null;
			}
		} catch (IOException e) {
			throw new MessagingException(requestMessage, e);
		} finally {
			session.close();
		}
	}

	protected List<?> ls(Session<F> session, String dir) throws IOException {
		List<F> lsFiles = new ArrayList<F>();
		F[] files = session.list(dir);
		if (!ObjectUtils.isEmpty(files)) {
			Collection<F> filteredFiles = this.filterFiles(files);
			for (F file : filteredFiles) {
				if (file != null) {
					if (this.options.contains(OPTION_SUBDIRS) || !isDirectory(file)) {
						lsFiles.add(file);
					}
				}
			}
		}
		else {
			return lsFiles;
		}
		if (!this.options.contains(OPTION_LINKS)) {
			purgeLinks(lsFiles);
		}
		if (!this.options.contains(OPTION_ALL)) {
			purgeDots(lsFiles);
		}
		if (this.options.contains(OPTION_NAME_ONLY)) {
			List<String> results = new ArrayList<String>();
			for (F file : lsFiles) {
				results.add(getFilename(file));
			}
			if (!this.options.contains(OPTION_NOSORT)) {
				Collections.sort(results);
			}
			return results;
		}
		else {
			List<AbstractFileInfo<F>> canonicalFiles = this.asFileInfoList(lsFiles);
			for (AbstractFileInfo<F> file : canonicalFiles) {
				file.setRemoteDirectory(dir);
			}
			if (!this.options.contains(OPTION_NOSORT)) {
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
			if (lsFirst && this.options.contains(OPTION_PRESERVE_TIMESTAMP)) {
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
		if (fileNames.length == 0 && this.options.contains(OPTION_EXCEPTION_WHEN_EMPTY)) {
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

	abstract protected boolean isDirectory(F file);

	abstract protected boolean isLink(F file);

	abstract protected String getFilename(F file);

	abstract protected long getModified(F file);

	abstract protected List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

}
