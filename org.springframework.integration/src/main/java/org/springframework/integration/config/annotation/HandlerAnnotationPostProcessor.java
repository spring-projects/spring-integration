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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.aggregator.AggregatorMessageHandlerCreator;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Handler;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.DefaultEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.integration.handler.config.DefaultMessageHandlerCreator;
import org.springframework.integration.handler.config.MessageHandlerCreator;
import org.springframework.integration.router.RouterMessageHandlerCreator;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.splitter.SplitterMessageHandlerCreator;
import org.springframework.integration.transformer.config.TransformerMessageHandlerCreator;
import org.springframework.util.StringUtils;

/**
 * Post-processor for the {@link Handler @Handler} annotation.
 * 
 * @author Mark Fisher
 */
public class HandlerAnnotationPostProcessor extends AbstractAnnotationMethodPostProcessor<MessageHandler> {

	private final Map<Class<? extends Annotation>, MessageHandlerCreator> handlerCreators =
			new ConcurrentHashMap<Class<? extends Annotation>, MessageHandlerCreator>();

	private final MessageHandlerCreator defaultHandlerCreator = new DefaultMessageHandlerCreator();


	public HandlerAnnotationPostProcessor(MessageBus messageBus, ClassLoader beanClassLoader) {
		super(Handler.class, messageBus, beanClassLoader);
		this.handlerCreators.put(Router.class, new RouterMessageHandlerCreator());
		this.handlerCreators.put(Splitter.class, new SplitterMessageHandlerCreator());
		this.handlerCreators.put(Aggregator.class, new AggregatorMessageHandlerCreator(messageBus));
		this.handlerCreators.put(Transformer.class, new TransformerMessageHandlerCreator());
	}


	public void setCustomHandlerCreators(Map<Class<? extends Annotation>, MessageHandlerCreator> customHandlerCreators) {
		for (Map.Entry<Class<? extends Annotation>, MessageHandlerCreator> entry : customHandlerCreators.entrySet()) {
			this.handlerCreators.put(entry.getKey(), entry.getValue());
		}
	}

	protected MessageHandler processMethod(Object bean, Method method, Annotation annotation) {
		MessageHandlerCreator handlerCreator = this.handlerCreators.get(annotation.annotationType());
		if (handlerCreator == null) {
			handlerCreator = this.defaultHandlerCreator;
			if (logger.isDebugEnabled()) {
				logger.debug("No handler creator has been registered for handler annotation '"
						+ annotation.annotationType() + "', using DefaultMessageHandlerCreator.");
			}
		}
		Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);
		Order order = AnnotationUtils.findAnnotation(method, Order.class);
		if (order != null) {
			attributes.put("order", order.value());
		}
		MessageHandler handler = handlerCreator.createHandler(bean, method, attributes);
		if (handler != null) {
			if (handler instanceof ChannelRegistryAware) {
				((ChannelRegistryAware) handler).setChannelRegistry(this.getMessageBus());
			}
			if (handler instanceof InitializingBean) {
				try {
					((InitializingBean) handler).afterPropertiesSet();
				}
				catch (Exception e) {
					throw new ConfigurationException("failed to initialize handler", e);
				}
			}
		}
		return handler;
	}

	@SuppressWarnings("unchecked")
	protected MessageHandler processResults(List<MessageHandler> results) {
		MessageHandlerChain handlerChain = new MessageHandlerChain();
		for (MessageHandler handler : results) {
			handlerChain.add(handler);
		}
		if (handlerChain.getHandlers().size() == 0) {
			return null;
		}
		if (handlerChain.getHandlers().size() == 1) {
			return handlerChain.getHandlers().get(0);
		}
		List<MessageHandler> handlers = new ArrayList<MessageHandler>(handlerChain.getHandlers());
		Collections.sort(handlers, new OrderComparator());
		handlerChain.setHandlers(handlers);
		return handlerChain;
	}

	public MessageEndpoint createEndpoint(Object bean, String beanName, Class<?> originalBeanClass,
			org.springframework.integration.annotation.MessageEndpoint endpointAnnotation) {
		DefaultEndpoint<MessageHandler> endpoint = new DefaultEndpoint<MessageHandler>((MessageHandler) bean);
		String outputChannelName = endpointAnnotation.output();
		if (StringUtils.hasText(outputChannelName)) {
			endpoint.setOutputChannelName(outputChannelName);
		}
		String inputChannelName = endpointAnnotation.input();
		if (StringUtils.hasText(inputChannelName)) {
			endpoint.setInputChannelName(inputChannelName);
		}
		Schedule schedule = this.extractSchedule(originalBeanClass);
		if (schedule != null) {
			endpoint.setSchedule(schedule);
		}
		return endpoint;
	}

}
