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

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.SocketException;


/**
 * A {@link org.springframework.integration.core.MessageHandler} implementation that sends files to an FTP server.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FtpSendingMessageHandler implements MessageHandler, InitializingBean {
    private FtpClientPool ftpClientPool;

    public FtpSendingMessageHandler() {
    }

    public FtpSendingMessageHandler(FtpClientPool ftpClientPool) {
        this.ftpClientPool = ftpClientPool;
    }

    public void setFtpClientPool(FtpClientPool ftpClientPool) {
        this.ftpClientPool = ftpClientPool;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(ftpClientPool, "'ftpClientPool' must not be null");
        Assert.notNull(temporaryBufferFolder, "'temporaryBufferFolder' must not be null");
        temporaryBufferFolderFile = this.temporaryBufferFolder.getFile();
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


    public void handleMessage(Message<?> message) throws MessageRejectedException,
            MessageHandlingException, MessageDeliveryException {


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
            } catch (FileNotFoundException e) {
                throw new MessageDeliveryException(message, "File [" + file + "] not found in local working directory; it was moved or deleted unexpectedly", e);
            } catch (IOException e) {
                throw new MessageDeliveryException(message, "Error transferring file [" + file + "] from local working directory to remote FTP directory", e);
            } catch (Exception e) {
                throw new MessageDeliveryException(message, "Error handling message for file [" + file + "]", e);
            } finally {
                if ( file.exists())
                    try {
                        file.delete();
                    } catch (Throwable th) {
                        /// noop
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
        Assert.state(client != null, FtpClientPool.class.getSimpleName() + " returned 'null' client this most likely a bug in the pool implementation.");

        return client;
    }
}
