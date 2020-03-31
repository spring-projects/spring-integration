/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.file.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1.7
 *
 */
public class RemoteFileTemplateTests {

	private RemoteFileTemplate<Object> template;

	private Session<Object> session;

	@TempDir
	Path folder;

	private File file;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void setUp() throws Exception {
		SessionFactory<Object> sessionFactory = mock(SessionFactory.class);
		this.template = new RemoteFileTemplate<>(sessionFactory);
		this.template.setRemoteDirectoryExpression(new LiteralExpression("/foo"));
		this.template.setBeanFactory(mock(BeanFactory.class));
		this.template.afterPropertiesSet();
		this.session = mock(Session.class);
		when(sessionFactory.getSession()).thenReturn(this.session);
		this.file = Files.createTempFile(this.folder, null, null).toFile();
	}

	@Test
	public void testReplace() throws Exception {
		this.template.send(new GenericMessage<>(this.file), FileExistsMode.REPLACE);
		verify(this.session).write(any(InputStream.class), anyString());
	}

	@Test
	public void testAppend() throws Exception {
		this.template.setUseTemporaryFileName(false);
		this.template.send(new GenericMessage<>(this.file), FileExistsMode.APPEND);
		verify(this.session).append(any(InputStream.class), anyString());
	}

	@Test
	public void testFailExists() throws Exception {
		when(session.exists(anyString())).thenReturn(true);
		try {
			this.template.send(new GenericMessage<>(this.file), FileExistsMode.FAIL);
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage()).contains("The destination file already exists");
		}
		verify(this.session, never()).write(any(InputStream.class), anyString());
	}

	@Test
	public void testIgnoreExists() throws Exception {
		when(session.exists(anyString())).thenReturn(true);
		this.template.send(new GenericMessage<>(this.file), FileExistsMode.IGNORE);
		verify(this.session, never()).write(any(InputStream.class), anyString());
	}

	@Test
	public void testFailNotExists() throws Exception {
		when(session.exists(anyString())).thenReturn(false);
		this.template.send(new GenericMessage<>(this.file), FileExistsMode.FAIL);
		verify(this.session).write(any(InputStream.class), anyString());
	}

	@Test
	public void testIgnoreNotExists() throws Exception {
		when(session.exists(anyString())).thenReturn(false);
		this.template.send(new GenericMessage<>(this.file), FileExistsMode.IGNORE);
		verify(this.session).write(any(InputStream.class), anyString());
	}

	@Test
	public void testStream() throws IOException {
		ByteArrayInputStream stream = new ByteArrayInputStream("foo".getBytes());
		this.template.send(new GenericMessage<>(stream),
				FileExistsMode.IGNORE);
		verify(this.session).write(eq(stream), any());
	}

	@Test
	public void testString() throws IOException {
		this.template.send(new GenericMessage<>("foo"), FileExistsMode.IGNORE);
		verify(this.session).write(any(InputStream.class), any());
	}

	@Test
	public void testBytes() throws IOException {
		this.template.send(new GenericMessage<>("foo".getBytes()), FileExistsMode.IGNORE);
		verify(this.session).write(any(InputStream.class), any());
	}

	@Test
	public void testResource() throws IOException {
		this.template.send(new GenericMessage<>(new ByteArrayResource("foo".getBytes())), FileExistsMode.IGNORE);
		verify(this.session).write(any(InputStream.class), any());
	}

	@Test
	public void testMissingFile() {
		this.template.send(new GenericMessage<>(new File(UUID.randomUUID().toString())), FileExistsMode.IGNORE);
		verifyNoMoreInteractions(this.session);
	}

	@Test
	public void testInvalid() {
		assertThatThrownBy(() -> this.template
				.send(new GenericMessage<>(new Object()), FileExistsMode.IGNORE))
			.isInstanceOf(MessageDeliveryException.class)
			.hasCauseInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Unsupported payload type");
	}

}
