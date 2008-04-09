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

package org.springframework.integration.adapter.mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.adapter.MessageMappingException;
import org.springframework.integration.message.AbstractMessageMapper;
import org.springframework.integration.message.Message;
import org.springframework.mail.MailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.Assert;

/**
 * Message mapper used for mapping byte array messages to mail messages.
 * Generates an e-mail message with the byte array as an attachment. The
 * multipart mode and attachment name are configurable.
 * 
 * @author Marius Bogoevici
 */
public class ByteArrayMailMessageMapper extends AbstractMessageMapper<byte[], MailMessage> {

	private final JavaMailSender mailSender;

	private volatile int multipartMode = MimeMessageHelper.MULTIPART_MODE_MIXED;

	private volatile String attachmentFilename = "content";


	public ByteArrayMailMessageMapper(JavaMailSender mailSender) {
		Assert.notNull(mailSender, "'mailSender' must not be null");
		this.mailSender = mailSender;
	}


	public void setMultipartMode(int multipartMode) {
		this.multipartMode = multipartMode;
	}

	public void setAttachmentFilename(String attachmentFilename) {
		this.attachmentFilename = attachmentFilename;
	}

	public Message<byte[]> toMessage(MailMessage source) {
		throw new UnsupportedOperationException("mapping from MailMessage to byte array not supported");
	}

	public MailMessage fromMessage(Message<byte[]> message) {
		try {
			MimeMessage mimeMessage = this.mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, this.multipartMode);
			helper.addAttachment(this.attachmentFilename, new ByteArrayResource(message.getPayload()));
			return new MimeMailMessage(helper);
		}
		catch (MessagingException e) {
			throw new MessageMappingException(message, "failed to create MimeMessage", e);
		}
	}

}
