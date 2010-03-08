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

package org.springframework.integration.control;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.assembler.AbstractConfigurableMBeanInfoAssembler;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * JMX-based Control Bus implementation. Exports all channel and endpoint
 * beans from a given BeanFactory as MBeans.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ControlBus implements BeanFactoryAware, InitializingBean {

	public static final String DEFAULT_DOMAIN = "org.springframework.integration";


	private final MBeanExporter exporter;

	private final String domain;

	private volatile ListableBeanFactory beanFactory;

	private final Set<Class<?>> managedTypes = new HashSet<Class<?>>(
			Arrays.asList(new Class<?>[] { MessageChannel.class, AbstractEndpoint.class }));


	/**
	 * Create a {@link ControlBus} that will register channels and endpoints
	 * as MBeans with the given MBeanServer using the default domain name.
	 * @see #DEFAULT_DOMAIN
	 */
	public ControlBus(MBeanServer server) {
		this(server, null);
	}

	/**
	 * Create a {@link ControlBus} that will register channels and endpoints
	 * as MBeans with the given MBeanServer using the specified domain name.
	 */
	public ControlBus(MBeanServer server, String domain) {
		Assert.notNull(server, "MBeanServer must not be null.");
		this.domain = (domain != null) ? domain : DEFAULT_DOMAIN;
		Assert.isTrue(!ObjectUtils.containsElement(server.getDomains(), this.domain),
				"Domain [" + this.domain + "] is already in use within this MBeanServer.");
		MBeanExporter exporter = new MBeanExporter();
		exporter.setServer(server);
		exporter.setAutodetect(false);
		exporter.setAssembler(new ControlBusMBeanInfoAssembler());
		this.exporter = exporter;
	}


	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isTrue(beanFactory instanceof ListableBeanFactory,
				"A ListableBeanFactory is required.");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		this.exporter.afterPropertiesSet();
		for (Class<?> type : this.managedTypes) {
			Map<String, ?> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type);
			for (Map.Entry<String, ?> entry : beans.entrySet()) {
				Object bean = entry.getValue();
				String beanName = entry.getKey();
				Class<?> beanType = bean.getClass();
				try {
					this.exporter.registerManagedResource(bean, this.generateObjectName(beanName, beanType));
				}
				catch (MalformedObjectNameException e) {
					throw new BeanInitializationException("Failed to generate JMX ObjectName.", e);
				}
			}
		}
	}

	private ObjectName generateObjectName(String beanName, Class<?> beanType) throws MalformedObjectNameException {
		StringBuilder sb = new StringBuilder(this.domain + ":name=" + beanName + ",");
		if (MessageChannel.class.isAssignableFrom(beanType)) {
			sb.append("type=channel");
		}
		else if (AbstractEndpoint.class.isAssignableFrom(beanType)) {
			sb.append("type=endpoint");
		}
		else {
			sb.append("type=" + ClassUtils.getShortNameAsProperty(beanType));
		}
		return ObjectNameManager.getInstance(sb.toString());
	}


	/**
	 * An {@link MBeanInfoAssembler} implementation for channels and endpoints.
	 */
	private static class ControlBusMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

		@Override
		protected boolean includeOperation(Method method, String beanKey) {
			Class<?> declaringClass = method.getDeclaringClass();
			return this.shouldInclude(method, declaringClass);
		}

		@Override
		protected boolean includeReadAttribute(Method method, String beanKey) {
			Class<?> declaringClass = method.getDeclaringClass();
			return this.shouldInclude(method, declaringClass);
		}

		@Override
		protected boolean includeWriteAttribute(Method method, String beanKey) {
			Class<?> declaringClass = method.getDeclaringClass();
			return this.shouldInclude(method, declaringClass);
		}

		private boolean shouldInclude(Method method, Class<?> declaringClass) {
			if (MessageChannel.class.isAssignableFrom(declaringClass) ||
					AbstractEndpoint.class.isAssignableFrom(declaringClass)) {
				Class<?> managementInterface = this.getManagementInterface(declaringClass);
				if (managementInterface != null) {
					for (Method interfaceMethod : managementInterface.getMethods()) {
						if (interfaceMethod.getName().equals(method.getName()) &&
								Arrays.equals(interfaceMethod.getParameterTypes(), method.getParameterTypes())) {
							return true;
						}
					}
				}
			}
			return false;
		}

		private Class<?> getManagementInterface(Class<?> type) {
			if (AbstractEndpoint.class.isAssignableFrom(type)) {
				return Lifecycle.class;
			}
			if (QueueChannel.class.isAssignableFrom(type)) {
				return QueueChannelInfo.class;
			}
			if (PollableChannel.class.isAssignableFrom(type)) {
				return PollableChannelInfo.class;
			}
			if (MessageChannel.class.isAssignableFrom(type)) {
				return MessageChannelInfo.class;
			}
			return null;
		}
	}

}
