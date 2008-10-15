/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.util.Assert;

/**
 * A {@link MessageConsumer} implementation that sends files to an FTP server.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FtpSendingMessageConsumer implements MessageConsumer {

	private final FTPClientPool ftpClientPool;


	public FtpSendingMessageConsumer(FTPClientPool ftpClientPool) {
		Assert.notNull(ftpClientPool, "ftpClientPool must not be null");
		this.ftpClientPool = ftpClientPool;
	}


	public void onMessage(Message<?> message) {
		Assert.notNull(message, "message must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "message payload must not be null");
		Assert.isInstanceOf(File.class, payload, "Message payload must be an instance of [java.io.File]");
		File file = (File) payload;
		if (file != null && file.exists()) {
			FTPClient client = null;
			try {
				FileInputStream fileInputStream = new FileInputStream(file);
				client = this.ftpClientPool.getClient();
				boolean sent = client.storeFile(file.getName(), fileInputStream);
				fileInputStream.close();
				if (!sent) {
					throw new MessageDeliveryException(message, "Failed to store file '" + file + "'");
				}
			}
			catch (FileNotFoundException e) {
				throw new MessageDeliveryException(message, "File [" + file + "] lost from local working directory", e);
			}
			catch (IOException e) {
				throw new MessageDeliveryException(message, "Error transferring File [" + file
						+ "] from local working directory to remote FTP directory", e);
			}
			finally {
				ftpClientPool.releaseClient(client);
			}
		}
	}

}
