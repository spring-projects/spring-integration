/*
 * Copyright 2014-2022 the original author or authors.
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

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
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
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
	private MessageProcessor<String> keyStrategy;

	@Autowired
	private MessageProcessor<String> valueStrategy;

	@Autowired
	private AlwaysAccept alwaysAccept;

	@Autowired
	@Qualifier("nullChannel")
	private MessageChannel nullChannel;

	@Autowired
	private IdempotentReceiverInterceptor expressionInterceptor;

	@Autowired
	private MetadataStore store;

	@Test
	public void testSelectorInterceptor() {
		assertThat(getPropertyValue(this.selectorInterceptor, "messageSelector")).isSameAs(this.selector);
		assertThat(getPropertyValue(this.selectorInterceptor, "discardChannel")).isNull();
		assertThat(getPropertyValue(this.selectorInterceptor, "throwExceptionOnRejection", Boolean.class)).isFalse();
		@SuppressWarnings("unchecked")
		Map<String, List<String>> idempotentEndpoints =
				getPropertyValue(this.idempotentReceiverAutoProxyCreator,
						"idempotentEndpoints", Map.class);
		List<String> endpoints = idempotentEndpoints.get("selectorInterceptor");
		assertThat(endpoints).isNotNull();
		assertThat(endpoints.isEmpty()).isFalse();
		assertThat(endpoints.contains("foo.handler")).isTrue();
	}

	@Test
	public void testStrategyInterceptor() {
		assertThat(getPropertyValue(this.strategyInterceptor, "discardChannel")).isSameAs(this.nullChannel);
		assertThat(getPropertyValue(this.strategyInterceptor, "throwExceptionOnRejection", Boolean.class)).isTrue();
		Object messageSelector = getPropertyValue(this.strategyInterceptor, "messageSelector");
		assertThat(messageSelector).isInstanceOf(MetadataStoreSelector.class);
		assertThat(getPropertyValue(messageSelector, "keyStrategy")).isSameAs(this.keyStrategy);
		assertThat(getPropertyValue(messageSelector, "valueStrategy")).isSameAs(this.valueStrategy);
		assertThat(getPropertyValue(messageSelector, "compareValues")).isSameAs(this.alwaysAccept);
		@SuppressWarnings("unchecked")
		Map<String, List<String>> idempotentEndpoints =
				getPropertyValue(this.idempotentReceiverAutoProxyCreator,
						"idempotentEndpoints", Map.class);
		List<String> endpoints = idempotentEndpoints.get("strategyInterceptor");
		assertThat(endpoints).isNotNull();
		assertThat(endpoints.isEmpty()).isFalse();
		assertThat(endpoints.contains("foo.handler")).isTrue();
	}

	@Test
	public void testExpressionInterceptor() {
		Object messageSelector = getPropertyValue(this.expressionInterceptor, "messageSelector");
		assertThat(messageSelector).isInstanceOf(MetadataStoreSelector.class);
		assertThat(getPropertyValue(messageSelector, "metadataStore")).isSameAs(this.store);
		Object keyStrategy = getPropertyValue(messageSelector, "keyStrategy");
		assertThat(keyStrategy).isInstanceOf(ExpressionEvaluatingMessageProcessor.class);
		assertThat(keyStrategy.toString()).contains("headers.foo");
		@SuppressWarnings("unchecked")
		Map<String, List<String>> idempotentEndpoints =
				getPropertyValue(this.idempotentReceiverAutoProxyCreator,
						"idempotentEndpoints", Map.class);
		List<String> endpoints = idempotentEndpoints.get("expressionInterceptor");
		assertThat(endpoints).isNotNull();
		assertThat(endpoints.isEmpty()).isFalse();
		assertThat(endpoints.contains("foo.handler")).isTrue();
		assertThat(endpoints.contains("bar*.handler")).isTrue();
	}

	@Test
	public void testEmpty() throws Exception {
		try {
			bootStrap("empty");
			fail("BeanDefinitionParsingException expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage())
					.contains("One of the 'selector', 'key-strategy' or 'key-expression' attributes " +
							"must be provided");
		}
	}

	@Test
	public void testWithoutEndpoint() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("without-endpoint"))
				.withMessageContaining("The 'endpoint' attribute is required");
	}

	@Test
	public void testSelectorAndStore() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("selector-and-store"))
				.withMessageContaining("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
						"'key-strategy', 'key-expression', 'value-strategy', 'value-expression', and "
						+ "'compare-values'");
	}

	@Test
	public void testSelectorAndKeyStrategy() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("selector-and-key-strategy"))
				.withMessageContaining("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
						"'key-strategy', 'key-expression', 'value-strategy', 'value-expression', and "
						+ "'compare-values'");
	}

	@Test
	public void testSelectorAndKeyExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("selector-and-key-expression"))
				.withMessageContaining("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
						"'key-strategy', 'key-expression', 'value-strategy', 'value-expression', and "
						+ "'compare-values'");
	}

	@Test
	public void testSelectorAndValueStrategy() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("selector-and-value-strategy"))
				.withMessageContaining("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
						"'key-strategy', 'key-expression', 'value-strategy', 'value-expression', and "
						+ "'compare-values'");
	}

	@Test
	public void testSelectorAndValueExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("selector-and-value-expression"))
				.withMessageContaining("The 'selector' attribute is mutually exclusive with 'metadata-store', " +
						"'key-strategy', 'key-expression', 'value-strategy', 'value-expression', and "
						+ "'compare-values'");
	}

	@Test
	public void testKeyStrategyAndKeyExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("key-strategy-and-key-expression"))
				.withMessageContaining("The 'key-strategy' and 'key-expression' attributes are mutually exclusive");
	}

	@Test
	public void testValueStrategyAndValueExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("value-strategy-and-value-expression"))
				.withMessageContaining("The 'value-strategy' and 'value-expression' attributes are mutually exclusive");
	}

	private static ApplicationContext bootStrap(String configProperty) throws Exception {
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

	public static class AlwaysAccept implements BiPredicate<String, String> {

		@Override
		public boolean test(String t, String u) {
			return true;
		}

	}

}
