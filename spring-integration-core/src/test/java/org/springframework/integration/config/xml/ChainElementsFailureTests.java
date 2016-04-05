/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.Properties;

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

	@Test
	public void chainServiceActivator() throws Exception {

		try {
			this.bootStrap("service-activator");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:service-activator'.", e.getCause().getMessage());
		}

	}

	@Test
	public void chainAggregator() throws Exception {

		try {
			this.bootStrap("aggregator");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:aggregator'.", e.getCause().getMessage());
		}

	}

	@Test
	public void chainChain() throws Exception {

		try {
			this.bootStrap("chain");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:chain'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainDelayer() throws Exception {

		try {
			this.bootStrap("delayer");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:delayer'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainFilter() throws Exception {

		try {
			this.bootStrap("filter");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:filter'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainGateway() throws Exception {

		try {
			this.bootStrap("gateway");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:gateway'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainHeaderEnricher() throws Exception {

		try {
			this.bootStrap("header-enricher");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:header-enricher'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainHeaderFilter() throws Exception {

		try {
			this.bootStrap("header-filter");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:header-filter'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainHeaderValueRouter() throws Exception {

		try {
			this.bootStrap("header-value-router");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:header-value-router'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainTransformer() throws Exception {

		try {
			this.bootStrap("transformer");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:transformer'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainRouter() throws Exception {

		try {
			this.bootStrap("router");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:router'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainSplitter() throws Exception {

		try {
			this.bootStrap("splitter");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:splitter'.", e.getCause().getMessage());
		}
	}

	@Test
	public void chainResequencer() throws Exception {

		try {
			this.bootStrap("resequencer");
			fail("Expected a XmlBeanDefinitionStoreException to be thrown.");
		}
		catch (XmlBeanDefinitionStoreException e) {
			assertEquals("cvc-complex-type.3.2.2: Attribute 'input-channel' is not" +
					" allowed to appear in element 'int:resequencer'.", e.getCause().getMessage());
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
			assertTrue("Error message did not start with '" + expectedMessage +
					"' but instead returned: '" + actualMessage + "'", actualMessage.startsWith(expectedMessage));
		}
	}

	@Test
	public void testInt2755DetectDuplicateHandlerId() throws Exception {

		try {
			this.bootStrap("duplicate-handler-id");
			fail("Expected a BeanDefinitionParsingException to be thrown.");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("A bean definition is already registered for " +
					"beanName: 'foo$child.bar.handler' within the current <chain>."));
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
