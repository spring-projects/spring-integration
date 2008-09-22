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

package org.springframework.integration.mail;

import org.springframework.integration.message.MessageHeaders;

/**
 * @author Jonas Partner
 */
public class DefaultMailMessageHeaderMapper extends AbstractMailHeaderMapper {

	@Override
	protected String getSubject(MessageHeaders headers) {
		return this.retrieveAsString(headers, MailHeaders.SUBJECT);
	}

	@Override
	protected String[] getTo(MessageHeaders headers) {
		return this.retrieveAsStringArray(headers, MailHeaders.TO);
	}

	@Override
	protected String[] getCc(MessageHeaders headers) {
		return this.retrieveAsStringArray(headers, MailHeaders.CC);
	}

	@Override
	protected String[] getBcc(MessageHeaders headers) {
		return this.retrieveAsStringArray(headers, MailHeaders.BCC);
	}

	@Override
	protected String getFrom(MessageHeaders headers) {
		return this.retrieveAsString(headers, MailHeaders.FROM);
	}

	@Override
	protected String getReplyTo(MessageHeaders headers) {
		return this.retrieveAsString(headers, MailHeaders.REPLY_TO);
	}

}
