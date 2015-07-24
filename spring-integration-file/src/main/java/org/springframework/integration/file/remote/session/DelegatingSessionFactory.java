/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.file.remote.session;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link SessionFactory} that delegates to a {@link SessionFactory} retrieved from a
 * {@link SessionFactoryFactory}.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class DelegatingSessionFactory<F> implements SessionFactory<F> {

	private final SessionFactoryFactory<F> factoryFactory;

	private final ThreadLocal<Object> threadKey = new ThreadLocal<Object>();

	/**
	 * Construct an instance with a {@link DefaultSessionFactoryFactory} using the
	 * supplied factories and default key.
	 * @param factories the factories.
	 * @param defaultKey the default key.
	 */
	public DelegatingSessionFactory(Map<Object, SessionFactory<F>> factories, Object defaultKey) {
		this(new DefaultSessionFactoryFactory<F>(factories, defaultKey));
	}

	/**
	 * Construct an instance using the supplied factory.
	 * @param factoryFactory the factory.
	 */
	public DelegatingSessionFactory(SessionFactoryFactory<F> factoryFactory) {
		Assert.notNull(factoryFactory, "'factoryFactory' cannot be null");
		this.factoryFactory = factoryFactory;
	}

	/**
	 * Set a key to be used for {@link #getSession()} on this thread.
	 * @param key the key.
	 */
	public void setThreadKey(Object key) {
		this.threadKey.set(key);
	}

	/**
	 * Clear the key for this thread.
	 */
	public void clearThreadKey() {
		this.threadKey.remove();
	}

	/**
	 * Messaging-friendly version of {@link #setThreadKey(Object)} that can be invoked from
	 * a service activator.
	 * @param message the message.
	 * @param key the key.
	 * @return the message (unchanged).
	 */
	public Message<?> setThreadKey(Message<?> message, Object key) {
		this.threadKey.set(key);
		return message;
	}

	/**
	 * Messaging-friendly version of {@link #clearThreadKey()} that can be invoked from
	 * a service activator.
	 * @param message the message.
	 * @return the message (unchanged).
	 */
	public Message<?> clearThreadKey(Message<?> message) {
		this.threadKey.remove();
		return message;
	}

	@Override
	public Session<F> getSession() {
		return getSession(this.threadKey.get());
	}

	public Session<F> getSession(Object key) {
		SessionFactory<F> sessionFactory = this.factoryFactory.getSessionFactory(key);
		Assert.notNull(sessionFactory, "No default SessionFactory configured");
		return sessionFactory.getSession();
	}

}
