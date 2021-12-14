/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.handler.support.CollectionArgumentResolver;
import org.springframework.integration.handler.support.MapArgumentResolver;
import org.springframework.integration.handler.support.PayloadExpressionArgumentResolver;
import org.springframework.integration.handler.support.PayloadsArgumentResolver;
import org.springframework.integration.support.NullAwarePayloadArgumentResolver;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * The {@link FactoryBean} for creating integration-specific {@link MessageHandlerMethodFactory} instance.
 * It adds these custom {@link HandlerMethodArgumentResolver}s in the order:
 * <ul>
 *  <li>{@link PayloadExpressionArgumentResolver};
 *  <li>{@link NullAwarePayloadArgumentResolver};
 *  <li>{@link PayloadsArgumentResolver};
 *  <li>{@link CollectionArgumentResolver} if {@link #listCapable} is true;
 *  <li>{@link MapArgumentResolver}.
 * </ul>
 *
 * @author Artyem Bilan
 *
 * @since 5.5.7
 */
class MessageHandlerMethodFactoryCreatingFactoryBean
		implements FactoryBean<MessageHandlerMethodFactory>, BeanFactoryAware {

	private final boolean listCapable;

	private MessageConverter argumentResolverMessageConverter;

	private BeanFactory beanFactory;

	MessageHandlerMethodFactoryCreatingFactoryBean(boolean listCapable) {
		this.listCapable = listCapable;
	}

	public void setArgumentResolverMessageConverter(MessageConverter argumentResolverMessageConverter) {
		this.argumentResolverMessageConverter = argumentResolverMessageConverter;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return MessageHandlerMethodFactory.class;
	}

	@Override
	public MessageHandlerMethodFactory getObject() {
		DefaultMessageHandlerMethodFactory handlerMethodFactory = new DefaultMessageHandlerMethodFactory();
		handlerMethodFactory.setBeanFactory(this.beanFactory);
		handlerMethodFactory.setMessageConverter(this.argumentResolverMessageConverter);
		handlerMethodFactory.setCustomArgumentResolvers(buildArgumentResolvers(this.listCapable));
		handlerMethodFactory.afterPropertiesSet();
		return handlerMethodFactory;
	}

	private List<HandlerMethodArgumentResolver> buildArgumentResolvers(boolean listCapable) {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		resolvers.add(new PayloadExpressionArgumentResolver());
		resolvers.add(new NullAwarePayloadArgumentResolver(this.argumentResolverMessageConverter));
		resolvers.add(new PayloadsArgumentResolver());
		if (listCapable) {
			resolvers.add(new CollectionArgumentResolver(true));
		}
		resolvers.add(new MapArgumentResolver());
		for (HandlerMethodArgumentResolver resolver : resolvers) {
			if (resolver instanceof BeanFactoryAware) {
				((BeanFactoryAware) resolver).setBeanFactory(this.beanFactory);
			}
			if (resolver instanceof InitializingBean) {
				try {
					((InitializingBean) resolver).afterPropertiesSet();
				}
				catch (Exception ex) {
					throw new BeanInitializationException("Cannot initialize 'HandlerMethodArgumentResolver'", ex);
				}
			}
		}
		return resolvers;
	}

}
