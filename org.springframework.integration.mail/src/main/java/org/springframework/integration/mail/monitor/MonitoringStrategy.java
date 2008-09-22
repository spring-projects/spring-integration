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

/**
 * Defines the contract for objects that monitor a given folder for new messages. Allows for multiple implementation
 * strategies, including polling, or event-driven techniques such as IMAP's <code>IDLE</code> command.
 *
 * @author Arjen Poutsma
 */
public interface MonitoringStrategy {

    /**
     * Monitors the given folder, and returns any new messages when they arrive.
     *
     * @param folder the folder in which to look for new messages
     * @return the new messages
     * @throws MessagingException   in case of JavaMail errors
     * @throws InterruptedException if a thread is interrupted
     */
    Message[] receive(Folder folder) throws MessagingException, InterruptedException;

    /**
     * Returns the folder open mode to be used by this strategy. Can be either {@link Folder#READ_ONLY} or {@link
     * Folder#READ_WRITE}.
     */
    int getFolderOpenMode();

}
