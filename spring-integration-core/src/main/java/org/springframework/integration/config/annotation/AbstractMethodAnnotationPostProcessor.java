/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation> implements MethodAnnotationPostProcessor<T> {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";

	private static final String ADVICE_CHAIN_ATTRIBUTE = "adviceChain";


	protected final BeanFactory beanFactory;

	protected final ChannelResolver channelResolver;


	public AbstractMethodAnnotationPostProcessor(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
	}


	public Object postProcess(Object bean, String beanName, Method method, T annotation) {
		MessageHandler handler = this.createHandler(bean, method, annotation);
		setAdviceChainIfPresent(beanName, annotation, handler);
		if (handler instanceof Orderable) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				((Orderable) handler).setOrder(orderAnnotation.value());
			}
		}
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			handler = (MessageHandler) ((ConfigurableListableBeanFactory) beanFactory).initializeBean(handler, "_initHandlerFor_" + beanName);
		}
		AbstractEndpoint endpoint = this.createEndpoint(handler, annotation);
		if (endpoint != null) {
			return endpoint;
		}
		return handler;
	}


	protected final void setAdviceChainIfPresent(String beanName, T annotation, MessageHandler handler) {
		String[] adviceChainNames = (String[]) AnnotationUtils.getValue(annotation, ADVICE_CHAIN_ATTRIBUTE);
		if (adviceChainNames != null && adviceChainNames.length > 0) {
			List<Advice> adviceChain = new ArrayList<Advice>();
			for (String adviceChainName : adviceChainNames) {
				Object adviceChainBean = this.beanFactory.getBean(adviceChainName);
				if (adviceChainBean instanceof Advice) {
					adviceChain.add((Advice) adviceChainBean);
				}
				else if (adviceChainBean instanceof Advice[]) {
					for (Advice advice : (Advice[]) adviceChainBean) {
						adviceChain.add(advice);
					}
				}
				else if (adviceChainBean instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<Advice> adviceChainEntries = (Collection<Advice>) adviceChainBean;
					for (Advice advice : adviceChainEntries) {
						adviceChain.add(advice);
					}
				}
				else {
					throw new IllegalArgumentException("Invalid advice chain type:" +
						adviceChainName.getClass().getName() + " for bean '" + beanName + "'");
				}
			}
			if (handler instanceof AbstractReplyProducingMessageHandler) {
				((AbstractReplyProducingMessageHandler) handler).setAdviceChain(adviceChain);
			}
			else {
				throw new IllegalArgumentException("Cannot apply advice chain to " + handler.getClass().getName());
			}
		}
	}

	protected boolean shouldCreateEndpoint(T annotation) {
		return (StringUtils.hasText((String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE)));
	}

	private AbstractEndpoint createEndpoint(MessageHandler handler, T annotation) {
		AbstractEndpoint endpoint = null;
		String inputChannelName = (String) AnnotationUtils.getValue(annotation, INPUT_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel = this.channelResolver.resolveChannelName(inputChannelName);
			Assert.notNull(inputChannel, "failed to resolve inputChannel '" + inputChannelName + "'");
			Assert.isTrue(inputChannel instanceof SubscribableChannel,
					"The input channel for an Annotation-based endpoint must be a SubscribableChannel.");
			endpoint = new EventDrivenConsumer((SubscribableChannel) inputChannel, handler);
			if (handler instanceof BeanFactoryAware) {
				((BeanFactoryAware) handler).setBeanFactory(this.beanFactory);
			}
		}
		return endpoint;
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 */
	protected abstract MessageHandler createHandler(Object bean, Method method, T annotation);

}
