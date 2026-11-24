/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	@SuppressWarnings("this-escape")
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
