/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.xml.config;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.integration.xml.transformer.XPathTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class ChainElementsTests {

	@Test
	public void chainXPathTransformer() throws Exception {
		try {
			bootStrap("xpath-transformer");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			final String expectedMessage = "Configuration problem: " +
					"The 'input-channel' attribute isn't allowed for a nested " +
					"(e.g. inside a <chain/>) endpoint element: 'int-xml:xpath-transformer'.";
			final String actualMessage = e.getMessage();
			assertThat(actualMessage.startsWith(expectedMessage))
					.as("Error message did not start with '" + expectedMessage +
							"' but instead returned: '" + actualMessage + "'").isTrue();
		}

	}

	@Test
	public void chainXPathTransformerId() throws Exception {
		try (ConfigurableApplicationContext ctx = bootStrap("xpath-transformer-id")) {
			assertThat(ctx.getBean(XPathTransformer.class)).isNotNull();
		}
	}

	@Test
	public void chainXPathRouterOrder() throws Exception {
		try {
			bootStrap("xpath-router-order");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			final String expectedMessage = "Configuration problem: " +
					"'int-xml:xpath-router' must not define an 'order' attribute " +
					"when used within a chain.";
			final String actualMessage = e.getMessage();
			assertThat(actualMessage.startsWith(expectedMessage))
					.as("Error message did not start with '" + expectedMessage +
							"' but instead returned: '" + actualMessage + "'").isTrue();
		}

	}

	@Test
	public void chainXPathTransformerPoller() throws Exception {
		try {
			bootStrap("xpath-transformer-poller");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			final String expectedMessage = "Configuration problem: " +
					"'int-xml:xpath-transformer' must not define a 'poller' " +
					"sub-element when used within a chain.";
			final String actualMessage = e.getMessage();
			assertThat(actualMessage).as("Error message did not start with '" + expectedMessage +
					"' but instead returned: '" + actualMessage + "'").startsWith(expectedMessage);
		}
	}

	@Test
	public void chainXPathTransformerSuccess() throws Exception {
		try (ConfigurableApplicationContext ctx = bootStrap("xpath-transformer-success")) {
			assertThat(ctx.getBean(XPathTransformer.class)).isNotNull();
		}
	}

	private ConfigurableApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource(
				"org/springframework/integration/xml/config/chain-elements-config.properties"));
		pfb.afterPropertiesSet();
		Properties prop = pfb.getObject();
		StringBuffer buffer = new StringBuffer();
		buffer.append(prop.getProperty("xmlheaders"))
				.append(prop.getProperty(configProperty))
				.append(prop.getProperty("xmlfooter"));
		ByteArrayInputStream stream = new ByteArrayInputStream(buffer.toString().getBytes());

		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

}
