/*
 * Copyright 2014 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.ExpressionMetadataKeyStrategy;
import org.springframework.integration.metadata.MetadataKeyStrategy;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class IdempotentReceiverParserTests {

	@Autowired
	@Qualifier("org.springframework.integration.config.IdempotentReceiverAutoProxyCreator")
	private BeanPostProcessor idempotentReceiverAutoProxyCreator;

	@Autowired
	private IdempotentReceiverInterceptor selectorInterceptor;

	@Autowired
	private MessageSelector selector;

	@Autowired
	private IdempotentReceiverInterceptor strategyInterceptor;

	@Autowired
	private MetadataKeyStrategy keyStrategy;

	@Autowired
	@Qualifier("nullChannel")
	private MessageChannel nullChannel;

	@Autowired
	private IdempotentReceiverInterceptor expressionInterceptor;

	@Autowired
	private MetadataStore store;

	@Test
	public void testSelectorInterceptor() {
		assertSame(this.selector, getPropertyValue(this.selectorInterceptor, "messageSelector"));
		assertNull(getPropertyValue(this.selectorInterceptor, "discardChannel"));
		assertFalse(getPropertyValue(this.selectorInterceptor, "throwExceptionOnRejection", Boolean.class));
		@SuppressWarnings("unchecked")
		Map<String, List<String>> idempotentEndpoints =
				(Map<String, List<String>>) getPropertyValue(this.idempotentReceiverAutoProxyCreator,
						"idempotentEndpoints", Map.class);
		List<String> endpoints = idempotentEndpoints.get("selectorInterceptor");
		assertNotNull(endpoints);
		assertFalse(endpoints.isEmpty());
		assertTrue(endpoints.contains("foo"));
	}

	@Test
	public void testStrategyInterceptor() {
		assertSame(this.nullChannel, getPropertyValue(this.strategyInterceptor, "discardChannel"));
		assertTrue(getPropertyValue(this.strategyInterceptor, "throwExceptionOnRejection", Boolean.class));
		Object messageSelector = getPropertyValue(this.strategyInterceptor, "messageSelector");
		assertThat(messageSelector, instanceOf(MetadataStoreSelector.class));
		assertSame(this.keyStrategy, getPropertyValue(messageSelector, "keyStrategy"));
		@SuppressWarnings("unchecked")
		Map<String, List<String>> idempotentEndpoints =
				(Map<String, List<String>>) getPropertyValue(this.idempotentReceiverAutoProxyCreator,
						"idempotentEndpoints", Map.class);
		List<String> endpoints = idempotentEndpoints.get("strategyInterceptor");
		assertNotNull(endpoints);
		assertFalse(endpoints.isEmpty());
		assertTrue(endpoints.contains("foo"));
	}

	@Test
	public void testExpressionInterceptor() {
		Object messageSelector = getPropertyValue(this.expressionInterceptor, "messageSelector");
		assertThat(messageSelector, instanceOf(MetadataStoreSelector.class));
		assertSame(this.store, getPropertyValue(messageSelector, "metadataStore"));
		Object keyStrategy = getPropertyValue(messageSelector, "keyStrategy");
		assertThat(keyStrategy, instanceOf(ExpressionMetadataKeyStrategy.class));
		assertThat(keyStrategy.toString(), containsString("headers.foo"));
		@SuppressWarnings("unchecked")
		Map<String, List<String>> idempotentEndpoints =
				(Map<String, List<String>>) getPropertyValue(this.idempotentReceiverAutoProxyCreator,
						"idempotentEndpoints", Map.class);
		List<String> endpoints = idempotentEndpoints.get("expressionInterceptor");
		assertNotNull(endpoints);
		assertFalse(endpoints.isEmpty());
		assertTrue(endpoints.contains("foo"));
		assertTrue(endpoints.contains("bar*"));
	}

	@Test
	public void testEmpty() throws Exception {
		try {
			bootStrap("empty");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(),
					containsString("One of the 'selector', 'key-strategy' or 'key-expression' attributes " +
							"must be provided"));
		}
	}

	@Test
	public void testWithoutEndpoint() throws Exception {
		try {
			bootStrap("without-endpoint");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(),
					containsString("he 'endpoint' attribute is required"));
		}
	}

	@Test
	public void testSelectorAndStore() throws Exception {
		try {
			bootStrap("selector-and-store");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(),
					containsString("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
							"'key-strategy' or 'key-expression'"));
		}
	}

	@Test
	public void testSelectorAndStrategy() throws Exception {
		try {
			bootStrap("selector-and-strategy");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(),
					containsString("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
							"'key-strategy' or 'key-expression'"));
		}
	}

	@Test
	public void testSelectorAndExpression() throws Exception {
		try {
			bootStrap("selector-and-expression");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(),
					containsString("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
							"'key-strategy' or 'key-expression'"));
		}
	}

	@Test
	public void testStrategyAndExpression() throws Exception {
		try {
			bootStrap("strategy-and-expression");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage(),
					containsString("The 'key-strategy' and 'key-expression' attributes are mutually exclusive"));
		}
	}

	private ApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource(
				"org/springframework/integration/config/xml/idempotent-receiver-configs.properties"));
		pfb.afterPropertiesSet();
		Properties prop = pfb.getObject();
		ByteArrayInputStream stream = new ByteArrayInputStream((prop.getProperty("xmlheaders")
				+ prop.getProperty(configProperty) + prop.getProperty("xmlfooter")).getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

}
