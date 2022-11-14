/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
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
@IntegrationDsl
public abstract class IntegrationComponentSpec<S extends IntegrationComponentSpec<S, T>, T>
		extends AbstractFactoryBean<T>
		implements SmartLifecycle {

	protected static final SpelExpressionParser PARSER = new SpelExpressionParser();

	protected volatile T target; // NOSONAR

	private String id;

	/**
	 * Configure the component identifier. Used as the {@code beanName} to register the
	 * bean in the application context for this component.
	 * @param idToSet the id.
	 * @return the spec.
	 */
	protected S id(String idToSet) {
		this.id = idToSet;
		return _this();
	}

	public final String getId() {
		return this.id;
	}

	/**
	 * @return the configured component.
	 */
	public T get() {
		if (this.target == null) {
			this.target = doGet();
		}
		return this.target;
	}

	@Override
	public Class<?> getObjectType() {
		return get().getClass();
	}

	@Override
	protected T createInstance() {
		T instance = get();
		if (instance instanceof InitializingBean) {
			try {
				((InitializingBean) instance).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot initialize bean: " + instance, e);
			}
		}
		return instance;
	}

	@Override
	protected void destroyInstance(T instance) {
		if (instance instanceof DisposableBean) {
			try {
				((DisposableBean) instance).destroy();
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot destroy bean: " + instance, e);
			}
		}
	}

	@Override
	public void start() {
		T instance = get();
		if (instance instanceof Lifecycle) {
			((Lifecycle) instance).start();
		}
	}

	@Override
	public void stop() {
		T instance = get();
		if (instance instanceof Lifecycle) {
			((Lifecycle) instance).stop();
		}
	}

	@Override
	public boolean isRunning() {
		T instance = get();
		return !(instance instanceof Lifecycle) || ((Lifecycle) instance).isRunning();
	}

	@Override
	public boolean isAutoStartup() {
		T instance = get();
		return instance instanceof SmartLifecycle && ((SmartLifecycle) instance).isAutoStartup();
	}

	@Override
	public void stop(Runnable callback) {
		T instance = get();
		if (instance instanceof SmartLifecycle) {
			((SmartLifecycle) instance).stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public int getPhase() {
		T instance = get();
		if (instance instanceof SmartLifecycle) {
			return ((SmartLifecycle) instance).getPhase();
		}
		else {
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	protected final S _this() { // NOSONAR
		return (S) this;
	}

	protected T doGet() {
		throw new UnsupportedOperationException();
	}

}
