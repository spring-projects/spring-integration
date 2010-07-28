/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.http;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

/**
 * InboundRequestMapper implementation that binds the request parameter map to
 * a target instance. The target instance may be a non-singleton bean as
 * specified by the {@link #setTargetBeanName(String) 'targetBeanName'}
 * property. Otherwise, this mapper's target type must provide a default,
 * no-arg constructor.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class DataBindingInboundRequestMapper implements InboundRequestMapper, BeanFactoryAware, InitializingBean {

	private volatile Class<?> targetType = Object.class;

	private volatile String targetBeanName;

	private volatile WebBindingInitializer webBindingInitializer;

	private volatile BeanFactory beanFactory;

	private volatile boolean validated;


	public DataBindingInboundRequestMapper() {
		this.targetType = Object.class;
	}

	public DataBindingInboundRequestMapper(Class<?> targetType) {
		Assert.notNull(targetType, "targetType must not be null");
		this.targetType = targetType;
	}


	public void setTargetType(Class<?> targetType) {
		this.targetType = targetType;
	}

	/**
	 * Specify the name of a bean definition to use when creating the target
	 * instance. The bean must <em>not</em> be a singleton, and it must be
	 * compatible with the {@link #targetType}.
	 * <p>If no 'targetBeanName' value is provided, the target type must
	 * provide a default, no-arg constructor. 
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	/**
	 * Specify an optional {@link WebBindingInitializer} to be invoked prior
	 * to the request binding process.
	 */
	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * Provides the {@link BeanFactory} necessary to look up a
	 * {@link #setTargetBeanName(String) 'targetBeanName'} if specified.
	 * This method is typically invoked automatically by the container.
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public final void afterPropertiesSet() {
		if (this.targetBeanName == null && Object.class.equals(this.targetType)) {
			throw new IllegalArgumentException(
					"When no 'targetBeanName' is provided, the 'targetType' must be more specific than Object.");
		}
		this.validateTargetBeanIfNecessary();
	}

	private void validateTargetBeanIfNecessary() {
		if (this.targetBeanName != null && !this.validated) {
			Assert.notNull(this.beanFactory, "beanFactory is required for binding to a bean");
			if (this.beanFactory.isSingleton(this.targetBeanName)) {
				throw new IllegalArgumentException("binding target bean must not be a singleton");
			}
			Class<?> beanType = this.beanFactory.getType(this.targetBeanName);
			if (beanType != null) {
				Assert.isAssignable(this.targetType, beanType);
			}
			this.validated = true;
		}
	}

	@SuppressWarnings("unchecked")
	public Message<?> toMessage(HttpServletRequest request) throws Exception {
		ServletRequestDataBinder binder = new ServletRequestDataBinder(getTarget());
		this.initBinder(binder, request);
		binder.bind(request);
		// this will immediately throw any bind Exceptions
		Map map = binder.close();
		Object payload = map.get(ServletRequestDataBinder.DEFAULT_OBJECT_NAME);
		return MessageBuilder.withPayload(payload).build();
	}

	private void initBinder(ServletRequestDataBinder binder, HttpServletRequest request) {
		if (this.webBindingInitializer != null) {
			this.webBindingInitializer.initBinder(binder, new DispatcherServletWebRequest(request));
		}
	}

	private Object getTarget() throws InstantiationException, IllegalAccessException {
		if (this.targetBeanName != null) {
			this.validateTargetBeanIfNecessary();
			return this.beanFactory.getBean(this.targetBeanName, this.targetType);
		}
		return this.targetType.newInstance();
	}

}
