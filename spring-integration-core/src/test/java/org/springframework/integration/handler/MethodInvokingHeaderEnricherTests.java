/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MethodInvokingHeaderEnricherTests {

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Test
	public void emptyHeadersOnRequest() {
		TestBean testBean = new TestBean();
		MethodInvokingMessageProcessor processor = new MethodInvokingMessageProcessor(testBean, "process");
		processor.setBeanFactory(mock(BeanFactory.class));
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
		processor.setBeanFactory(mock(BeanFactory.class));
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
		processor.setBeanFactory(mock(BeanFactory.class));
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
		processor.setBeanFactory(mock(BeanFactory.class));
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
