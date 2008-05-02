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

package org.springframework.integration.bus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.Assert;

/**
 * A bean post processor which injects all {@link MessageBusAware} beans with a 
 * reference to the {@link MessageBus}.
 * 
 * @author Marius Bogoevici
 *
 */
public class MessageBusAwareBeanPostProcessor implements BeanPostProcessor {

	private final MessageBus messageBus;
	
	public MessageBusAwareBeanPostProcessor(MessageBus messageBus) {
		Assert.notNull(messageBus, "'messageBus' must not be null");
		this.messageBus = messageBus;
	}
	
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MessageBusAware) {
			((MessageBusAware) bean).setMessageBus(messageBus);
		}
		return bean;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	
}
