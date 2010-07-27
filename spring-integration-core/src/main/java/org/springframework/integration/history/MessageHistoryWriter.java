/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.history;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageHandler;

/**
 * This components is responsible for maintaining the history of {@link MessageChannel}s and 
 * {@link MessageHandler}s
 * There can only be ine instance of this class per ApplicationContext hierarchy 
 * otherwise the Exception will be thrown.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MessageHistoryWriter implements BeanFactoryAware, InitializingBean{

	private BeanFactory beanFactory;

	public void writeHistory(NamedComponent component, Message<?> message) {
		if (message != null) {
			message.getHeaders().getHistory().addEvent(component);
		}
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		if (BeanFactoryUtils.beansOfTypeIncludingAncestors((ListableBeanFactory)this.beanFactory, MessageHistoryWriter.class).size() > 1){
			throw new IllegalArgumentException("Attempt to register more then one MessageHistoryWriter");
		}
	}

}
