/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * {@link MessageSource} implementation that delegates to a
 * {@link MailReceiver} to poll a mailbox. Each poll of the mailbox may
 * return more than one message which will then be stored in a queue.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 */
public class MailReceivingMessageSource implements MessageSource<javax.mail.Message>,
		BeanFactoryAware, BeanNameAware, NamedComponent {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final MailReceiver mailReceiver;

	private final Queue<javax.mail.Message> mailQueue = new ConcurrentLinkedQueue<javax.mail.Message>();

	private volatile BeanFactory beanFactory;

	private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private volatile String beanName;


	public MailReceivingMessageSource(MailReceiver mailReceiver) {
		Assert.notNull(mailReceiver, "mailReceiver must not be null");
		this.mailReceiver = mailReceiver;
	}

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
	}

	protected BeanFactory getBeanFactory() {
		return beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return messageBuilderFactory;
	}

	@Override
	public String getComponentName() {
		return this.beanName;
	}

	@Override
	public String getComponentType() {
		return "mail:inbound-channel-adapter";
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public Message<javax.mail.Message> receive() {
		try {
			javax.mail.Message mailMessage = this.mailQueue.poll();
			if (mailMessage == null) {
				javax.mail.Message[] messages = this.mailReceiver.receive();
				if (messages != null) {
					this.mailQueue.addAll(Arrays.asList(messages));
				}
				mailMessage = this.mailQueue.poll();
			}
			if (mailMessage != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("received mail message [" + mailMessage + "]");
				}
				return this.messageBuilderFactory.withPayload(mailMessage).build();
			}
		}
		catch (Exception e) {
			throw new MessagingException("failure occurred while polling for mail", e);
		}
		return null;
	}

}
