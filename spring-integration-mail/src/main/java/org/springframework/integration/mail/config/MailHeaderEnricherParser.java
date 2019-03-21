/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.mail.config;

import org.springframework.integration.config.xml.HeaderEnricherParserSupport;
import org.springframework.integration.mail.MailHeaders;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MailHeaderEnricherParser extends HeaderEnricherParserSupport {

	public MailHeaderEnricherParser() {
		this.addElementToHeaderMapping("subject", MailHeaders.SUBJECT);
		this.addElementToHeaderMapping("to", MailHeaders.TO);
		this.addElementToHeaderMapping("cc", MailHeaders.CC);
		this.addElementToHeaderMapping("bcc", MailHeaders.BCC);
		this.addElementToHeaderMapping("from", MailHeaders.FROM);
		this.addElementToHeaderMapping("reply-to", MailHeaders.REPLY_TO);
		this.addElementToHeaderMapping("content-type", MailHeaders.CONTENT_TYPE);
		this.addElementToHeaderMapping("attachment-filename", MailHeaders.ATTACHMENT_FILENAME);
		this.addElementToHeaderMapping("multipart-mode", MailHeaders.MULTIPART_MODE);
	}

}
