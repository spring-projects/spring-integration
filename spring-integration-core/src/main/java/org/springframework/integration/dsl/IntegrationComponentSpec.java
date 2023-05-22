/*
 * Copyright 2016-2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * The common Builder abstraction.
 * If used as a bean definition, must be treated as an {@link FactoryBean},
 * therefore its {@link #getObject()} method must not be called in the target configuration.
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
		implements FactoryBean<T>, InitializingBean, DisposableBean, SmartLifecycle {

	protected static final SpelExpressionParser PARSER = new SpelExpressionParser();

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR - final

	protected volatile T target; // NOSONAR

	private String id;

	/**
	 * Configure the component identifier. Used as the {@code beanName} to register the
	 * bean in the application context for this component.
	 * @param idToSet the id.
	 * @return the spec.
	 */
	protected S id(@Nullable String idToSet) {
		this.id = idToSet;
		return _this();
	}

	@Nullable
	public final String getId() {
		return this.id;
	}

	@Override
	public Class<?> getObjectType() {
		return getObject().getClass();
	}

	/**
	 * !!! This method must not be called from the target configuration !!!
	 * @return the object backed by this factory bean.
	 */
	@NonNull
	@Override
	public T getObject() {
		if (this.target == null) {
			this.target = doGet();
		}
		return this.target;
	}

	@Override
	public void afterPropertiesSet() {
		try {
			if (this.target instanceof InitializingBean initializingBean) {
				initializingBean.afterPropertiesSet();
			}
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Cannot initialize bean: " + this.target, ex);
		}
	}

	@Override
	public void destroy() {
		if (this.target instanceof DisposableBean disposableBean) {
			try {
				disposableBean.destroy();
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot destroy bean: " + this.target, e);
			}
		}
	}

	@Override
	public void start() {
		if (this.target instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	public void stop() {
		if (this.target instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.target instanceof Lifecycle lifecycle) || lifecycle.isRunning();
	}

	@Override
	public boolean isAutoStartup() {
		return this.target instanceof SmartLifecycle lifecycle && lifecycle.isAutoStartup();
	}

	@Override
	public void stop(Runnable callback) {
		if (this.target instanceof SmartLifecycle lifecycle) {
			lifecycle.stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public int getPhase() {
		if (this.target instanceof SmartLifecycle lifecycle) {
			return lifecycle.getPhase();
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
