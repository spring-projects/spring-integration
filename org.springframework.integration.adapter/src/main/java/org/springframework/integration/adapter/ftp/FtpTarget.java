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

/**
 * Target adapter for sending files to an ftp server.
 * 
 * @author Iwein Fuld
 *
 */
public class FtpTarget implements MessageTarget {

	private final MessageMapper<?, File> messageMapper;

	private FTPClientPool ftpClientPool = new FTPClientPool();

	public FtpTarget(MessageMapper<?, File> messageMapper) {
		this.messageMapper = messageMapper;
	}

	public boolean send(Message message) {
boolean sent = false;
		File file = messageMapper.mapMessage(message);
		if (file.exists()) {
			FTPClient client=null;
			FileInputStream fileInputStream = null;
			try {
				fileInputStream = new FileInputStream(file);
					client = ftpClientPool.getClient();
				sent = client.storeFile(file.getName(), fileInputStream);
				fileInputStream.close();
				}
			catch (FileNotFoundException e) {
				throw new MessageDeliveryException(message, "File " + file + " lost from local working directory", e);
			}
			catch (IOException e) {
				throw new MessageDeliveryException(message, "Error transferring " + file
						+ " from local working directory to remote ftp directory", e);
			}
			finally {
				ftpClientPool.releaseClient(client);
			}
		}
		return sent;
	}

	public void setFtpClientPool(FTPClientPool ftpClientPool) {
		this.ftpClientPool = ftpClientPool;
	}

}
