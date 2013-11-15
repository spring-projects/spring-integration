/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.component2;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.component.TestIntegrationConfiguration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.util.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration(
		classes = IntegrationConfigurationAnnotationConfigContextTests.IntegrationConfigurationContextConfiguration.class,
		loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationConfigurationAnnotationConfigContextTests {

	@Autowired
	@Qualifier("bar")
	private String bar;

	@Autowired
	private TestIntegrationConfiguration configuration;

	@Autowired
	private MessageChannel logger;

	@Autowired
	private TestConfiguration testConfiguration;

	@Autowired
	@Qualifier("testBean")
	private String testBean;

	@Autowired
	@Qualifier(IntegrationContextUtils.INTEGRATION_PROPERTIES_BEAN_NAME)
	private Properties integrationProperties;

	@Test
	public void testInt2500AnnotationConfigContext() {
		logger.send(new GenericMessage<Object>("test"));
		assertEquals(new Integer(1), configuration.getBeanCount());
		assertEquals("error", integrationProperties.getProperty("messagingTemplate.lateReply.logging.level"));
	}

	@Configuration
	@ImportResource("classpath:org/springframework/integration/component2/integrationConfigurationAnnotationConfigContextTests.xml")
	public static class IntegrationConfigurationContextConfiguration {

		@Bean
		public String bar() {
			return  "bar";
		}

	}

}
