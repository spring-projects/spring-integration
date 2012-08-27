/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.mail;

import javax.mail.Folder;
import javax.mail.Message;

import org.springframework.util.Assert;


/**
 * Strategy interface for receiving mail {@link javax.mail.Message Messages}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public interface MailReceiver {

	javax.mail.Message[] receive() throws javax.mail.MessagingException;

//	MailReceiverContext getTransactionContext();
//
//	void closeContextAfterSuccess(MailReceiverContext context);
//
//	void closeContextAfterFailure(MailReceiverContext context);

	public static class MailReceiverContext {

		private final Folder folder;

		private volatile Message[] messages = new Message[0];

		MailReceiverContext(Folder folder) {
			this.folder = folder;
		}

		Message[] getMessages() {
			return messages;
		}

		void setMessages(Message[] messages) {
			Assert.noNullElements(messages, "messages cannot be null");
			this.messages = messages;
		}

		Folder getFolder() {
			return folder;
		}

	}
}
