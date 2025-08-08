/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aop;

import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Post-processes beans that contain the
 * method-level @{@link org.springframework.integration.annotation.Publisher} annotation.
 * <p>
 * Only one bean instance of this processor can be declared in the application context, manual
 * or automatic by thr framework via annotation or XML processing.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Rick Hogge
 *
 * @since 2.0
 */
@SuppressWarnings("serial")
public class PublisherAnnotationBeanPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor
		implements BeanNameAware, SmartInitializingSingleton {

	private String defaultChannelName;

	private String beanName;

	private BeanFactory beanFactory;

	/**
	 * Set the default channel where Messages should be sent if the annotation
	 * itself does not provide a channel.
	 * @param defaultChannelName the publisher interceptor defaultChannel
	 * @since 4.0.3
	 */
	public void setDefaultChannelName(String defaultChannelName) {
		this.defaultChannelName = defaultChannelName;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		super.setBeanFactory(beanFactory);
		PublisherAnnotationAdvisor publisherAnnotationAdvisor = new PublisherAnnotationAdvisor();
		publisherAnnotationAdvisor.setBeanFactory(beanFactory);
		publisherAnnotationAdvisor.setDefaultChannelName(this.defaultChannelName);
		this.advisor = publisherAnnotationAdvisor;
	}

	@Override
	public void afterSingletonsInstantiated() {
		try {
			this.beanFactory.getBean(PublisherAnnotationBeanPostProcessor.class);
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw new BeanCreationException(this.beanName,
					"Only one 'PublisherAnnotationBeanPostProcessor' bean can be defined in the application context." +
							" Do not use '@EnablePublisher' (or '<int:enable-publisher>') if you declare a" +
							" 'PublisherAnnotationBeanPostProcessor' bean definition manually.", ex);
		}
	}

}
