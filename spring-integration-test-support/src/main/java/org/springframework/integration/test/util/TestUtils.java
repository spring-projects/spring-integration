/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.test.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class TestUtils {

	/**
	 * Obtain a value for the property from the provide object.
	 * Supports nested properties via period delimiter.
	 * @param root the object to obtain the property value
	 * @param propertyPath the property name to obtain a value.
	 * Can be nested path defined by the period.
	 * @return the value of the property or null
	 * @see DirectFieldAccessor
	 */
	public static Object getPropertyValue(Object root, String propertyPath) {
		Object value = null;
		DirectFieldAccessor accessor = new DirectFieldAccessor(root);
		String[] tokens = propertyPath.split("\\.");
		for (int i = 0; i < tokens.length; i++) {
			value = accessor.getPropertyValue(tokens[i]);
			if (value != null) {
				accessor = new DirectFieldAccessor(value);
			}
			else if (i == tokens.length - 1) {
				return null;
			}
			else {
				throw new IllegalArgumentException(
						"intermediate property '" + tokens[i] + "' is null");
			}
		}
		return value;
	}

	/**
	 * Obtain a value for the property from the provide object
	 * and try to cast it to the provided type.
	 * Supports nested properties via period delimiter.
	 * @param root the object to obtain the property value
	 * @param propertyPath the property name to obtain a value.
	 * @param type the expected value type.
	 * @param <T> the expected value type.
	 * Can be nested path defined by the period.
	 * @return the value of the property or null
	 * @see DirectFieldAccessor
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getPropertyValue(Object root, String propertyPath, Class<T> type) {
		Object value = getPropertyValue(root, propertyPath);
		if (value != null) {
			Assert.isAssignable(type, value.getClass());
		}
		return (T) value;
	}

	/**
	 * Create a {@link TestApplicationContext} instance
	 * supplied with the basic Spring Integration infrastructure.
	 * @return the {@link TestApplicationContext} instance
	 */
	public static TestApplicationContext createTestApplicationContext() {
		TestApplicationContext context = new TestApplicationContext();
		ErrorHandler errorHandler = new MessagePublishingErrorHandler(context);
		ThreadPoolTaskScheduler scheduler = createTaskScheduler(10);
		scheduler.setErrorHandler(errorHandler);
		registerBean("taskScheduler", scheduler, context);
		registerBean("integrationConversionService", new DefaultFormattingConversionService(), context);
		return context;
	}

	/**
	 * A factory for the {@link ThreadPoolTaskScheduler} instances based on the provided {@code poolSize}.
	 * @param poolSize the size for the {@link ThreadPoolTaskScheduler}
	 * @return the {@link ThreadPoolTaskScheduler} instance.
	 */
	public static ThreadPoolTaskScheduler createTaskScheduler(int poolSize) {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(poolSize);
		scheduler.setRejectedExecutionHandler(new CallerRunsPolicy());
		scheduler.afterPropertiesSet();
		return scheduler;
	}

	private static void registerBean(String beanName, Object bean, BeanFactory beanFactory) {
		Assert.notNull(beanName, "bean name must not be null");
		ConfigurableListableBeanFactory configurableListableBeanFactory = null;
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
		else if (beanFactory instanceof GenericApplicationContext) {
			configurableListableBeanFactory = ((GenericApplicationContext) beanFactory).getBeanFactory();
		}
		if (bean instanceof BeanNameAware) {
			((BeanNameAware) bean).setBeanName(beanName);
		}
		if (bean instanceof BeanFactoryAware) {
			((BeanFactoryAware) bean).setBeanFactory(beanFactory);
		}
		if (bean instanceof InitializingBean) {
			try {
				((InitializingBean) bean).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new FatalBeanException("failed to register bean with test context", e);
			}
		}
		configurableListableBeanFactory.registerSingleton(beanName, bean); //NOSONAR false positive
	}


	/**
	 * A {@link GenericApplicationContext} extension with some support methods
	 * to register Spring Integration beans in the application context at runtime.
	 */
	public static class TestApplicationContext extends GenericApplicationContext {

		TestApplicationContext() {
			super();
		}

		public void registerChannel(String channelName, final MessageChannel channel) {
			String componentName = getComponentNameIfNamed(channel);
			if (componentName != null) {
				if (channelName == null) {
					channelName = componentName;
				}
				else {
					Assert.isTrue(componentName.equals(channelName),
							"channel name has already been set with a conflicting value");
				}
			}
			TestUtils.registerBean(channelName, channel, this);
		}

		public void registerEndpoint(String endpointName, Object endpoint) {
			TestUtils.registerBean(endpointName, endpoint, this);
		}

		public void registerBean(String beanName, Object bean) {
			TestUtils.registerBean(beanName, bean, this);
		}

		private String getComponentNameIfNamed(final MessageChannel channel) {
			Set<Class<?>> interfaces = ClassUtils.getAllInterfacesAsSet(channel);
			final AtomicReference<String> componentName = new AtomicReference<String>();
			for (Class<?> intface : interfaces) {
				if ("org.springframework.integration.support.context.NamedComponent".equals(intface.getName())) {
					ReflectionUtils.doWithMethods(channel.getClass(), method -> {
						try {
							componentName.set((String) method.invoke(channel, new Object[0]));
						}
						catch (InvocationTargetException e) {
							throw new IllegalArgumentException(e);
						}
					}, method -> method.getName().equals("getComponentName"));
					break;
				}
			}
			return componentName.get();
		}

	}

	/**
	 * @param history a message history
	 * @param componentName the name of a component to scan for
	 * @param startingIndex the index to start scanning
	 * @return the properties provided by the named component or null if none available
	 */
	public static Properties locateComponentInHistory(List<Properties> history, String componentName,
			int startingIndex) {
		Assert.notNull(history, "'history' must not be null");
		Assert.isTrue(StringUtils.hasText(componentName), "'componentName' must be provided");
		Assert.isTrue(startingIndex < history.size(), "'startingIndex' can not be greater then size of history");
		Properties component = null;
		for (int i = startingIndex; i < history.size(); i++) {
			Properties properties = history.get(i);
			if (componentName.equals(properties.get("name"))) {
				component = properties;
				break;
			}
		}
		return component;
	}

	/**
	 * Update file path by replacing any '/' with the system's file separator.
	 * @param s The file path containing '/'.
	 * @return The updated file path (if necessary).
	 */
	public static String applySystemFileSeparator(String s) {
		return s.replaceAll("/", java.util.regex.Matcher.quoteReplacement(File.separator));
	}

	private static class MessagePublishingErrorHandler implements ErrorHandler {

		private final Log logger = LogFactory.getLog(this.getClass());

		private final TestApplicationContext context;

		MessagePublishingErrorHandler(TestApplicationContext ctx) {
			this.context = ctx;
		}

		@Override
		public void handleError(Throwable t) {
			MessageChannel errorChannel = this.resolveErrorChannel(t);
			boolean sent = false;
			if (errorChannel != null) {
				try {
					sent = errorChannel.send(new ErrorMessage(t), 10000);
				}
				catch (Throwable errorDeliveryError) { //NOSONAR
					// message will be logged only
					if (logger.isWarnEnabled()) {
						logger.warn("Error message was not delivered.", errorDeliveryError);
					}
					if (errorDeliveryError instanceof Error) {
						throw ((Error) errorDeliveryError);
					}
				}
			}
			if (!sent && logger.isErrorEnabled()) {
				Message<?> failedMessage = (t instanceof MessagingException) ?
						((MessagingException) t).getFailedMessage() : null;
				if (failedMessage != null) {
					logger.error("failure occurred in messaging task with message: " + failedMessage, t);
				}
				else {
					logger.error("failure occurred in messaging task", t);
				}
			}

		}

		private MessageChannel resolveErrorChannel(Throwable t) {
			if (t instanceof MessagingException) {
				Message<?> failedMessage = ((MessagingException) t).getFailedMessage();
				Object errorChannelHeader = failedMessage.getHeaders().getErrorChannel();
				if (errorChannelHeader instanceof MessageChannel) {
					return (MessageChannel) errorChannelHeader;
				}
				Assert.isInstanceOf(String.class, errorChannelHeader, "Unsupported error channel header type. " +
						"Expected MessageChannel or String, but actual type is [" +
						errorChannelHeader.getClass() + "]");
				return this.context.getBean((String) errorChannelHeader, MessageChannel.class);
			}
			else {
				return null;
			}
		}

	}

}
