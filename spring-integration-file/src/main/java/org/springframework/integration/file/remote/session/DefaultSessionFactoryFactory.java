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
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default implementation of {@link SessionFactoryFactory} using a simple map lookup.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class DefaultSessionFactoryFactory<F> implements SessionFactoryFactory<F> {

	private final Map<Object, SessionFactory<F>> factories = new ConcurrentHashMap<Object, SessionFactory<F>>();

	private final SessionFactory<F> defaultFactory;

	public DefaultSessionFactoryFactory(Map<Object, SessionFactory<F>> factories) {
		this(factories, null);
	}

	public DefaultSessionFactoryFactory(Map<Object, SessionFactory<F>> factories, Object defaultKey) {
		this.factories.putAll(factories);
		if (defaultKey != null) {
			this.defaultFactory = factories.get(defaultKey);
		}
		else {
			this.defaultFactory = null;
		}
	}


	@Override
	public SessionFactory<F> getSessionFactory(Object key) {
		if (key == null) {
			return this.defaultFactory;
		}
		SessionFactory<F> factory = this.factories.get(key);
		return factory != null ? factory : this.defaultFactory;
	}

}
