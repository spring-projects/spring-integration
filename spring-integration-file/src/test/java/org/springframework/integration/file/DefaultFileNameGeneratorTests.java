/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class DefaultFileNameGeneratorTests {

	@Test
	public void defaultHeaderNamePresent() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = MessageBuilder.withPayload("test").setHeader(FileHeaders.FILENAME, "foo").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("foo");
	}

	@Test
	public void defaultHeaderNameNotPresent() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = MessageBuilder.withPayload("test").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo(message.getHeaders().getId() + ".msg");
	}

	@Test
	public void defaultHeaderNameNotString() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		Message<?> message = MessageBuilder.withPayload("test").setHeader(FileHeaders.FILENAME, 123)
				.build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo(message.getHeaders().getId() + ".msg");
	}

	@Test
	public void customHeaderNamePresent() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setHeaderName("foo");
		Message<?> message = MessageBuilder.withPayload("test").setHeader("foo", "bar").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("bar");
	}

	@Test
	public void customHeaderNameNotPresent() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setHeaderName("foo");
		Message<?> message = MessageBuilder.withPayload("test").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo(message.getHeaders().getId() + ".msg");
	}

	@Test
	public void customHeaderNameNotString() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setHeaderName("foo");
		Message<?> message = MessageBuilder.withPayload("test").setHeader("foo", 123).build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo(message.getHeaders().getId() + ".msg");
	}

	@Test
	public void filePayloadPresent() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		File payload = new File("/some/path/foo");
		Message<?> message = MessageBuilder.withPayload(payload).build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("foo");
	}

	@Test
	public void defaultHeaderNameTakesPrecedenceOverFilePayload() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		File payload = new File("/some/path/ignore");
		Message<?> message = MessageBuilder.withPayload(payload).setHeader(FileHeaders.FILENAME, "foo").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("foo");
	}

	@Test
	public void customHeaderNameTakesPrecedenceOverFilePayload() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setHeaderName("foo");
		File payload = new File("/some/path/ignore");
		Message<?> message = MessageBuilder.withPayload(payload).setHeader("foo", "bar").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("bar");
	}

	@Test
	public void customExpressionTakesPrecedenceOverFilePayload() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setExpression("'foobar'");
		File payload = new File("/some/path/ignore");
		Message<?> message = MessageBuilder.withPayload(payload).build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("foobar");
	}

	@Test
	public void customHeaderNameTakesPrecedenceOverDefault() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setHeaderName("foo");
		Message<?> message = MessageBuilder.withPayload("test").setHeader(FileHeaders.FILENAME, "ignore")
				.setHeader("foo", "bar").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("bar");
	}

	@Test
	public void customHeaderNameTakesPrecedenceOverFilePayloadAndDefault() {
		DefaultFileNameGenerator generator = new DefaultFileNameGenerator();
		generator.setBeanFactory(mock(BeanFactory.class));
		generator.setHeaderName("foo");
		File payload = new File("/some/path/ignore1");
		Message<?> message = MessageBuilder.withPayload(payload).setHeader(FileHeaders.FILENAME, "ignore2")
				.setHeader("foo", "bar").build();
		String filename = generator.generateFileName(message);
		assertThat(filename).isEqualTo("bar");
	}

}
