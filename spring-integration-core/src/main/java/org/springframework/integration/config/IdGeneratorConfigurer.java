/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * @since 2.0.4
 */
public final class IdGeneratorConfigurer implements ApplicationListener<ApplicationContextEvent> {

	private static final Set<String> generatorContextId = new HashSet<String>();

	private static volatile IdGenerator theIdGenerator;

	private final Log logger = LogFactory.getLog(getClass());

	public synchronized void onApplicationEvent(ApplicationContextEvent event) {
		ApplicationContext context = event.getApplicationContext();
		if (event instanceof ContextRefreshedEvent) {
			boolean contextHasIdGenerator = context.getBeanNamesForType(IdGenerator.class).length > 0;
			if (contextHasIdGenerator) {
				if (this.setIdGenerator(context)) {
					IdGeneratorConfigurer.generatorContextId.add(context.getId());
				}
			}
		}
		else if (event instanceof ContextClosedEvent) {
			if (IdGeneratorConfigurer.generatorContextId.contains(context.getId())) {
				if (IdGeneratorConfigurer.generatorContextId.size() == 1) {
					this.unsetIdGenerator();
				}
				IdGeneratorConfigurer.generatorContextId.remove(context.getId());
			}
		}
	}

	private boolean setIdGenerator(ApplicationContext context) {
		try {
			IdGenerator idGeneratorBean = context.getBean(IdGenerator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("using custom MessageHeaders.IdGenerator [" + idGeneratorBean.getClass() + "]");
			}
			Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
			ReflectionUtils.makeAccessible(idGeneratorField);
			IdGenerator currentIdGenerator = (IdGenerator) ReflectionUtils.getField(idGeneratorField, null);
			if (currentIdGenerator != null) {
				if (currentIdGenerator.equals(idGeneratorBean)) {
					// same instance is already set, nothing needs to be done
					return false;
				}
				else {
					if (IdGeneratorConfigurer.theIdGenerator.getClass() == idGeneratorBean.getClass()) {
						if (logger.isWarnEnabled()) {
							logger.warn("Another instance of " + idGeneratorBean.getClass() +
									" has already been established; ignoring");
						}
						return true;
					}
					else {
						// different instance has been set, not legal
						throw new BeanDefinitionStoreException("'MessageHeaders.idGenerator' has already been set and can not be set again");
					}
				}
			}
			if (logger.isInfoEnabled()) {
				logger.info("Message IDs will be generated using custom IdGenerator [" + idGeneratorBean.getClass() + "]");
			}
			ReflectionUtils.setField(idGeneratorField, null, idGeneratorBean);
			IdGeneratorConfigurer.theIdGenerator = idGeneratorBean;
		}
		catch (NoSuchBeanDefinitionException e) {
			// No custom IdGenerator. We will use the default.
			int idBeans = context.getBeansOfType(IdGenerator.class).size();
			if (idBeans > 1 && logger.isWarnEnabled()) {
				logger.warn("Found too many 'IdGenerator' beans (" + idBeans + ") " +
						"Will use the existing UUID strategy.");
			}
			else if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageHeaders.IdGenerator. Will use the existing UUID strategy.");
			}
			return false;
		}
		catch (IllegalStateException e) {
			// thrown from ReflectionUtils
			if (logger.isWarnEnabled()) {
				logger.warn("Unexpected exception occurred while accessing idGenerator of MessageHeaders." +
						" Will use the existing UUID strategy.", e);
			}
			return false;
		}
		return true;
	}

	private void unsetIdGenerator() {
		try {
			Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
			ReflectionUtils.makeAccessible(idGeneratorField);
			idGeneratorField.set(null, null);
			IdGeneratorConfigurer.theIdGenerator = null;
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Unexpected exception occurred while accessing idGenerator of MessageHeaders.", e);
			}
		}
	}

}
