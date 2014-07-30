/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.file.remote.gateway;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;


/**
 * @author Gary Russell
 * @author liujiong
 * @since 2.1
 */
@SuppressWarnings("rawtypes")
public class RemoteFileOutboundGatewayTests {

	private final String tmpDir = System.getProperty("java.io.tmpdir");

	@Rule
	public final TemporaryFolder tempFolder = new TemporaryFolder();


	@Test(expected = IllegalArgumentException.class)
	public void testBad() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "bad", "payload");
		gw.afterPropertiesSet();
	}

	@Test
	public void testBadFilterGet() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "get", "payload");
		gw.setFilter(new TestPatternFilter(""));
		try {
			gw.afterPropertiesSet();
			fail("Exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith("Filters are not supported"));
		}
	}

	@Test
	public void testBadFilterRm() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "rm", "payload");
		gw.setFilter(new TestPatternFilter(""));
		try {
			gw.afterPropertiesSet();
			fail("Exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().startsWith("Filters are not supported"));
		}
	}

	@Test
	public void testLs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/x/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x"));
		assertEquals(2, out.getPayload().size());
		assertSame(files[1], out.getPayload().get(0)); // sort by default
		assertSame(files[0], out.getPayload().get(1));
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testMGetWild() throws Exception {
		testMGetWildGuts("f1", "f2");
	}

	/**
	 * Test a wildcard mget where the full path is returned for each file
	 *
	 * @throws Exception
	 */
	@Test
	public void testMGetWildFullPath() throws Exception {
		testMGetWildGuts("testremote/f1", "testremote/f2");
	}

	private void testMGetWildGuts(final String path1, final String path2) {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "mget", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		new File(this.tmpDir + "/f2").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {
			int n;

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				if (n++ == 0) {
					assertEquals("testremote/f1", source);
				}
				else {
					assertEquals("testremote/f2", source);
				}
				outputStream.write("testData".getBytes());
			}

			@Override
			public String[] listNames(String path) throws IOException {
				return new String[] { path1, path2 };
			}

		});
		@SuppressWarnings("unchecked")
		Message<List<File>> out = (Message<List<File>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/*"));
		assertEquals(2, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0).getName());
		assertEquals("f2", out.getPayload().get(1).getName());
		assertEquals("testremote/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testMGetSingle() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "mget", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testData".getBytes());
			}

			@Override
			public String[] listNames(String path) throws IOException {
				return new String[]{"f1"};
			}

		});
		@SuppressWarnings("unchecked")
		Message<List<File>> out = (Message<List<File>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/f1"));
		assertEquals(1, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0).getName());
		assertEquals("testremote/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test(expected = MessagingException.class)
	public void testMGetEmpty() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "mget", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.setOptions("   -x   ");
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		new File(this.tmpDir + "/f2").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testData".getBytes());
			}

		});
		gw.handleRequestMessage(new GenericMessage<String>("testremote/*"));
	}

	@Test
	public void testMove() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "mv", "payload");
		gw.afterPropertiesSet();
		Session<?> session = mock(Session.class);
		final AtomicReference<String> args = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				args.set((String) arguments[0] + (String) arguments[1]);
				return null;
			}
		}).when(session).rename(anyString(), anyString());
		when(sessionFactory.getSession()).thenReturn(session);
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.RENAME_TO, "bar")
				.build();
		Message<?> out = (Message<?>) gw.handleRequestMessage(requestMessage);
		assertEquals("foo", out.getHeaders().get(FileHeaders.REMOTE_FILE));
		assertEquals("foobar", args.get());
		assertEquals(Boolean.TRUE, out.getPayload());
	}

	@Test
	public void testMoveWithExpression() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "mv", "payload");
		gw.setRenameExpression("payload.substring(1)");
		gw.afterPropertiesSet();
		Session<?> session = mock(Session.class);
		final AtomicReference<String> args = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				args.set((String) arguments[0] + (String) arguments[1]);
				return null;
			}
		}).when(session).rename(anyString(), anyString());
		when(sessionFactory.getSession()).thenReturn(session);
		Message<?> out = (Message<?>) gw.handleRequestMessage(new GenericMessage<String>("foo"));
		assertEquals("oo", out.getHeaders().get(FileHeaders.RENAME_TO));
		assertEquals("foo", out.getHeaders().get(FileHeaders.REMOTE_FILE));
		assertEquals("foooo", args.get());
		assertEquals(Boolean.TRUE, out.getPayload());
	}

	@Test
	public void testMoveWithMkDirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "mv", "payload");
		gw.setRenameExpression("'foo/bar/baz'");
		gw.afterPropertiesSet();
		Session<?> session = mock(Session.class);
		final AtomicReference<String> args = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object[] arguments = invocation.getArguments();
				args.set((String) arguments[0] + (String) arguments[1]);
				return null;
			}
		}).when(session).rename(anyString(), anyString());
		final List<String> madeDirs = new ArrayList<String>();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				madeDirs.add((String) invocation.getArguments()[0]);
				return null;
			}
		}).when(session).mkdir(anyString());
		when(sessionFactory.getSession()).thenReturn(session);
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.RENAME_TO, "bar")
				.build();
		Message<?> out = (Message<?>) gw.handleRequestMessage(requestMessage);
		assertEquals("foo", out.getHeaders().get(FileHeaders.REMOTE_FILE));
		assertEquals("foofoo/bar/baz", args.get());
		assertEquals(Boolean.TRUE, out.getPayload());
		assertEquals(2, madeDirs.size());
		assertEquals("foo", madeDirs.get(0));
		assertEquals("foo/bar", madeDirs.get(1));
	}

	public TestLsEntry[] fileList() {
		TestLsEntry[] files = new TestLsEntry[6];
		files[0] = new TestLsEntry("f2", 123, false, false, 1234, "-r--r--r--");
		files[1] = new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--");
		files[2] = new TestLsEntry("f3", 12345, true, false, 123456, "drw-r--r--");
		files[3] = new TestLsEntry("f4", 12346, false, true, 1234567, "lrw-r--r--");
		files[4] = new TestLsEntry(".f5", 12347, false, false, 12345678, "-rw-r--r--");
		files[5] = new TestLsEntry(".f6", 12347, true, false, 123456789, "drw-r--r--");
		return files;
	}

	@Test
	public void testLs_f() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-f");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/x/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x"));
		assertEquals(2, out.getPayload().size());
		assertSame(files[0], out.getPayload().get(0));
		assertSame(files[1], out.getPayload().get(1));
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	public TestLsEntry[] level1List() {
		return new TestLsEntry[]{
				new TestLsEntry("f1", 123, false, false, 1234, "-r--r--r--"),
				new TestLsEntry("d1", 0, true, false, 12345, "drw-r--r--"),
				new TestLsEntry("f2", 12345, false, false, 123456, "-rw-r--r--")
		};
	}

	public TestLsEntry[] level2List() {
		return new TestLsEntry[]{
				new TestLsEntry("d2", 0, true, false, 12345, "drw-r--r--"),
				new TestLsEntry("f3", 12345, false, false, 123456, "-rw-r--r--")
		};
	}

	public TestLsEntry[] level3List() {
		return new TestLsEntry[]{
				new TestLsEntry("f4", 12345, false, false, 123456, "-rw-r--r--")
		};
	}

	@Test
	public void testLs_f_R() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-f -R");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] level1 = level1List();
		TestLsEntry[] level2 = level2List();
		TestLsEntry[] level3 = level3List();
		when(session.list("testremote/x/")).thenReturn(level1);
		when(session.list("testremote/x/d1/")).thenReturn(level2);
		when(session.list("testremote/x/d1/d2/")).thenReturn(level3);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x"));
		assertEquals(4, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0).getFilename());
		assertEquals("d1/d2/f4", out.getPayload().get(1).getFilename());
		assertEquals("d1/f3", out.getPayload().get(2).getFilename());
		assertEquals("f2", out.getPayload().get(3).getFilename());
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testLs_f_R_dirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-f -R -dirs");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] level1 = level1List();
		TestLsEntry[] level2 = level2List();
		TestLsEntry[] level3 = level3List();
		when(session.list("testremote/x/")).thenReturn(level1);
		when(session.list("testremote/x/d1/")).thenReturn(level2);
		when(session.list("testremote/x/d1/d2/")).thenReturn(level3);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x"));
		assertEquals(6, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0).getFilename());
		assertEquals("d1", out.getPayload().get(1).getFilename());
		assertEquals("d1/d2", out.getPayload().get(2).getFilename());
		assertEquals("d1/d2/f4", out.getPayload().get(3).getFilename());
		assertEquals("d1/f3", out.getPayload().get(4).getFilename());
		assertEquals("f2", out.getPayload().get(5).getFilename());
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
	}

	@Test
	public void testLs_None() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = new TestLsEntry[0];
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<TestLsEntry>> out = (Message<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(0, out.getPayload().size());
	}

	@Test
	public void testLs_1() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-1");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(2, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0));
		assertEquals("f2", out.getPayload().get(1));
	}

	@Test
	public void testLs_1_f() throws Exception { //no sort
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -f");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(2, out.getPayload().size());
		assertEquals("f2", out.getPayload().get(0));
		assertEquals("f1", out.getPayload().get(1));
	}

	@Test
	public void testLs_1_dirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -dirs");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(3, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0));
		assertEquals("f2", out.getPayload().get(1));
		assertEquals("f3", out.getPayload().get(2));
	}

	@Test
	public void testLs_1_dirs_links() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -dirs -links");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(4, out.getPayload().size());
		assertEquals("f1", out.getPayload().get(0));
		assertEquals("f2", out.getPayload().get(1));
		assertEquals("f3", out.getPayload().get(2));
		assertEquals("f4", out.getPayload().get(3));
	}

	@Test
	public void testLs_1_a_f_dirs_links() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -a -f -dirs -links");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(6, out.getPayload().size());
		assertEquals("f2", out.getPayload().get(0));
		assertEquals("f1", out.getPayload().get(1));
		assertEquals("f3", out.getPayload().get(2));
		assertEquals("f4", out.getPayload().get(3));
		assertEquals(".f5", out.getPayload().get(4));
		assertEquals(".f6", out.getPayload().get(5));
	}

	@Test
	public void testLs_1_a_f_dirs_links_filtered() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -a -f -dirs -links");
		gw.setFilter(new TestPatternFilter("*4"));
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote"));
		assertEquals(1, out.getPayload().size());
		assertEquals("f4", out.getPayload().get(0));
	}

	@Test
	public void testGet() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[]{
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}

		});
		@SuppressWarnings("unchecked")
		Message<File> out = (Message<File>) gw.handleRequestMessage(new GenericMessage<String>("f1"));
		File outFile = new File(this.tmpDir + "/f1");
		assertEquals(outFile, out.getPayload());
		assertTrue(outFile.exists());
		outFile.delete();
		assertEquals("/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("f1",
				out.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testGetTempFileDelete() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[]{
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream) {
				throw new RuntimeException("test remove .writing");
			}

		});
		try{
			gw.handleRequestMessage(new GenericMessage<String>("f1"));
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getCause(), instanceOf(RuntimeException.class));
			assertEquals("test remove .writing", e.getCause().getMessage());
			@SuppressWarnings("unchecked")
			RemoteFileTemplate template = new RemoteFileTemplate(sessionFactory);
			File outFile = new File(this.tmpDir + "/f1"+ template.getTemporaryFileSuffix());
			assertFalse(outFile.exists());
		}
	}


	@Test
	public void testGet_P() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.setOptions("-P");
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		final Date modified = new Date(cal.getTime().getTime() / 1000 * 1000);
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[]{
						new TestLsEntry("f1", 1234, false, false, modified.getTime(), "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}

		});
		@SuppressWarnings("unchecked")
		Message<File> out = (Message<File>) gw.handleRequestMessage(new GenericMessage<String>("x/f1"));
		File outFile = new File(this.tmpDir + "/f1");
		assertEquals(outFile, out.getPayload());
		assertTrue(outFile.exists());
		assertEquals(modified.getTime(), outFile.lastModified());
		outFile.delete();
		assertEquals("x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("f1",
				out.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testGet_create_dir() throws Exception {
		new File(this.tmpDir + "/x/f1").delete();
		new File(this.tmpDir + "/x").delete();
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir + "/x"));
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) throws IOException {
				return new TestLsEntry[]{
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}

		});
		gw.handleRequestMessage(new GenericMessage<String>("f1"));
		File out = new File(this.tmpDir + "/x/f1");
		assertTrue(out.exists());
		out.delete();
	}

	@Test
	public void testRm() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway
				(sessionFactory, "rm", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		when(session.remove("testremote/x/f1")).thenReturn(Boolean.TRUE);
		@SuppressWarnings("unchecked")
		Message<Boolean> out = (Message<Boolean>) gw
				.handleRequestMessage(new GenericMessage<String>("testremote/x/f1"));
		assertEquals(Boolean.TRUE, out.getPayload());
		verify(session).remove("testremote/x/f1");
		assertEquals("testremote/x/",
				out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("f1",
				out.getHeaders().get(FileHeaders.REMOTE_FILE));
	}

	@Test
	public void testPut() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestLsEntry> session = mock(Session.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<TestLsEntry>(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "put", null);
		FileTransferringMessageHandler<TestLsEntry> handler = new FileTransferringMessageHandler<TestLsEntry>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		final AtomicReference<String> written = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				written.set((String) invocation.getArguments()[1]);
				return null;
			}
		}).when(session).write(any(InputStream.class), anyString());
		Message<String> requestMessage = MessageBuilder.withPayload("hello")
				.setHeader(FileHeaders.FILENAME, "bar.txt")
				.build();
		String path = (String) gw.handleRequestMessage(requestMessage);
		assertEquals("foo/bar.txt", path);
		verify(session).rename("foo/bar.txt.writing", "foo/bar.txt");
	}

	@Test
	public void testMput() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestLsEntry> session = mock(Session.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<TestLsEntry>(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "mput", null);
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		final AtomicReference<String> written = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				written.set((String) invocation.getArguments()[1]);
				return null;
			}
		}).when(session).write(any(InputStream.class), anyString());
		tempFolder.newFile("baz.txt");
		tempFolder.newFile("qux.txt");
		Message<File> requestMessage = MessageBuilder.withPayload(tempFolder.getRoot())
				.build();
		@SuppressWarnings("unchecked")
		List<String> out = (List<String>) gw.handleRequestMessage(requestMessage);
		assertEquals(2, out.size());
		assertThat(out.get(0),
				not(equalTo(out.get(1))));
		assertThat(out.get(0), anyOf(
				equalTo("foo/baz.txt"), equalTo("foo/qux.txt")));
		assertThat(out.get(1), anyOf(
				equalTo("foo/baz.txt"), equalTo("foo/qux.txt")));
	}

	@Test
	public void testMputRecursive() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestLsEntry> session = mock(Session.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<TestLsEntry>(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "mput", null);
		gw.setOptions("-R");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		final AtomicReference<String> written = new AtomicReference<String>();
		doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				written.set((String) invocation.getArguments()[1]);
				return null;
			}
		}).when(session).write(any(InputStream.class), anyString());
		tempFolder.newFile("baz.txt");
		tempFolder.newFile("qux.txt");
		File dir1 = tempFolder.newFolder();
		File file3 = File.createTempFile("foo", ".txt", dir1);

		Message<File> requestMessage = MessageBuilder.withPayload(tempFolder.getRoot())
				.build();
		@SuppressWarnings("unchecked")
		List<String> out = (List<String>) gw.handleRequestMessage(requestMessage);
		assertEquals(3, out.size());
		assertThat(out.get(0),
				not(equalTo(out.get(1))));
		assertThat(out.get(0), anyOf(
				equalTo("foo/baz.txt"), equalTo("foo/qux.txt"), equalTo("foo/" + dir1.getName() + "/" + file3.getName())));
		assertThat(out.get(1), anyOf(
				equalTo("foo/baz.txt"), equalTo("foo/qux.txt"), equalTo("foo/" + dir1.getName() + "/" + file3.getName())));
		assertThat(out.get(2), anyOf(
				equalTo("foo/baz.txt"), equalTo("foo/qux.txt"), equalTo("foo/" + dir1.getName() + "/" + file3.getName())));
	}

}

abstract class TestSession implements org.springframework.integration.file.remote.session.Session<TestLsEntry> {

	private boolean open;


	@Override
	public boolean remove(String path) throws IOException {
		return false;
	}

	@Override
	public TestLsEntry[] list(String path) throws IOException {
		return null;
	}

	@Override
	public void read(String source, OutputStream outputStream)
			throws IOException {
	}

	@Override
	public void write(InputStream inputStream, String destination)
			throws IOException {
	}

	@Override
	public void append(InputStream inputStream, String destination)
			throws IOException {
	}

	@Override
	public boolean mkdir(String directory) throws IOException {
		return true;
	}

	@Override
	public boolean rmdir(String directory) throws IOException {
		return true;
	}

	@Override
	public void rename(String pathFrom, String pathTo)
			throws IOException {
	}

	@Override
	public void close() {
		open = false;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public boolean exists(String path) throws IOException {
		return true;
	}

	@Override
	public String[] listNames(String path) throws IOException {
		return null;
	}

	@Override
	public InputStream readRaw(String source) throws IOException {
		return null;
	}

	@Override
	public boolean finalizeRaw() throws IOException {
		return false;
	}

	@Override
	public Object getClientInstance() {
		return null;
	}

}

class TestRemoteFileOutboundGateway extends AbstractRemoteFileOutboundGateway<TestLsEntry> {

	@SuppressWarnings({"rawtypes", "unchecked"})
	public TestRemoteFileOutboundGateway(SessionFactory sessionFactory,
										 String command, String expression) {
		super(sessionFactory, Command.toCommand(command), expression);
		this.setBeanFactory(mock(BeanFactory.class));
	}

	public TestRemoteFileOutboundGateway(RemoteFileTemplate<TestLsEntry> remoteFileTemplate, String command,
			String expression) {
		super(remoteFileTemplate, command, expression);
		this.setBeanFactory(mock(BeanFactory.class));
	}


	@Override
	protected boolean isDirectory(TestLsEntry file) {
		return file.isDirectory();
	}

	@Override
	protected boolean isLink(TestLsEntry file) {
		return file.isLink();
	}

	@Override
	protected String getFilename(TestLsEntry file) {
		return file.getFilename();
	}

	@Override
	protected String getFilename(AbstractFileInfo<TestLsEntry> file) {
		return file.getFilename();
	}

	@Override
	protected long getModified(TestLsEntry file) {
		return file.getModified();
	}

	@Override
	protected List<AbstractFileInfo<TestLsEntry>> asFileInfoList(
			Collection<TestLsEntry> files) {
		return new ArrayList<AbstractFileInfo<TestLsEntry>>(files);
	}

	@Override
	protected TestLsEntry enhanceNameWithSubDirectory(TestLsEntry file, String directory) {
		file.setFilename(directory + file.getFilename());
		return file;
	}

}

class TestLsEntry extends AbstractFileInfo<TestLsEntry> {

	private volatile String filename;
	private final long size;
	private final boolean dir;
	private final boolean link;
	private final long modified;
	private final String permissions;

	public TestLsEntry(String filename, long size, boolean dir, boolean link,
					   long modified, String permissions) {
		this.filename = filename;
		this.size = size;
		this.dir = dir;
		this.link = link;
		this.modified = modified;
		this.permissions = permissions;
	}

	@Override
	public boolean isDirectory() {
		return this.dir;
	}

	@Override
	public long getModified() {
		return this.modified;
	}

	@Override
	public String getFilename() {
		return this.filename;
	}

	@Override
	public boolean isLink() {
		return this.link;
	}

	@Override
	public long getSize() {
		return this.size;
	}

	@Override
	public String getPermissions() {
		return this.permissions;
	}

	@Override
	public TestLsEntry getFileInfo() {
		return this;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}

class TestPatternFilter extends AbstractSimplePatternFileListFilter<TestLsEntry> {

	public TestPatternFilter(String path) {
		super(path);
	}

	@Override
	protected String getFilename(TestLsEntry file) {
		return file.getFilename();
	}

}
