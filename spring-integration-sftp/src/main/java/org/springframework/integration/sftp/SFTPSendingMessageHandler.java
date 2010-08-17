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

import org.springframework.beans.factory.InitializingBean;

import org.springframework.integration.*;
import org.springframework.integration.core.MessageHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * Sending a message payload to a remote SFTP endpoint. For now, we assume that the payload of the inbound message is of
 * type {@link java.io.File}. Perhaps we could support a payload of java.io.InputStream with a Header designating the file
 * name?
 *
 * @author Josh Long
 */
public class SFTPSendingMessageHandler implements MessageHandler, InitializingBean {
    private SFTPSessionPool pool;
    private String remoteDirectory;
    private volatile boolean afterPropertiesSetRan;

    public SFTPSendingMessageHandler(SFTPSessionPool pool) {
        this.pool = pool;
    }

    public void afterPropertiesSet() throws Exception {
        assert this.pool != null : "the pool can't be null!";

        //        logger.debug("afterPropertiesSet() called on SFTPSendingMessageHandler");
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

    public void handleMessage(final Message<?> message)
        throws MessageRejectedException, MessageHandlingException, MessageDeliveryException {
        assert this.pool != null : "need a working pool";
        assert message.getPayload() instanceof File : "the payload needs to be java.io.File";

        try {
            File inboundFilePayload = (File) message.getPayload();

            if ((inboundFilePayload != null) && inboundFilePayload.exists()) {
                sendFileToRemoteEndpoint(message, inboundFilePayload);
            }
        } catch (Throwable thr) {
            //   logger.debug("recieved an exception.", thr);
            throw new MessageDeliveryException(message, "couldn't deliver the message!", thr);
        }
    }

    public void setRemoteDirectory(final String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }

    private boolean sendFileToRemoteEndpoint(Message<?> message, File file)
        throws Throwable {
        assert this.pool != null : "need a working pool";

        SFTPSession session = this.pool.getSession();

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

                if ((messageHeaders != null) && messageHeaders.containsKey(SFTPConstants.SFTP_REMOTE_DIRECTORY_HEADER)) {
                    dynRd = (String) messageHeaders.get(SFTPConstants.SFTP_REMOTE_DIRECTORY_HEADER);

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
