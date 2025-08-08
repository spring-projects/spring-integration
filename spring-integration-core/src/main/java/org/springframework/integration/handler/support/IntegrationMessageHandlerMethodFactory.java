/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.handler.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.KotlinDetector;
import org.springframework.integration.support.NullAwarePayloadArgumentResolver;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolverComposite;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * Extension of the {@link DefaultMessageHandlerMethodFactory} for Spring Integration requirements.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class IntegrationMessageHandlerMethodFactory extends DefaultMessageHandlerMethodFactory {

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final boolean listCapable;

	private MessageConverter messageConverter;

	private BeanFactory beanFactory;

	public IntegrationMessageHandlerMethodFactory() {
		this(false);
	}

	public IntegrationMessageHandlerMethodFactory(boolean listCapable) {
		this.listCapable = listCapable;
	}

	@Override
	public void setMessageConverter(MessageConverter messageConverter) {
		super.setMessageConverter(messageConverter);
		this.messageConverter = messageConverter;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		setCustomArgumentResolvers(buildArgumentResolvers(this.listCapable));
		super.afterPropertiesSet();
	}

	@Override
	protected List<HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = super.initArgumentResolvers();
		this.argumentResolvers.addResolvers(resolvers);
		return resolvers;
	}

	@Override
	public InvocableHandlerMethod createInvocableHandlerMethod(Object bean, Method method) {
		InvocableHandlerMethod handlerMethod = new IntegrationInvocableHandlerMethod(bean, method);
		handlerMethod.setMessageMethodArgumentResolvers(this.argumentResolvers);
		return handlerMethod;
	}

	private List<HandlerMethodArgumentResolver> buildArgumentResolvers(boolean listCapable) {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
		resolvers.add(new PayloadExpressionArgumentResolver());
		resolvers.add(new NullAwarePayloadArgumentResolver(this.messageConverter));
		resolvers.add(new PayloadsArgumentResolver());
		if (listCapable) {
			resolvers.add(new CollectionArgumentResolver(true));
		}
		resolvers.add(new MapArgumentResolver());
		if (KotlinDetector.isKotlinPresent()) {
			resolvers.add(new ContinuationHandlerMethodArgumentResolver());
		}

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
