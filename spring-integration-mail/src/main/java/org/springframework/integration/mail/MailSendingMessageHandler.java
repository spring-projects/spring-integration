/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link org.springframework.messaging.MessageHandler} implementation for sending mail.
 *
 * <p>If the Message is an instance of {@link MailMessage}, it will be passed
 * as-is. If the Message payload is a byte array, it will be passed as an
 * attachment, and in that case, the {@link MailHeaders#ATTACHMENT_FILENAME}
 * header is required. Otherwise, a String type is expected, and its content
 * will be used as the text within a {@link SimpleMailMessage}.
 *
 *
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @see MailHeaders
 */
public class MailSendingMessageHandler extends AbstractMessageHandler {

	private final MailSender mailSender;

	/**
	 * Create a MailSendingMessageHandler.
	 * @param mailSender the {@link MailSender} instance to which this
	 * adapter will delegate.
	 */
	public MailSendingMessageHandler(MailSender mailSender) {
		Assert.notNull(mailSender, "'mailSender' must not be null");
		this.mailSender = mailSender;
	}

	@Override
	public String getComponentType() {
		return "mail:outbound-channel-adapter";
	}

	@Override
	protected final void handleMessageInternal(Message<?> message) {
		MailMessage mailMessage = convertMessageToMailMessage(message);
		if (mailMessage instanceof SimpleMailMessage) {
			this.mailSender.send((SimpleMailMessage) mailMessage);
		}
		else if (mailMessage instanceof MimeMailMessage) {
			Assert.state(this.mailSender instanceof JavaMailSender,
					"this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'");

			((JavaMailSender) this.mailSender).send(((MimeMailMessage) mailMessage).getMimeMessage());
		}
		else {
			throw new IllegalArgumentException(
					"Unsupported MailMessage type [" + mailMessage.getClass().getName() + "].");
		}
	}

	@SuppressWarnings("unchecked")
	private MailMessage convertMessageToMailMessage(Message<?> message) {
		MailMessage mailMessage;
		Object payload = message.getPayload();
		if (payload instanceof MimeMessage) {
			mailMessage = new MimeMailMessage((MimeMessage) payload);
		}
		else if (payload instanceof MailMessage) {
			mailMessage = (MailMessage) payload;
		}
		else if (payload instanceof byte[]) {
			mailMessage = createMailMessageFromByteArrayMessage((Message<byte[]>) message);
		}
		else if (payload instanceof String) {
			String contentType = (String) message.getHeaders().get(MailHeaders.CONTENT_TYPE);
			if (StringUtils.hasText(contentType)) {
				mailMessage = createMailMessageWithContentType((Message<String>) message, contentType);
			}
			else {
				mailMessage = new SimpleMailMessage();
				mailMessage.setText((String) payload);
			}
		}
		else {
			throw new IllegalArgumentException("Unable to create MailMessage from payload type ["
					+ message.getPayload().getClass().getName() + "], " +
					"expected MimeMessage, MailMessage, byte array or String.");
		}
		applyHeadersToMailMessage(mailMessage, message.getHeaders());
		return mailMessage;
	}

	private MailMessage createMailMessageWithContentType(Message<String> message, String contentType) {
		Assert.state(this.mailSender instanceof JavaMailSender,
				"this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'");

		MimeMessage mimeMessage = ((JavaMailSender) this.mailSender).createMimeMessage();
		try {
			mimeMessage.setContent(message.getPayload(), contentType);
			return new MimeMailMessage(mimeMessage);
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to create MimeMessage with contentType: " + contentType, e);
		}
	}

	private MailMessage createMailMessageFromByteArrayMessage(Message<byte[]> message) {
		Assert.state(this.mailSender instanceof JavaMailSender,
				"this adapter requires a 'JavaMailSender' to send a 'MimeMailMessage'");

		String attachmentFileName = message.getHeaders().get(MailHeaders.ATTACHMENT_FILENAME, String.class);
		if (attachmentFileName == null) {
			throw new MessageMappingException(message, "Header '" + MailHeaders.ATTACHMENT_FILENAME
					+ "' is required when mapping a Message with a byte array payload to a MailMessage.");
		}
		Integer multipartMode = message.getHeaders().get(MailHeaders.MULTIPART_MODE, Integer.class);
		if (multipartMode == null) {
			multipartMode = MimeMessageHelper.MULTIPART_MODE_MIXED;
		}

		MimeMessage mimeMessage = ((JavaMailSender) this.mailSender).createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipartMode);
			helper.addAttachment(attachmentFileName, new ByteArrayResource(message.getPayload()));
			return new MimeMailMessage(helper);
		}
		catch (MessagingException e) {
			throw new MessageMappingException(message, "failed to create MimeMessage", e);
		}
	}

	private void applyHeadersToMailMessage(MailMessage mailMessage, MessageHeaders headers) {
		String subject = headers.get(MailHeaders.SUBJECT, String.class);
		if (subject != null) {
			mailMessage.setSubject(subject);
		}
		String[] to = retrieveHeaderValueAsStringArray(headers, MailHeaders.TO);
		if (to != null) {
			mailMessage.setTo(to);
		}
		if (mailMessage instanceof SimpleMailMessage) {
			Assert.state(!ObjectUtils.isEmpty(((SimpleMailMessage) mailMessage).getTo()),
					"No recipient has been provided on the MailMessage or the 'MailHeaders.TO' header.");
		}
		String[] cc = retrieveHeaderValueAsStringArray(headers, MailHeaders.CC);
		if (cc != null) {
			mailMessage.setCc(cc);
		}
		String[] bcc = retrieveHeaderValueAsStringArray(headers, MailHeaders.BCC);
		if (bcc != null) {
			mailMessage.setBcc(bcc);
		}
		String from = headers.get(MailHeaders.FROM, String.class);
		if (from != null) {
			mailMessage.setFrom(from);
		}
		String replyTo = headers.get(MailHeaders.REPLY_TO, String.class);
		if (replyTo != null) {
			mailMessage.setReplyTo(replyTo);
		}
	}

	private String[] retrieveHeaderValueAsStringArray(MessageHeaders headers, String key) {
		Object value = headers.get(key);
		String[] returnedHeaders = null;
		if (value != null) {
			if (value instanceof String[]) {
				returnedHeaders = (String[]) value;
			}
			else if (value instanceof String) {
				returnedHeaders = StringUtils.commaDelimitedListToStringArray((String) value);
			}
		}
		if (returnedHeaders == null || ObjectUtils.isEmpty(returnedHeaders)) {
			returnedHeaders = null;
		}
		return returnedHeaders;
	}

}
