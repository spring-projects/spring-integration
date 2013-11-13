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

package org.springframework.integration.component;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationConfigurationWithScannerLastTests {

	@Autowired
	private TestIntegrationConfiguration configuration;

	@Autowired
	private MessageChannel logger;

	@Autowired
	@Qualifier(IntegrationContextUtils.INTEGRATION_PROPERTIES_BEAN_NAME)
	private Properties integrationProperties;

	@Test
	public void testInt2500ConfigurationCreatedOnlyOnceNewAnnotations() {
		logger.send(new GenericMessage<Object>("test"));
		assertEquals(new Integer(1), configuration.getBeanCount());
		assertEquals("error", integrationProperties.getProperty("messagingTemplate.lateReply.logging.level"));
	}

	@Test
	public void testInt2500ConfigurationCreatedOnlyOnceNewCtx() {
		AbstractRefreshableApplicationContext context = new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-context.xml", this.getClass());
		context.setAllowBeanDefinitionOverriding(false);
		context.refresh();

		TestIntegrationConfiguration configuration = context.getBean(TestIntegrationConfiguration.class);
		MessageChannel logger = context.getBean("logger", MessageChannel.class);
		Properties integrationProperties = context.getBean(IntegrationContextUtils.INTEGRATION_PROPERTIES_BEAN_NAME, Properties.class);

		logger.send(new GenericMessage<Object>("test"));
		assertEquals(new Integer(1), configuration.getBeanCount());
		assertEquals("error", integrationProperties.getProperty("messagingTemplate.lateReply.logging.level"));
	}

}
