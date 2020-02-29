/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;


/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class ChainElementsFailureTests {

	private Locale localeBeforeTest;

	@Before
	public void setUp() {
		localeBeforeTest = Locale.getDefault();
		Locale.setDefault(new Locale("en", "US"));
	}

	@After
	public void tearDown() {
		Locale.setDefault(localeBeforeTest);
	}

	@Test
	public void chainServiceActivator() throws Exception {

		try {
			this.bootStrap("service-activator");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:service-activator'.");
		}

	}

	@Test
	public void chainAggregator() throws Exception {

		try {
			this.bootStrap("aggregator");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:aggregator'.");
		}

	}

	@Test
	public void chainChain() throws Exception {

		try {
			this.bootStrap("chain");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:chain'.");
		}
	}

	@Test
	public void chainDelayer() throws Exception {

		try {
			this.bootStrap("delayer");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:delayer'.");
		}
	}

	@Test
	public void chainFilter() throws Exception {

		try {
			this.bootStrap("filter");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:filter'.");
		}
	}

	@Test
	public void chainGateway() throws Exception {

		try {
			this.bootStrap("gateway");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:gateway'.");
		}
	}

	@Test
	public void chainHeaderEnricher() throws Exception {

		try {
			this.bootStrap("header-enricher");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:header-enricher'.");
		}
	}

	@Test
	public void chainHeaderFilter() throws Exception {

		try {
			this.bootStrap("header-filter");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:header-filter'.");
		}
	}

	@Test
	public void chainHeaderValueRouter() throws Exception {

		try {
			this.bootStrap("header-value-router");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is " +
					"not" +
					" allowed to appear in element 'int:header-value-router'.");
		}
	}

	@Test
	public void chainTransformer() throws Exception {

		try {
			this.bootStrap("transformer");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:transformer'.");
		}
	}

	@Test
	public void chainRouter() throws Exception {

		try {
			this.bootStrap("router");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:router'.");
		}
	}

	@Test
	public void chainSplitter() throws Exception {

		try {
			this.bootStrap("splitter");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:splitter'.");
		}
	}

	@Test
	public void chainResequencer() throws Exception {

		try {
			this.bootStrap("resequencer");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage()).isEqualTo("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:resequencer'.");
		}
	}

	@Test
	public void chainResequencerPoller() throws Exception {

		try {
			this.bootStrap("resequencer-poller");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			final String expectedMessage = "Configuration problem: " +
					"'int:resequencer' must not define a 'poller' sub-element " +
					"when used within a chain.";
			final String actualMessage = e.getMessage();
			assertThat(actualMessage.startsWith(expectedMessage))
					.as("Error message did not start with '" + expectedMessage +
							"' but instead returned: '" + actualMessage + "'").isTrue();
		}
	}

	@Test
	public void testInt2755DetectDuplicateHandlerId() throws Exception {

		try {
			this.bootStrap("duplicate-handler-id");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage().contains("A bean definition is already registered for " +
					"beanName: 'foo$child.bar.handler' within the current <chain>.")).isTrue();
		}
	}

	private ApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("org/springframework/integration/config/xml/chain-elements-config.properties"));
		pfb.afterPropertiesSet();
		Properties prop = pfb.getObject();
		StringBuilder buffer = new StringBuilder();
		buffer.append(prop.getProperty("xmlheaders")).append(prop.getProperty(configProperty)).append(prop.getProperty("xmlfooter"));
		ByteArrayInputStream stream = new ByteArrayInputStream(buffer.toString().getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

	public static class SampleService {
		public String echo(String value) {
			return value;
		}
	}
}
