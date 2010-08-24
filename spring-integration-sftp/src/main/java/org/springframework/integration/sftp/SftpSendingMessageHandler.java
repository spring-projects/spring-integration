/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.*;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.*;


/**
 * Sending a message payload to a remote SFTP endpoint. For now, we assume that the payload of the inbound message is of
 * type {@link java.io.File}. Perhaps we could support a payload of java.io.InputStream with a Header designating the file
 * name?
 *
 * @author Josh Long
 */
public class SftpSendingMessageHandler implements MessageHandler, InitializingBean {
	private SftpSessionPool pool;
	private String remoteDirectory;
	private volatile boolean afterPropertiesSetRan;

	public SftpSendingMessageHandler(SftpSessionPool pool) {
		this.pool = pool;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.state(this.pool != null, "the pool can't be null!");

		temporaryBufferFolderFile = this.temporaryBufferFolder.getFile();

		if (!afterPropertiesSetRan) {
			if (StringUtils.isEmpty(this.remoteDirectory)) {
				remoteDirectory = null;
			}

			this.afterPropertiesSetRan = true;
		}
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	/* Ugh this needs to be put in a convenient place accessible for all the file:, sftp:, and ftp:* adapters */

	private File handleFileMessage(File sourceFile, File tempFile, File resultFile)
			throws IOException {
		if (sourceFile.renameTo(resultFile)) {
			return resultFile;
		}

		FileCopyUtils.copy(sourceFile, tempFile);
		tempFile.renameTo(resultFile);

		return resultFile;
	}

	private File handleByteArrayMessage(byte[] bytes, File tempFile, File resultFile)
			throws IOException {
		FileCopyUtils.copy(bytes, tempFile);
		tempFile.renameTo(resultFile);

		return resultFile;
	}

	private File handleStringMessage(String content, File tempFile, File resultFile, String charset)
			throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), charset);
		FileCopyUtils.copy(content, writer);
		tempFile.renameTo(resultFile);

		return resultFile;
	}

	private static final String TEMPORARY_FILE_SUFFIX = ".writing";
	private FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
	private File temporaryBufferFolderFile;
	private Resource temporaryBufferFolder = new FileSystemResource(SystemUtils.getJavaIoTmpDir());

	public void setTemporaryBufferFolder(Resource temporaryBufferFolder) {
		this.temporaryBufferFolder = temporaryBufferFolder;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	private File redeemForStorableFile(Message<?> msg) throws MessageDeliveryException {
		try {
			Object payload = msg.getPayload();
			String generateFileName = this.fileNameGenerator.generateFileName(msg);
			File tempFile = new File(temporaryBufferFolderFile, generateFileName + TEMPORARY_FILE_SUFFIX);
			File resultFile = new File(temporaryBufferFolderFile, generateFileName);
			File sendableFile;
			if (payload instanceof String)
				sendableFile = this.handleStringMessage((String) payload, tempFile, resultFile, this.charset);
			else if (payload instanceof File)
				sendableFile = this.handleFileMessage((File) payload, tempFile, resultFile);
			else if (payload instanceof byte[])
				sendableFile = this.handleByteArrayMessage((byte[]) payload, tempFile, resultFile);
			else sendableFile = null;
			return sendableFile;
		} catch (Throwable th) {
			throw new MessageDeliveryException(msg);
		}

	}

	private String charset;

	public void setCharset(String charset) {
		this.charset = charset;
	}
	/* Ugh this needs to be put in a convenient place accessible for all the file:, sftp:, and ftp:* adapters */


	public void handleMessage(final Message<?> message)
			throws MessageRejectedException, MessageHandlingException, MessageDeliveryException {
		Assert.state(this.pool != null, "need a working pool");
		File inboundFilePayload = this.redeemForStorableFile(message);
		try {

			if ((inboundFilePayload != null) && inboundFilePayload.exists()) {
				sendFileToRemoteEndpoint(message, inboundFilePayload);
			}
		} catch (Throwable thr) {
			//   logger.debug("recieved an exception.", thr);
			throw new MessageDeliveryException(message, "couldn't deliver the message!", thr);
		} finally {
			if (inboundFilePayload != null && inboundFilePayload.exists())
				inboundFilePayload.delete();

		}
	}

	public void setRemoteDirectory(final String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	private boolean sendFileToRemoteEndpoint(Message<?> message, File file)
			throws Throwable {
		assert this.pool != null : "need a working pool";

		SftpSession session = this.pool.getSession();

		if (session == null) {
			throw new RuntimeException("the session returned from the pool is null, can't possibly proceed.");
		}

		session.start();

		ChannelSftp sftp = session.getChannel();

		InputStream fileInputStream = null;

		try {
			fileInputStream = new FileInputStream(file);

			String baseOfRemotePath = StringUtils.isEmpty(this.remoteDirectory) ? StringUtils.EMPTY : remoteDirectory; // the safe default

			// logger.debug("going to send " + file.getAbsolutePath() + " to a remote sftp endpoint");
			String dynRd = null;
			MessageHeaders messageHeaders = null;

			if (message != null) {
				messageHeaders = message.getHeaders();

				if ((messageHeaders != null) && messageHeaders.containsKey(SftpConstants.SFTP_REMOTE_DIRECTORY_HEADER)) {
					dynRd = (String) messageHeaders.get(SftpConstants.SFTP_REMOTE_DIRECTORY_HEADER);

					if (!StringUtils.isEmpty(dynRd)) {
						baseOfRemotePath = dynRd;
					}
				}
			}

			if (!StringUtils.defaultString(baseOfRemotePath).endsWith("/")) {
				baseOfRemotePath += "/";
			}

			sftp.put(fileInputStream, baseOfRemotePath + file.getName());

			return true;
		} finally {
			IOUtils.closeQuietly(fileInputStream);

			if (pool != null) {
				pool.release(session);
			}
		}
	}
}
