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

package org.springframework.integration.adapter.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.Assert;

/**
 * Target adapter for sending files to an FTP server.
 * 
 * @author Iwein Fuld
 */
public class FtpTarget implements MessageTarget {

	private final MessageMapper<?, File> messageMapper;

	private volatile FTPClientPool ftpClientPool = new QueuedFTPClientPool();


	public FtpTarget(MessageMapper<?, File> messageMapper) {
		Assert.notNull(messageMapper, "MessageMapper must not be null");
		this.messageMapper = messageMapper;
	}


	public void setFtpClientPool(FTPClientPool ftpClientPool) {
		Assert.notNull(ftpClientPool, "ftpClientPool must not be null");
		this.ftpClientPool = ftpClientPool;
	}

	public boolean send(Message message) {
		boolean sent = false;
		File file = this.messageMapper.mapMessage(message);
		if (file != null && file.exists()) {
			FTPClient client = null;
			try {
				FileInputStream fileInputStream = new FileInputStream(file);
				client = this.ftpClientPool.getClient();
				sent = client.storeFile(file.getName(), fileInputStream);
				fileInputStream.close();
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
		return sent;
	}

}
