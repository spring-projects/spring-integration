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

package org.springframework.integration.ftp.outbound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.nio.charset.Charset;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.ftp.client.FtpClientPool;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A {@link org.springframework.integration.core.MessageHandler} implementation that sends files to an FTP server.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Josh Long
 * @author Oleg Zhurakousky
 */
public class FtpSendingMessageHandler extends AbstractMessageHandler{

	private static final String TEMPORARY_FILE_SUFFIX = ".writing";

	private volatile FtpClientPool ftpClientPool;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile File temporaryBufferFolderFile;

	private volatile Resource temporaryBufferFolder = new FileSystemResource(SystemUtils.getJavaIoTmpDir());

	private volatile String charset = Charset.defaultCharset().name();


	public FtpSendingMessageHandler() {
	}

	public FtpSendingMessageHandler(FtpClientPool ftpClientPool) {
		this.ftpClientPool = ftpClientPool;
	}


	public void setFtpClientPool(FtpClientPool ftpClientPool) {
		this.ftpClientPool = ftpClientPool;
	}

	public void setTemporaryBufferFolder(Resource temporaryBufferFolder) {
		this.temporaryBufferFolder = temporaryBufferFolder;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	protected void onInit() throws Exception {
		Assert.notNull(this.ftpClientPool, "'ftpClientPool' must not be null");
		Assert.notNull(this.temporaryBufferFolder,
				"'temporaryBufferFolder' must not be null");
		this.temporaryBufferFolderFile = this.temporaryBufferFolder.getFile();
	}

	private File handleFileMessage(File sourceFile, File tempFile, File resultFile) throws IOException {
		if (sourceFile.renameTo(resultFile)) {
			return resultFile;
		}
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
			File tempFile = new File(temporaryBufferFolderFile, generateFileName + TEMPORARY_FILE_SUFFIX);
			File resultFile = new File(temporaryBufferFolderFile, generateFileName);
			File sendableFile;
			if (payload instanceof String) {
				sendableFile = this.handleStringMessage((String) payload, tempFile, resultFile, this.charset);
			}
			else if (payload instanceof File) {
				sendableFile = this.handleFileMessage((File) payload, tempFile, resultFile);
			}
			else if (payload instanceof byte[]) {
				sendableFile = this.handleByteArrayMessage((byte[]) payload, tempFile, resultFile);
			}
			else {
				sendableFile = null;
			}
			return sendableFile;
		}
		catch (Throwable th) {
			throw new MessageDeliveryException(message);
		}
	}

	private boolean sendFile(File file, FTPClient client) throws FileNotFoundException, IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		boolean sent = client.storeFile(file.getName(), fileInputStream);
		fileInputStream.close();
		return sent;
	}

	private FTPClient getFtpClient() throws SocketException, IOException {
		FTPClient client;
		client = this.ftpClientPool.getClient();
		Assert.state(client != null, FtpClientPool.class.getSimpleName() +
				" returned 'null' client this most likely a bug in the pool implementation.");
		return client;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.notNull(message, "'message' must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "Message payload must not be null");
		File file = this.redeemForStorableFile(message);
		if ((file != null) && file.exists()) {
			FTPClient client = null;
			boolean sentSuccesfully;
			try {
				client = getFtpClient();
				sentSuccesfully = sendFile(file, client);
			}
			catch (FileNotFoundException e) {
				throw new MessageDeliveryException(message,
						"File [" + file + "] not found in local working directory; it was moved or deleted unexpectedly", e);
			}
			catch (IOException e) {
				throw new MessageDeliveryException(message,
						"Error transferring file [" + file + "] from local working directory to remote FTP directory", e);
			}
			catch (Exception e) {
				throw new MessageDeliveryException(message,
						"Error handling message for file [" + file + "]", e);
			}
			finally {
				if (file.exists()) {
					try {
						file.delete();
					}
					catch (Throwable th) {
						// ignore
					}
				}
				if (client != null) {
					ftpClientPool.releaseClient(client);
				}
			}
			if (!sentSuccesfully) {
				throw new MessageDeliveryException(message, "Failed to store file '" + file + "'");
			}
		}
	}

}
