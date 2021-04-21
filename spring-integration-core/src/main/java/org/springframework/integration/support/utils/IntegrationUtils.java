/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.support.utils;

import java.io.UnsupportedEncodingException;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * General utility methods.
 *
 * @author Gary Russell
 * @author Marius Bogoevici
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public final class IntegrationUtils {

	private static final Log LOGGER = LogFactory.getLog(IntegrationUtils.class);

	private static final String INTERNAL_COMPONENT_PREFIX = '_' + IntegrationContextUtils.BASE_PACKAGE;

	public static final String INTEGRATION_CONVERSION_SERVICE_BEAN_NAME = "integrationConversionService";

	public static final String INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME = "messageBuilderFactory";

	/**
	 * Should be set to TRUE on CI plans and framework developer systems.
	 */
	public static final boolean FATAL_WHEN_NO_BEANFACTORY =
			Boolean.parseBoolean(System.getenv("SI_FATAL_WHEN_NO_BEANFACTORY"));

	private IntegrationUtils() {
	}

	/**
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return The {@link ConversionService} bean whose name is "integrationConversionService" if available.
	 */
	public static ConversionService getConversionService(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, INTEGRATION_CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
	}

	/**
	 * Returns the context-wide `messageBuilderFactory` bean from the beanFactory,
	 * or a {@link DefaultMessageBuilderFactory} if not found or the beanFactory is null.
	 * @param beanFactory The bean factory.
	 * @return The message builder factory.
	 */
	public static MessageBuilderFactory getMessageBuilderFactory(@Nullable BeanFactory beanFactory) {
		MessageBuilderFactory messageBuilderFactory = null;
		if (beanFactory != null) {
			try {
				messageBuilderFactory = beanFactory.getBean(
						INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME, MessageBuilderFactory.class);
			}
			catch (Exception e) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("No MessageBuilderFactory with name '"
							+ INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME
							+ "' found: " + e.getMessage()
							+ ", using default.");
				}
			}
		}
		else {
			LOGGER.debug("No 'beanFactory' supplied; cannot find MessageBuilderFactory, using default.");
			if (FATAL_WHEN_NO_BEANFACTORY) {
				throw new IllegalStateException("All Message creators need a BeanFactory");
			}
		}
		if (messageBuilderFactory == null) {
			messageBuilderFactory = new DefaultMessageBuilderFactory();
		}
		return messageBuilderFactory;
	}

	private static <T> T getBeanOfType(BeanFactory beanFactory, String beanName, Class<T> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		if (!beanFactory.containsBean(beanName)) {
			return null;
		}
		return beanFactory.getBean(beanName, type);
	}

	/**
	 * Utility method for null-safe conversion from String to byte[]
	 * @param value the String to be converted
	 * @param encoding the encoding
	 * @return the byte[] corresponding to the given String and encoding, null if provided String argument was null
	 * @throws IllegalArgumentException if the encoding is not supported
	 */
	public static byte[] stringToBytes(String value, String encoding) {
		try {
			return value != null ? value.getBytes(encoding) : null;
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Utility method for null-safe conversion from byte[] to String
	 * @param bytes the byte[] to be converted
	 * @param encoding the encoding
	 * @return the String corresponding to the given byte[] and encoding, null if provided byte[] argument was null
	 * @throws IllegalArgumentException if the encoding is not supported
	 */
	public static String bytesToString(byte[] bytes, String encoding) {
		try {
			return bytes == null ? null : new String(bytes, encoding);
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * If the exception is not a {@link MessagingException} or does not have
	 * a {@link MessagingException#getFailedMessage() failedMessage}, wrap it
	 * in a new {@link MessageDeliveryException} with the message.
	 * @param message the message.
	 * @param text a Supplier for the new exception's message text.
	 * @param ex the exception.
	 * @return the wrapper, if necessary, or the original exception.
	 * @since 5.0.4
	 */
	public static RuntimeException wrapInDeliveryExceptionIfNecessary(Message<?> message, Supplier<String> text,
			Throwable ex) {

		RuntimeException runtimeException = (ex instanceof RuntimeException)
				? (RuntimeException) ex
				: new MessageDeliveryException(message, text.get(), ex);
		if (!(ex instanceof MessagingException) ||
				((MessagingException) ex).getFailedMessage() == null) {
			runtimeException = new MessageDeliveryException(message, text.get(), ex);
		}
		return runtimeException;
	}

	/**
	 * If the exception is not a {@link MessagingException} or does not have
	 * a {@link MessagingException#getFailedMessage() failedMessage}, wrap it
	 * in a new {@link MessageHandlingException} with the message.
	 * @param message the message.
	 * @param text a Supplier for the new exception's message text.
	 * @param ex the exception.
	 * @return the wrapper, if necessary, or the original exception.
	 * @since 5.0.4
	 */
	public static RuntimeException wrapInHandlingExceptionIfNecessary(Message<?> message, Supplier<String> text,
			Throwable ex) {

		RuntimeException runtimeException = (ex instanceof RuntimeException)
				? (RuntimeException) ex
				: new MessageHandlingException(message, text.get(), ex);

		if (!(ex instanceof MessagingException) ||
				((MessagingException) ex).getFailedMessage() == null) {
			runtimeException = new MessageHandlingException(message, text.get(),
					(ex instanceof IllegalStateException && ex.getCause() != null) ? ex.getCause() : ex);
		}
		return runtimeException;
	}

	/**
	 * Obtain a component name from the provided {@link NamedComponent}.
	 * @param component the {@link NamedComponent} source for component name.
	 * @return the component name
	 * @since 5.3
	 */
	public static String obtainComponentName(NamedComponent component) {
		String name = component.getComponentName();
		if (name.charAt(0) == '_' && name.startsWith(INTERNAL_COMPONENT_PREFIX)) {
			name = name.substring((INTERNAL_COMPONENT_PREFIX).length() + 1);
		}
		return name;
	}

}
