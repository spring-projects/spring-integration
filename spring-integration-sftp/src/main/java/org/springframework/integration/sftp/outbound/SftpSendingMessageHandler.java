/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.sftp.outbound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpSessionPool;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.jcraft.jsch.ChannelSftp;

/**
 * Sends message payloads to a remote SFTP endpoint.
 * Assumes that the payload of the inbound message is of type {@link java.io.File}.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpSendingMessageHandler extends AbstractMessageHandler {

	private static final String TEMPORARY_FILE_SUFFIX = ".writing";

	private final SftpSessionPool sessionPool;
	
	private volatile ExpressionEvaluatingMessageProcessor<String> directoryExpressionProcesor;

	private volatile Expression remoteDirectoryExpression;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile File temporaryBufferFolderFile;

	private volatile Resource temporaryBufferFolder = new FileSystemResource(SystemUtils.getJavaIoTmpDir());

	private volatile String charset = Charset.defaultCharset().name();


	public SftpSendingMessageHandler(SftpSessionPool sessionPool) {
		Assert.notNull(sessionPool, "'sessionPool' must not be null");
		this.sessionPool = sessionPool;
	}


	public void setTemporaryBufferFolder(Resource temporaryBufferFolder) {
		this.temporaryBufferFolder = temporaryBufferFolder;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.remoteDirectoryExpression = remoteDirectoryExpression;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	protected void onInit() throws Exception {
		this.temporaryBufferFolderFile = this.temporaryBufferFolder.getFile();
		if (this.remoteDirectoryExpression != null) {
			this.directoryExpressionProcesor =
					new ExpressionEvaluatingMessageProcessor<String>(this.remoteDirectoryExpression, String.class);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		File inboundFilePayload = this.redeemForStorableFile(message);
		try {
			if ((inboundFilePayload != null) && inboundFilePayload.exists()) {
				this.sendFileToRemoteEndpoint(message, inboundFilePayload);
			}
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to transfer '" + message.getPayload() + "' to " +
					this.remoteDirectoryExpression.getExpressionString(), e);
		}
		finally {
			if (inboundFilePayload != null && inboundFilePayload.exists()) {
				inboundFilePayload.delete();
			}		
		}
	}

	private File handleFileMessage(File sourceFile, File tempFile, File resultFile) throws IOException {
		FileCopyUtils.copy(sourceFile, tempFile);
		tempFile.renameTo(resultFile);
		return resultFile;
	}

	private File handleByteArrayMessage(byte[] bytes, File tempFile, File resultFile) throws IOException {
		FileCopyUtils.copy(bytes, tempFile);
		tempFile.renameTo(resultFile);
		return resultFile;
	}

	private File handleStringMessage(String content, File tempFile, File resultFile, String charset) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempFile), charset);
		FileCopyUtils.copy(content, writer);
		tempFile.renameTo(resultFile);
		return resultFile;
	}

	private File redeemForStorableFile(Message<?> message) throws MessageDeliveryException {
		try {
			Object payload = message.getPayload();
			String generateFileName = this.fileNameGenerator.generateFileName(message);
			File tempFile = new File(this.temporaryBufferFolderFile, generateFileName + TEMPORARY_FILE_SUFFIX);
			File resultFile = new File(this.temporaryBufferFolderFile, generateFileName);
			File sendableFile = null;
			if (payload instanceof String) {
				sendableFile = this.handleStringMessage((String) payload, tempFile, resultFile, this.charset);
			}
			else if (payload instanceof File) {
				sendableFile = this.handleFileMessage((File) payload, tempFile, resultFile);
			}
			else if (payload instanceof byte[]) {
				sendableFile = this.handleByteArrayMessage((byte[]) payload, tempFile, resultFile);
			}
			return sendableFile;
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "Failed to create sendable file.", e);
		}
	}

	private boolean sendFileToRemoteEndpoint(Message<?> message, File file) throws Exception {
		SftpSession session = this.sessionPool.getSession();
		if (session == null) {
			throw new MessagingException("The session returned from the pool is null, cannot proceed.");
		}
		InputStream fileInputStream = null;
		try {
			session.connect();
			ChannelSftp sftp = session.getChannel();	
			fileInputStream = new FileInputStream(file);
			String baseOfRemotePath = "";
			if (this.directoryExpressionProcesor != null) {
				String result = this.directoryExpressionProcesor.processMessage(message);
				if (StringUtils.hasText(result)) {
					baseOfRemotePath = result;
				}
			}
			if (!StringUtils.endsWithIgnoreCase(baseOfRemotePath, "/")) {
				baseOfRemotePath += "/";
			}
			sftp.put(fileInputStream, baseOfRemotePath + file.getName());
			return true;
		}
		finally {
			IOUtils.closeQuietly(fileInputStream);
			this.sessionPool.release(session);
		}
	}

}
