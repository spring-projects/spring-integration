/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jms;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.DirectFieldAccessor;
/**
 * NOT A PUBLIC API
 *
 * Temporary class, will be removed as soon as
 * https://jira.springsource.org/browse/SPR-9648 is addressed
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
class TemporaryConsumerUtils {

	private static volatile Constructor<?> consumerCacheKeyConstructor;

	private static volatile Constructor<?> cachedMessageConsumerConstructor;

	private final static Log logger = LogFactory.getLog(TemporaryConsumerUtils.class);

	public static MessageConsumer createConsumer(Session session, Destination destination, String messageSelector)
			throws JMSException {
		if (Proxy.isProxyClass(session.getClass())){
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(session);
			DirectFieldAccessor ihDa = new DirectFieldAccessor(invocationHandler);
			@SuppressWarnings("unchecked")
			Map<Object, MessageConsumer> cachedConsumers =
				(Map<Object, MessageConsumer>) ihDa.getPropertyValue("cachedConsumers");
			return getCachedConsumer(cachedConsumers, session, destination, messageSelector, false);
		}
		else {
			return session.createConsumer(destination);
		}
	}

	private static MessageConsumer getCachedConsumer(Map<Object, MessageConsumer> cachedConsumers, Session targetSession,
			Destination destination, String selector, boolean noLocal) throws JMSException {

		Object cacheKey = createConsumerCacheKey(destination, selector, noLocal, null);
		MessageConsumer consumer = cachedConsumers.get(cacheKey);
		if (consumer != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found cached JMS MessageConsumer for destination [" + destination + "]: " + consumer);
			}
		}
		else {
			consumer = targetSession.createConsumer(destination, selector);
			if (logger.isDebugEnabled()) {
				logger.debug("Creating cached JMS MessageConsumer for destination [" + destination + "]: " + consumer);
			}
			cachedConsumers.put(cacheKey, consumer);
		}
		return createCachedMessageConsumer(consumer);
	}

	private static MessageConsumer createCachedMessageConsumer(MessageConsumer messageConsumer){
		try {
			if (cachedMessageConsumerConstructor == null){
				Class<?> cachedMessageConsumerClass =
						Class.forName("org.springframework.jms.connection.CachedMessageConsumer",
								true, Thread.currentThread().getContextClassLoader());
				cachedMessageConsumerConstructor = cachedMessageConsumerClass.getConstructor(MessageConsumer.class);
				cachedMessageConsumerConstructor.setAccessible(true);
			}
			return (MessageConsumer) cachedMessageConsumerConstructor.newInstance(messageConsumer);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create org.springframework.jms.connection.CachedMessageConsumer", e);
		}
	}

	private static Object createConsumerCacheKey(Destination destination, String selector, boolean noLocal, String subscription){
		try {
			if (consumerCacheKeyConstructor == null){
				Class<?> consumerCacheKeyClass =
						Class.forName("org.springframework.jms.connection.CachingConnectionFactory$ConsumerCacheKey",
								true, Thread.currentThread().getContextClassLoader());
				consumerCacheKeyConstructor =
						consumerCacheKeyClass.getConstructor(Destination.class, String.class, boolean.class, String.class);
				consumerCacheKeyConstructor.setAccessible(true);
			}
			return consumerCacheKeyConstructor.newInstance(destination, selector, noLocal, subscription);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create org.springframework.jms.connection.CachedMessageConsumer", e);
		}
	}
}
