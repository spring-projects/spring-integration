/*
 * Copyright 2002-2011 the original author or authors.
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

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public abstract class AbstractRemoteFileOutboundGateway<F> extends AbstractReplyProducingMessageHandler {

	protected final SessionFactory sessionFactory;

	protected final String command;

	public static final String COMMAND_LS = "ls";

	public static final String COMMAND_GET = "get";

	public static final String COMMAND_RM = "rm";

	protected Set<String> options = new HashSet<String>();

	public static final String OPTION_JUST_NAME = "-1";

	public static final String OPTION_ALL = "-a";

	public static final String OPTION_NOSORT = "-f";

	public static final String OPTION_SUBDIRS = "-dirs";

	public static final String OPTION_LINKS = "-links";

	public static final String OPTION_PRESERVE_TIMESTAMP = "-P";

	private final ExpressionEvaluatingMessageProcessor<String> processor;

	private volatile String remoteFileSeparator = "/";

	private volatile File localDirectory;

	private volatile boolean autoCreateLocalDirectory = true;

	private volatile String temporaryFileSuffix = ".writing";

	/**
	 * An {@link FileListFilter} that runs against the <emphasis>remote</emphasis> file system view.
	 */
	private volatile FileListFilter<F> filter;


	public AbstractRemoteFileOutboundGateway(SessionFactory sessionFactory, String command,
			String expression) {
		this.sessionFactory = sessionFactory;
		this.command = command;
		this.processor = new ExpressionEvaluatingMessageProcessor<String>(
			new SpelExpressionParser().parseExpression(expression));
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(this.command, "command must not be null");
		Assert.isTrue(COMMAND_LS.equals(this.command) || COMMAND_GET.equals(this.command) ||
					  COMMAND_RM.equals(this.command),
				"command must be one of ls, get, rm");
		if (COMMAND_RM.equals(this.command)) {
			Assert.isNull(this.filter, "Filters are not supported with the rm command");
		} else if (COMMAND_GET.equals(this.command)) {
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
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Session session = this.sessionFactory.getSession();
		try {
			if (COMMAND_LS.equals(this.command)) {
				String dir = this.processor.processMessage(requestMessage);
				if (!dir.endsWith("/")) {
					dir += "/";
				}
				return MessageBuilder.withPayload(ls(session, dir))
					.setHeader(FileHeaders.REMOTE_DIR, dir)
					.build();
			} else if (COMMAND_GET.equals(this.command)) {
				String remoteFilePath =  this.processor.processMessage(requestMessage);
				String remoteFilename = getRemoteFilename(remoteFilePath);
				String remoteDir = remoteFilePath.substring(0, remoteFilePath.indexOf(remoteFilename));
				if (remoteDir.length() == 0) {
					remoteDir = "/";
				}
				return MessageBuilder.withPayload(get(session, remoteFilePath, remoteFilename))
					.setHeader(FileHeaders.REMOTE_DIR, remoteDir)
					.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
					.build();
			} else if (COMMAND_RM.equals(this.command)) {
				String remoteFilePath =  this.processor.processMessage(requestMessage);
				String remoteFilename = getRemoteFilename(remoteFilePath);
				String remoteDir = remoteFilePath.substring(0, remoteFilePath.indexOf(remoteFilename));
				if (remoteDir.length() == 0) {
					remoteDir = "/";
				}
				return MessageBuilder.withPayload(rm(session, remoteFilePath))
					.setHeader(FileHeaders.REMOTE_DIR, remoteDir)
					.setHeader(FileHeaders.REMOTE_FILE, remoteFilename)
					.build();
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new MessagingException(requestMessage, e);
		} finally {
			session.close();
		}
	}

	protected List<?> ls(Session session, String dir) throws IOException {
		List<F> lsFiles = new ArrayList<F>();
		F[] files = session.<F>list(dir);
		if (!ObjectUtils.isEmpty(files)) {
			Collection<F> filteredFiles = this.filterFiles(files);
			for (F file : filteredFiles) {
				if (file != null) {
					if (this.options.contains(OPTION_SUBDIRS) ||
							!isDir(file)) {
						lsFiles.add(file);
					}
				}
			}
		} else {
			return lsFiles;
		}
		if (!this.options.contains(OPTION_LINKS)) {
			purgeLinks(lsFiles);
		}
		if (!this.options.contains(OPTION_ALL)) {
			purgeDots(lsFiles);
		}
		if (this.options.contains(OPTION_JUST_NAME)) {
			List<String> results = new ArrayList<String>();
			for (F file : lsFiles) {
				results.add(getFilename(file));
			}
			if (!this.options.contains(OPTION_NOSORT)) {
				Collections.sort(results);
			}
			return results;
		} else {
			List<AbstractFileInfo<F>> canonicalFiles = this.asFileInfoList(lsFiles);
			for (AbstractFileInfo<F> file : canonicalFiles) {
				file.setRemoteDir(dir);
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
	 * @return
	 * @throws IOException
	 */
	protected File get(Session session, String remoteFilePath, String remoteFilename)
			throws IOException {
		F[] files = session.<F>list(remoteFilePath);
		if (files.length != 1 || isDir(files[0]) || isLink(files[0])) {
			throw new MessagingException(remoteFilePath + " is not a file");
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
			if (this.options.contains(OPTION_PRESERVE_TIMESTAMP)) {
				localFile.setLastModified(getModified(files[0]));
			}
			return localFile;
		} else {
			throw new MessagingException("Local file " + localFile + " already exists");
		}
	}

	/**
	 * @param remoteFilePath
	 * @return
	 */
	protected String getRemoteFilename(String remoteFilePath) {
		String remoteFileName;
		int index = remoteFilePath.lastIndexOf(this.remoteFileSeparator);
		if (index < 0) {
			remoteFileName = remoteFilePath;
		} else {
			remoteFileName = remoteFilePath.substring(index + 1);
		}
		return remoteFileName;
	}

	protected boolean rm(Session session, String remoteFilePath)
			throws IOException {
		return session.remove(remoteFilePath);
	}

	abstract protected boolean isDir(F file);

	abstract protected boolean isLink(F file);

	abstract protected String getFilename(F file);

	abstract protected long getModified(F file);

	abstract protected List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

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

}
