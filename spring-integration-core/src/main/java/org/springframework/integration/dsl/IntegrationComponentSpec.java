/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * The common Builder abstraction. The {@link #get()} method returns the final component.
 *
 * @param <S> the target {@link IntegrationComponentSpec} implementation type.
 * @param <T> the target type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class IntegrationComponentSpec<S extends IntegrationComponentSpec<S, T>, T>
		implements FactoryBean<T> {

	protected final static SpelExpressionParser PARSER = new SpelExpressionParser();

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR

	protected volatile T target; // NOSONAR

	private String id;

	/**
	 * Configure the component identifier. Used as the {@code beanName} to register the
	 * bean in the application context for this component.
	 * @param id the id.
	 * @return the spec.
	 */
	protected S id(String id) {
		this.id = id;
		return _this();
	}

	public final String getId() {
		return this.id;
	}

	/**
	 * @return the configured component.
	 */
	public final T get() {
		if (this.target == null) {
			this.target = doGet();
		}
		return this.target;
	}

	@Override
	public T getObject() throws Exception {
		return get();
	}

	@Override
	public Class<?> getObjectType() {
		return get().getClass();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@SuppressWarnings("unchecked")
	protected final S _this() {
		return (S) this;
	}

	protected T doGet() {
		throw new UnsupportedOperationException();
	}

}
