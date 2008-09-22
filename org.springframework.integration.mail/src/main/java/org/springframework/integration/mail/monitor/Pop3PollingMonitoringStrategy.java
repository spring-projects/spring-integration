/*
 * Copyright 2007 the original author or authors.
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

package org.springframework.integration.mail.monitor;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Implementation of the {@link MonitoringStrategy} interface that uses a simple polling mechanism suitable for POP3
 * servers. Since POP3 does not have a native mechanism to determine which messages are "new", this implementation
 * simply retrieves all messages in the {@link Folder}, and delete them afterwards. All messages in the POP3 mailbox are
 * therefore, by definition, new.
 * <p/>
 * Setting the {@link #setDeleteMessages(boolean) deleteMessages} property is therefore ignored: messages are always
 * deleted.
 *
 * @author Arjen Poutsma
 */
public class Pop3PollingMonitoringStrategy extends PollingMonitoringStrategy {

    public Pop3PollingMonitoringStrategy() {
        super.setDeleteMessages(true);
    }

    public void setDeleteMessages(boolean deleteMessages) {
    }

    /**
     * Simply returns {@link Folder#getMessages()}.
     */
    protected Message[] searchForNewMessages(Folder folder) throws MessagingException {
        return folder.getMessages();
    }

    /**
     * Deletes the given messages from the given folder, and closes it to expunge deleted messages.
     *
     * @param folder   the folder to delete messages from
     * @param messages the messages to delete
     * @throws MessagingException in case of JavaMail errors
     */
    protected void deleteMessages(Folder folder, Message[] messages) throws MessagingException {
        super.deleteMessages(folder, messages);
        // expunge deleted mails, and make sure we've retrieved them before closing the folder
        for (int i = 0; i < messages.length; i++) {
            new MimeMessage((MimeMessage) messages[i]);
        }
        MailTransportUtils.closeFolder(folder, true);
    }
}