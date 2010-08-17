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

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;


/**
 * A {@link org.springframework.integration.core.MessageHandler} implementation that sends files to an FTP server.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FTPSendingMessageHandler implements MessageHandler, InitializingBean {
    private FTPClientPool ftpClientPool;

    public FTPSendingMessageHandler() {
    }

    public FTPSendingMessageHandler(FTPClientPool ftpClientPool) {
        this.ftpClientPool = ftpClientPool;
    }

    public void setFtpClientPool(FTPClientPool ftpClientPool) {
        this.ftpClientPool = ftpClientPool;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(ftpClientPool, "'ftpClientPool' must not be null");
    }

    public void handleMessage(Message<?> message) {
        Assert.notNull(message, "'message' must not be null");

        Object payload = message.getPayload();
        Assert.notNull(payload, "Message payload must not be null");
        Assert.isInstanceOf(File.class, payload, "Message payload must be an instance of [java.io.File]");

        File file = (File) payload;

        if ((file != null) && file.exists()) {
            FTPClient client = null;
            boolean sentSuccesfully;

            try {
                client = getFtpClient();
                sentSuccesfully = sendFile(file, client);
            } catch (FileNotFoundException e) {
                throw new MessageDeliveryException(message, "File [" + file + "] not found in local working directory; it was moved or deleted unexpectedly", e);
            } catch (IOException e) {
                throw new MessageDeliveryException(message, "Error transferring file [" + file + "] from local working directory to remote FTP directory", e);
            } catch (Exception e) {
                throw new MessageDeliveryException(message, "Error handling message for file [" + file + "]", e);
            } finally {
                if (client != null) {
                    ftpClientPool.releaseClient(client);
                }
            }

            if (!sentSuccesfully) {
                throw new MessageDeliveryException(message, "Failed to store file '" + file + "'");
            }
        }
    }

    private boolean sendFile(File file, FTPClient client)
        throws FileNotFoundException, IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        boolean sent = client.storeFile(file.getName(), fileInputStream);
        fileInputStream.close();
        return sent;
    }

    private FTPClient getFtpClient() throws SocketException, IOException {
        FTPClient client;
        client = this.ftpClientPool.getClient();
        Assert.state(client != null, FTPClientPool.class.getSimpleName() + " returned 'null' client this most likely a bug in the pool implementation.");

        return client;
    }
}
