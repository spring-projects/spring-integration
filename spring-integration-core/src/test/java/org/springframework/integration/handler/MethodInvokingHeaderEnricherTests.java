/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MethodInvokingHeaderEnricherTests implements TestApplicationContextAware {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void emptyHeadersOnRequest() {
		TestBean testBean = new TestBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, "process");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		HeaderEnricher enricher = new HeaderEnricher();
		enricher.setMessageProcessor(processor);
		enricher.setDefaultOverwrite(true);
		Message<?> message = MessageBuilder.withPayload("test").build();
		Message<?> result = enricher.transform(message);
		assertThat(result.getHeaders().get("foo")).isEqualTo("TEST");
		assertThat(result.getHeaders().get("bar")).isEqualTo("ABC");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void overwriteFalseByDefault() {
		TestBean testBean = new TestBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, "process");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		HeaderEnricher enricher = new HeaderEnricher();
		enricher.setMessageProcessor(processor);
		Message<?> message = MessageBuilder.withPayload("test").setHeader("bar", "XYZ").build();
		Message<?> result = enricher.transform(message);
		assertThat(result.getHeaders().get("foo")).isEqualTo("TEST");
		assertThat(result.getHeaders().get("bar")).isEqualTo("XYZ");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void overwriteFalseExplicit() {
		TestBean testBean = new TestBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, "process");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		HeaderEnricher enricher = new HeaderEnricher();
		enricher.setMessageProcessor(processor);
		enricher.setDefaultOverwrite(false);
		Message<?> message = MessageBuilder.withPayload("test").setHeader("bar", "XYZ").build();
		Message<?> result = enricher.transform(message);
		assertThat(result.getHeaders().get("foo")).isEqualTo("TEST");
		assertThat(result.getHeaders().get("bar")).isEqualTo("XYZ");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void overwriteTrue() {
		TestBean testBean = new TestBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, "process");
		processor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		HeaderEnricher enricher = new HeaderEnricher();
		enricher.setMessageProcessor(processor);
		enricher.setDefaultOverwrite(true);
		Message<?> message = MessageBuilder.withPayload("test").setHeader("bar", "XYZ").build();
		Message<?> result = enricher.transform(message);
		assertThat(result.getHeaders().get("foo")).isEqualTo("TEST");
		assertThat(result.getHeaders().get("bar")).isEqualTo("ABC");
	}

	@Test
	public void overwriteId() {
		HeaderEnricher enricher =
				new HeaderEnricher(Collections.singletonMap(MessageHeaders.ID,
						new StaticHeaderValueMessageProcessor<>("foo")));

		try {
			enricher.setBeanFactory(TEST_INTEGRATION_CONTEXT);
			enricher.afterPropertiesSet();
			fail("BeanInitializationException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(BeanInitializationException.class);
			assertThat(e.getMessage())
					.contains("HeaderEnricher cannot override 'id' and 'timestamp' read-only headers.");
		}
	}

	public static class TestBean {

		public Map<String, Object> process(@Payload("toUpperCase()") String s) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("foo", s);
			map.put("bar", "ABC");
			return map;
		}

	}

}
