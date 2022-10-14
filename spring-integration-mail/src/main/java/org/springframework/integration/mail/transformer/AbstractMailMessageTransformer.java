/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.mail.transformer;

import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.mail.support.MailUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;

/**
 * Base class for Transformers that convert from a JavaMail Message to a
 * Spring Integration Message.
 *
 * @param <T> the target payload type.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractMailMessageTransformer<T> implements Transformer, BeanFactoryAware {

	private BeanFactory beanFactory;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactorySet;

	@Override
	public final void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (!this.messageBuilderFactorySet) {
			if (this.beanFactory != null) {
				this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
			}
			this.messageBuilderFactorySet = true;
		}
		return this.messageBuilderFactory;
	}

	@Override
	public Message<?> transform(Message<?> message) {
		Object payload = message.getPayload();
		if (!(payload instanceof jakarta.mail.Message mailMessage)) {
			throw new MessageTransformationException(message, getClass().getSimpleName()
					+ " requires a jakarta.mail.Message payload");
		}
		AbstractIntegrationMessageBuilder<T> builder;
		builder = doTransform(mailMessage);
		if (builder == null) {
			throw new MessageTransformationException(message, "failed to transform mail message");
		}
		return builder.copyHeaders(extractHeaderMapFromMailMessage(mailMessage)).build();
	}

	protected abstract AbstractIntegrationMessageBuilder<T> doTransform(jakarta.mail.Message mailMessage);

	private static Map<String, Object> extractHeaderMapFromMailMessage(jakarta.mail.Message mailMessage) {
		return MailUtils.extractStandardHeaders(mailMessage);
	}

}
