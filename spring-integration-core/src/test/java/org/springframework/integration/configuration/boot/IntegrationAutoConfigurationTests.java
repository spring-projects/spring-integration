/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.configuration.boot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * @author Artem Bilan
 * @since 4.0
 */
public class IntegrationAutoConfigurationTests {

	@Test
	public void testIntegrationAutoConfiguration() {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(Configuration.class);
		assertTrue(applicationContext.containsBean(IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME));
		assertTrue(applicationContext.containsBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME));
		assertTrue(applicationContext.containsBean(IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME));
		assertTrue(applicationContext.containsBean("jsonPath"));
		assertFalse(applicationContext.containsBean("xpath"));
	}

	@EnableAutoConfiguration
	public static class Configuration {
	}

}
