/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mail;

import org.eclipse.angus.mail.imap.IMAPFolder;
import org.jspecify.annotations.Nullable;

/**
 * A {@link org.springframework.integration.mail.inbound.MailReceiver} implementation for receiving mail messages from a
 * mail server that supports the IMAP protocol. In addition to the pollable
 * {@link #receive()} method, the {@link #waitForNewMessages()} method provides
 * the option of blocking until new messages are available prior to calling
 * {@link #receive()}. That option is only available if the server supports
 * the {@link IMAPFolder#idle() idle} command.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.ImapMailReceiver}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ImapMailReceiver extends org.springframework.integration.mail.inbound.ImapMailReceiver {

	public ImapMailReceiver() {
	}

	public ImapMailReceiver(@Nullable String url) {
		super(url);
	}

}
