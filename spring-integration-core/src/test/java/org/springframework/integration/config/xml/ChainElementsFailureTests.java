/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class ChainElementsFailureTests {

	private Locale localeBeforeTest;

	@BeforeEach
	public void setUp() {
		localeBeforeTest = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("en-US"));
	}

	@AfterEach
	public void tearDown() {
		Locale.setDefault(localeBeforeTest);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"service-activator",
			"aggregator",
			"chain",
			"delayer",
			"filter",
			"gateway",
			"header-enricher",
			"header-filter",
			"header-filter",
			"header-value-router",
			"transformer",
			"router",
			"splitter",
			"resequencer"
	})
	void inputChannelNotAllowed(String element) {
		assertThatExceptionOfType(XmlBeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(element))
				.withStackTraceContaining(
						"cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
								" allowed to appear in element 'int:" + element + "'.");
	}

	@Test
	public void chainResequencerPoller() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("resequencer-poller"))
				.withMessageStartingWith("Configuration problem: " +
						"'int:resequencer' must not define a 'poller' sub-element " +
						"when used within a chain.");
	}

	@Test
	public void testInt2755DetectDuplicateHandlerId() throws Exception {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("duplicate-handler-id"))
				.withMessageContaining("A bean definition is already registered for " +
						"beanName: 'foo$child.bar.handler' within the current <chain>.");
	}

	private static void bootStrap(String configProperty) throws Exception {
		Properties prop =
				PropertiesLoaderUtils.loadProperties(
						new ClassPathResource(
								"org/springframework/integration/config/xml/chain-elements-config.properties"));
		ByteArrayInputStream stream =
				new ByteArrayInputStream((prop.getProperty("xmlheaders") +
						prop.getProperty(configProperty) +
						prop.getProperty("xmlfooter")).getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
	}

	public static class SampleService {

		public String echo(String value) {
			return value;
		}

	}

}
