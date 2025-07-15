/*
 * Copyright 2023-present the original author or authors.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * An {@link org.springframework.beans.factory.config.BeanDefinition#ROLE_INFRASTRUCTURE}
 * component to report Spring Integration relevant configuration state after application context is refreshed.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
class IntegrationConfigurationReport
		implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

	private static final LogAccessor LOGGER = new LogAccessor(IntegrationConfigurationReport.class);

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().equals(this.applicationContext)) {
			report();
		}
	}

	private void report() {
		printIntegrationProperties();
	}

	private void printIntegrationProperties() {
		if (LOGGER.isDebugEnabled()) {
			Properties integrationProperties =
					IntegrationContextUtils.getIntegrationProperties(this.applicationContext)
							.toProperties();

			StringWriter writer = new StringWriter();
			integrationProperties.list(new PrintWriter(writer));
			StringBuffer propertiesBuffer = writer.getBuffer()
					.delete(0, "-- listing properties --".length());
			LOGGER.debug("\nSpring Integration global properties:\n" + propertiesBuffer);
		}
	}

}
