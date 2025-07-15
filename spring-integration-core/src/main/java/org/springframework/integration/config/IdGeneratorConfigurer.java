/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;
import org.springframework.util.ReflectionUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 2.0.4
 */
public final class IdGeneratorConfigurer implements ApplicationListener<ApplicationContextEvent> {

	private final Lock lock = new ReentrantLock();

	private static final Set<String> GENERATOR_CONTEXT_ID = new HashSet<>();

	private static @Nullable IdGenerator theIdGenerator;

	private final Log logger = LogFactory.getLog(getClass());

	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		this.lock.lock();
		try {

			ApplicationContext context = event.getApplicationContext();
			if (event instanceof ContextRefreshedEvent) {
				boolean contextHasIdGenerator = context.getBeanNamesForType(IdGenerator.class).length > 0;
				if (contextHasIdGenerator && setIdGenerator(context)) {
					IdGeneratorConfigurer.GENERATOR_CONTEXT_ID.add(Objects.requireNonNull(context.getId()));
				}
			}
			else if (event instanceof ContextClosedEvent
					&& IdGeneratorConfigurer.GENERATOR_CONTEXT_ID.contains(context.getId())) {

				if (IdGeneratorConfigurer.GENERATOR_CONTEXT_ID.size() == 1) {
					unsetIdGenerator();
				}
				IdGeneratorConfigurer.GENERATOR_CONTEXT_ID.remove(context.getId());
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	private boolean setIdGenerator(ApplicationContext context) {
		try {
			IdGenerator idGeneratorBean = context.getBean(IdGenerator.class);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("using custom MessageHeaders.IdGenerator [" + idGeneratorBean.getClass() + "]");
			}
			Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
			ReflectionUtils.makeAccessible(Objects.requireNonNull(idGeneratorField));
			IdGenerator currentIdGenerator = (IdGenerator) ReflectionUtils.getField(Objects.requireNonNull(idGeneratorField), null);
			if (currentIdGenerator != null) {
				if (currentIdGenerator.equals(idGeneratorBean)) {
					// same instance is already set, nothing needs to be done
					return false;
				}
				else {
					if ((IdGeneratorConfigurer.theIdGenerator != null ?
							IdGeneratorConfigurer.theIdGenerator.getClass() : null) == idGeneratorBean.getClass()) {
						if (this.logger.isWarnEnabled()) {
							this.logger.warn("Another instance of " + idGeneratorBean.getClass() +
									" has already been established; ignoring");
						}
						return true;
					}
					else {
						// different instance has been set, not legal
						throw new BeanDefinitionStoreException("'MessageHeaders.idGenerator' has already been set and " +
								"can not be set again");
					}
				}
			}
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Message IDs will be generated using custom IdGenerator [" + idGeneratorBean
						.getClass() + "]");
			}
			ReflectionUtils.setField(idGeneratorField, null, idGeneratorBean);
			IdGeneratorConfigurer.theIdGenerator = idGeneratorBean;
		}
		catch (@SuppressWarnings("unused") NoSuchBeanDefinitionException e) {
			// No custom IdGenerator. We will use the default.
			noSuchBean(context);
			return false;
		}
		catch (IllegalStateException e) {
			// thrown from ReflectionUtils
			illegalState(e);
			return false;
		}
		return true;
	}

	private void noSuchBean(ApplicationContext context) {
		int idBeans = context.getBeansOfType(IdGenerator.class).size();
		if (idBeans > 1 && this.logger.isWarnEnabled()) {
			this.logger.warn("Found too many 'IdGenerator' beans (" + idBeans + ") " +
					"Will use the existing UUID strategy.");
		}
		else if (this.logger.isDebugEnabled()) {
			this.logger.debug("Unable to locate MessageHeaders.IdGenerator. Will use the existing UUID strategy.");
		}
	}

	private void illegalState(IllegalStateException e) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn("Unexpected exception occurred while accessing idGenerator of MessageHeaders." +
					" Will use the existing UUID strategy.", e);
		}
	}

	private void unsetIdGenerator() {
		try {
			Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
			ReflectionUtils.makeAccessible(Objects.requireNonNull(idGeneratorField));
			idGeneratorField.set(null, null);
			IdGeneratorConfigurer.theIdGenerator = null;
		}
		catch (Exception e) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("Unexpected exception occurred while accessing idGenerator of MessageHeaders.", e);
			}
		}
	}

}
