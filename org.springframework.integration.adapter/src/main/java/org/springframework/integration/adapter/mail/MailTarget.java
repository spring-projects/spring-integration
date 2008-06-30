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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.MessageTarget;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.util.Assert;

/**
 * A target adapter for sending mail.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class MailTarget implements MessageTarget, InitializingBean {

	private final JavaMailSender mailSender;

	private volatile MailHeaderGenerator mailHeaderGenerator = new DefaultMailHeaderGenerator();

	private volatile MessageMapper<String, MailMessage> textMessageMapper;

	private volatile MessageMapper<byte[], MailMessage> byteArrayMessageMapper;

	private volatile MessageMapper<Object, MailMessage> objectMessageMapper;


	/**
	 * Create a MailTargetAdapter.
	 * 
	 * @param mailSender the {@link JavaMailSender} instance to which this
	 * adapter will delegate.
	 */
	public MailTarget(JavaMailSender mailSender) {
		Assert.notNull(mailSender, "'mailSender' must not be null");
		this.mailSender = mailSender;
	}


	public void afterPropertiesSet() throws Exception {
		this.textMessageMapper = (this.textMessageMapper != null) ?
				this.textMessageMapper : new TextMailMessageMapper();
		this.byteArrayMessageMapper = (byteArrayMessageMapper != null) ?
				this.byteArrayMessageMapper : new ByteArrayMailMessageMapper(this.mailSender);
		this.objectMessageMapper = (objectMessageMapper != null) ?
				this.objectMessageMapper : new DefaultObjectMailMessageMapper();
	}

	public void setHeaderGenerator(MailHeaderGenerator mailHeaderGenerator) {
		Assert.notNull(mailHeaderGenerator, "'mailHeaderGenerator' must not be null");
		this.mailHeaderGenerator = mailHeaderGenerator;
	}

	public void setTextMessageMapper(MessageMapper<String, MailMessage> textMessageMapper) {
		this.textMessageMapper = textMessageMapper;
	}

	public void setByteArrayMessageMapper(MessageMapper<byte[], MailMessage> byteArrayMessageMapper) {
		this.byteArrayMessageMapper = byteArrayMessageMapper;
	}

	public void setObjectMessageMapper(MessageMapper<Object, MailMessage> objectMessageMapper) {
		this.objectMessageMapper = objectMessageMapper;
	}

	public final boolean send(Message<?> message) {
		MailMessage mailMessage = this.convertMessageToMailMessage(message);
		this.mailHeaderGenerator.populateMailMessageHeader(mailMessage, message);
		this.sendMailMessage(mailMessage);
		return true;
	}

	@SuppressWarnings("unchecked")
	private MailMessage convertMessageToMailMessage(Message<?> message) {
		if (message.getPayload() instanceof String) {
			return this.textMessageMapper.mapMessage((Message<String>) message);
		}
		else if (message.getPayload() instanceof byte[]) {
			return this.byteArrayMessageMapper.mapMessage((Message<byte[]>) message);
		}
		return this.objectMessageMapper.mapMessage((Message<Object>) message);
	}

	private void sendMailMessage(MailMessage mailMessage) {
		if (mailMessage instanceof SimpleMailMessage) {
			this.mailSender.send((SimpleMailMessage) mailMessage);
		}
		else if (mailMessage instanceof MimeMailMessage) {
			this.mailSender.send(((MimeMailMessage) mailMessage).getMimeMessage());
		}
		else {
			throw new IllegalArgumentException(
					"MailMessage subclass '" + mailMessage.getClass().getName() + "' not supported");
		}
	}


	private static class DefaultObjectMailMessageMapper implements MessageMapper<Object, MailMessage> {

		public Message<Object> toMessage(MailMessage source) {
			throw new UnsupportedOperationException("mapping from MailMessage to Object not supported");
		}

		public MailMessage mapMessage(Message<Object> objectMessage) {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setText(objectMessage.getPayload().toString());
			return message;
		}

	}

}
