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

import org.jspecify.annotations.Nullable;

/**
 * A {@link org.springframework.integration.mail.inbound.MailReceiver} implementation that polls a mail server using the
 * POP3 protocol.
 *
 * @author Arjen Poutsma
 * @author Mark Fisher
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.mail.inbound.Pop3MailReceiver}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class Pop3MailReceiver extends org.springframework.integration.mail.inbound.Pop3MailReceiver {

	public Pop3MailReceiver() {
	}

	public Pop3MailReceiver(@Nullable String url) {
		super(url);
	}

	public Pop3MailReceiver(String host, String username, String password) {
		super(host, username, password);
	}

	public Pop3MailReceiver(String host, int port, String username, String password) {
		super(host, port, username, password);
	}

}
