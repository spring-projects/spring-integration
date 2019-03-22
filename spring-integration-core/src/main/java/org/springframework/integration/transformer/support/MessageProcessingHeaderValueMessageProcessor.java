/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.integration.transformer.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 3.0
 */
public class MessageProcessingHeaderValueMessageProcessor extends AbstractHeaderValueMessageProcessor<Object>
		implements BeanFactoryAware {

	private final MessageProcessor<?> targetProcessor;

	public <T> MessageProcessingHeaderValueMessageProcessor(MessageProcessor<T> targetProcessor) {
		this.targetProcessor = targetProcessor;
	}

	public MessageProcessingHeaderValueMessageProcessor(Object targetObject) {
		this(targetObject, null);
	}

	public MessageProcessingHeaderValueMessageProcessor(Object targetObject, String method) {
		this.targetProcessor = new MethodInvokingMessageProcessor<Object>(targetObject, method);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (this.targetProcessor instanceof BeanFactoryAware) {
			((BeanFactoryAware) this.targetProcessor).setBeanFactory(beanFactory);
		}
	}

	public Object processMessage(Message<?> message) {
		return this.targetProcessor.processMessage(message);
	}

}
