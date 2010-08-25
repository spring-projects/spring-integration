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

package org.springframework.integration.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This component is responsible for maintaining the history of {@link MessageChannel}s and 
 * {@link MessageHandler}s. There can only be one instance of this class per ApplicationContext
 * hierarchy otherwise an Exception will be thrown.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class MessageHistoryWriter implements BeanFactoryAware, InitializingBean {

	public static final String NAME_PROPERTY = "name";

	public static final String TYPE_PROPERTY = "type";

	public static final String TIMESTAMP_PROPERTY = "timestamp";


	private volatile BeanFactory beanFactory;


	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.beanFactory, "BeanFactory is required");
		if (BeanFactoryUtils.beansOfTypeIncludingAncestors((ListableBeanFactory) this.beanFactory, MessageHistoryWriter.class).size() > 1) {
			throw new IllegalArgumentException("more than one MessageHistoryWriter exists in the context");
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> Message<T> writeHistory(NamedComponent component, Message<T> message) {
		if (component != null && message != null) {
			String componentName = component.getComponentName();
			if (componentName != null && !componentName.startsWith("org.springframework.integration")) {
				Properties historyEvent = new Properties();
				String componentType = component.getComponentType();
				if (StringUtils.hasText(componentType)) {
					historyEvent.setProperty(TYPE_PROPERTY, componentType);
				}
				historyEvent.setProperty(NAME_PROPERTY, componentName);
				historyEvent.setProperty(TIMESTAMP_PROPERTY, "" + System.currentTimeMillis());
				List history = message.getHeaders().get(MessageHeaders.HISTORY, List.class);
				if (history == null) {
					history = new ArrayList();
				}
				history.add(historyEvent);
				message = MessageBuilder.fromMessage(message).setHeader(MessageHeaders.HISTORY, history).build();
			}
		}
		return message;
	}

}
