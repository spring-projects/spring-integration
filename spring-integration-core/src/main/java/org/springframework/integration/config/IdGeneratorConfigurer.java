/*
 * Copyright 2002-2011 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessageHeaders.IdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;


/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class IdGeneratorConfigurer implements ApplicationListener<ContextRefreshedEvent>, DisposableBean{

	private final Log logger = LogFactory.getLog(getClass());
	
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.setIdGenerationStrategy(event);
	}

	public void destroy() throws Exception {
		this.resetIdGenerationStrategy();
	}
	
	private void resetIdGenerationStrategy(){
		try{
			Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
			ReflectionUtils.makeAccessible(idGeneratorField);
			idGeneratorField.set(null, null);
		} 
		catch (Exception ex){
			if (logger.isWarnEnabled()) {
				logger.warn("Unexpected exception happened while accessing idGenerator of MessageHeaders.", ex);
			}
		}
	}
	
	private void setIdGenerationStrategy(ContextRefreshedEvent event){
		try {
			IdGenerator idGenerationStrategy = 
				event.getApplicationContext().getBean(IdGenerator.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MessageHeaders.idGenerator [" + idGenerationStrategy + "]");
			}
			Field idGeneratorField = ReflectionUtils.findField(MessageHeaders.class, "idGenerator");
			ReflectionUtils.makeAccessible(idGeneratorField);
			IdGenerator idGenerator = (IdGenerator) idGeneratorField.get(null);
			Assert.state(idGenerator == null, "'MessageHeaders.idGenerator' " +
					"has already been set and can not be set again");
			if (logger.isInfoEnabled()){
				logger.info("Message IDs will be generated using custom ID generation strategy: " + idGenerationStrategy);
			}
			ReflectionUtils.setField(idGeneratorField, null, idGenerationStrategy);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageHeaders.idGenerator. Will use default UUID.randomUUID()");
			}
		}
		catch (Exception e){
			if (logger.isWarnEnabled()) {
				logger.warn("Unexpected exception happened while accessing idGenerator of MessageHeaders." +
						" Will use default UUID.randomUUID()", e);
			}
		}
	}

}
