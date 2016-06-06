/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.file.remote;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 4.1.7
 *
 */
public class RemoteFileTemplateTests {

	private RemoteFileTemplate<Object> template;

	private Session<Object> session;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private File file;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		SessionFactory<Object> sessionFactory = mock(SessionFactory.class);
		this.template = new RemoteFileTemplate<Object>(sessionFactory);
		this.template.setRemoteDirectoryExpression(new LiteralExpression("/foo"));
		this.template.setBeanFactory(mock(BeanFactory.class));
		this.template.afterPropertiesSet();
		this.session = mock(Session.class);
		when(sessionFactory.getSession()).thenReturn(this.session);
		this.file = this.folder.newFile();
	}

	@Test
	public void testReplace() throws Exception {
		this.template.send(new GenericMessage<File>(this.file), FileExistsMode.REPLACE);
		verify(this.session).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@Test
	public void testAppend() throws Exception {
		this.template.setUseTemporaryFileName(false);
		this.template.send(new GenericMessage<File>(this.file), FileExistsMode.APPEND);
		verify(this.session).append(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@Test
	public void testFailExists() throws Exception {
		when(session.exists(Mockito.anyString())).thenReturn(true);
		try {
			this.template.send(new GenericMessage<File>(this.file), FileExistsMode.FAIL);
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage(), Matchers.containsString("The destination file already exists"));
		}
		verify(this.session, never()).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@Test
	public void testIgnoreExists() throws Exception {
		when(session.exists(Mockito.anyString())).thenReturn(true);
		this.template.send(new GenericMessage<File>(this.file), FileExistsMode.IGNORE);
		verify(this.session, never()).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@Test
	public void testFailNotExists() throws Exception {
		when(session.exists(Mockito.anyString())).thenReturn(false);
		this.template.send(new GenericMessage<File>(this.file), FileExistsMode.FAIL);
		verify(this.session).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

	@Test
	public void testIgnoreNotExists() throws Exception {
		when(session.exists(Mockito.anyString())).thenReturn(false);
		this.template.send(new GenericMessage<File>(this.file), FileExistsMode.IGNORE);
		verify(this.session).write(Mockito.any(InputStream.class), Mockito.anyString());
	}

}
