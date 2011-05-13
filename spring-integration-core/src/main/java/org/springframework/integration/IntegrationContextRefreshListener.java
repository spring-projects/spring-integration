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
package org.springframework.integration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.MessageHeaders.MessageIdGenerationStrategy;


/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class IntegrationContextRefreshListener implements ApplicationListener<ContextRefreshedEvent>, DisposableBean{

	private final Log logger = LogFactory.getLog(getClass());
	
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			MessageIdGenerationStrategy idGenerationStrategy = 
				event.getApplicationContext().getBean(MessageIdGenerationStrategy.class);
			if (logger.isDebugEnabled()) {
				logger.debug("Using MessageHeaders.MessageIdGenerationStrategy [" + idGenerationStrategy + "]");
			}
			MessageHeaders.setMessageIdGenerationStrategy(idGenerationStrategy);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// We need to use the default.
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate MessageHeaders.MessageIdGenerationStrategy. Will use default UUID.randomUUID()");
			}
		}
	}

	public void destroy() throws Exception {
		MessageHeaders.reset();
	}

}
