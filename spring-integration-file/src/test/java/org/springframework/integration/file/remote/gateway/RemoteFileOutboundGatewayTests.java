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

package org.springframework.integration.file.remote.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;


/**
 * @author Gary Russell
 * @author Liu Jiong
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SuppressWarnings("rawtypes")
public class RemoteFileOutboundGatewayTests {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final String tmpDir = System.getProperty("java.io.tmpdir");

	@Rule
	public final TemporaryFolder tempFolder = new TemporaryFolder();


	@Test
	public void testBad() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new TestRemoteFileOutboundGateway(sessionFactory, "bad", "payload"));
	}

	@Test
	public void testBadFilterGet() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "get", "payload");
		gw.setFilter(new TestPatternFilter(""));

		assertThatIllegalArgumentException()
				.isThrownBy(gw::afterPropertiesSet)
				.withMessageStartingWith("Filters are not supported");
	}

	@Test
	public void testBadFilterRm() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "rm", "payload");
		gw.setFilter(new TestPatternFilter(""));

		assertThatIllegalArgumentException()
				.isThrownBy(gw::afterPropertiesSet)
				.withMessageStartingWith("Filters are not supported");
	}

	@Test
	public void testLs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/x/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<TestLsEntry>> out = (MessageBuilder<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/x"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0)).isSameAs(files[1]); // sort by default
		assertThat(out.getPayload().get(1)).isSameAs(files[0]);
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/x/");
	}

	@Test
	public void testMGetWild() {
		testMGetWildGuts("f1", "f2");
	}

	@Test
	public void testMGetWildFullPath() {
		testMGetWildGuts("testremote/f1", "testremote/f2");
	}

	private void testMGetWildGuts(final String path1, final String path2) {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mget", "payload");
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
					assertThat(source).isEqualTo("testremote/f1");
				}
				else {
					assertThat(source).isEqualTo("testremote/f2");
				}
				outputStream.write("testData".getBytes());
			}

			@Override
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] {
						new TestLsEntry(path1.replaceFirst("testremote/", ""), 123, false, false, 1234, "-r--r--r--"),
						new TestLsEntry(path2.replaceFirst("testremote/", ""), 123, false, false, 1234,
								"-r--r--r--") };
			}

		});
		@SuppressWarnings("unchecked")
		MessageBuilder<List<File>> out = (MessageBuilder<List<File>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/*"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0).getName()).isEqualTo("f1");
		assertThat(out.getPayload().get(1).getName()).isEqualTo("f2");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/");
	}

	@Test
	public void testMGetSingle() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mget", "payload");
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
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] { new TestLsEntry("f1", 123, false, false, 1234, "-r--r--r--") };
			}

		});
		@SuppressWarnings("unchecked")
		MessageBuilder<List<File>> out = (MessageBuilder<List<File>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/f1"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(1);
		assertThat(out.getPayload().get(0).getName()).isEqualTo("f1");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/");
	}

	@Test(expected = MessagingException.class)
	public void testMGetEmpty() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mget", "payload");
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
		gw.handleRequestMessage(new GenericMessage<>("testremote/*"));
	}

	@Test
	public void testMove() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mv", "payload");
		gw.afterPropertiesSet();
		Session<?> session = mock(Session.class);
		final AtomicReference<String> args = new AtomicReference<>();
		doAnswer(invocation -> {
			Object[] arguments = invocation.getArguments();
			args.set((String) arguments[0] + arguments[1]);
			return null;
		}).when(session).rename(anyString(), anyString());
		when(sessionFactory.getSession()).thenReturn(session);
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.RENAME_TO, "bar")
				.build();
		MessageBuilder<?> out = (MessageBuilder<?>) gw.handleRequestMessage(requestMessage);
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");
		assertThat(args.get()).isEqualTo("foobar");
		assertThat(out.getPayload()).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void testMoveWithExpression() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mv", "payload");
		gw.setRenameExpression(PARSER.parseExpression("payload.substring(1)"));
		gw.afterPropertiesSet();
		Session<?> session = mock(Session.class);
		final AtomicReference<String> args = new AtomicReference<>();
		doAnswer(invocation -> {
			Object[] arguments = invocation.getArguments();
			args.set((String) arguments[0] + arguments[1]);
			return null;
		}).when(session).rename(anyString(), anyString());
		when(sessionFactory.getSession()).thenReturn(session);
		MessageBuilder<?> out = (MessageBuilder<?>) gw.handleRequestMessage(new GenericMessage<>("foo"));
		assertThat(out.getHeaders().get(FileHeaders.RENAME_TO)).isEqualTo("oo");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");
		assertThat(args.get()).isEqualTo("foooo");
		assertThat(out.getPayload()).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void testMoveWithMkDirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mv", "payload");
		gw.setRenameExpression(PARSER.parseExpression("'foo/bar/baz'"));
		gw.afterPropertiesSet();
		Session<?> session = mock(Session.class);
		final AtomicReference<String> args = new AtomicReference<>();
		doAnswer(invocation -> {
			Object[] arguments = invocation.getArguments();
			args.set((String) arguments[0] + arguments[1]);
			return null;
		}).when(session).rename(anyString(), anyString());
		final List<String> madeDirs = new ArrayList<>();
		doAnswer(invocation -> {
			madeDirs.add(invocation.getArgument(0));
			return null;
		}).when(session).mkdir(anyString());
		when(sessionFactory.getSession()).thenReturn(session);
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.RENAME_TO, "bar")
				.build();
		MessageBuilder<?> out = (MessageBuilder<?>) gw.handleRequestMessage(requestMessage);
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("foo");
		assertThat(args.get()).isEqualTo("foofoo/bar/baz");
		assertThat(out.getPayload()).isEqualTo(Boolean.TRUE);
		assertThat(madeDirs.size()).isEqualTo(2);
		assertThat(madeDirs.get(0)).isEqualTo("foo");
		assertThat(madeDirs.get(1)).isEqualTo("foo/bar");
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
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-f");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/x/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<TestLsEntry>> out = (MessageBuilder<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/x"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0)).isSameAs(files[0]);
		assertThat(out.getPayload().get(1)).isSameAs(files[1]);
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/x/");
	}

	public TestLsEntry[] level1List() {
		return new TestLsEntry[] {
				new TestLsEntry("f1", 123, false, false, 1234, "-r--r--r--"),
				new TestLsEntry("d1", 0, true, false, 12345, "drw-r--r--"),
				new TestLsEntry("f2", 12345, false, false, 123456, "-rw-r--r--")
		};
	}

	public TestLsEntry[] level2List() {
		return new TestLsEntry[] {
				new TestLsEntry("d2", 0, true, false, 12345, "drw-r--r--"),
				new TestLsEntry("f3", 12345, false, false, 123456, "-rw-r--r--")
		};
	}

	public TestLsEntry[] level3List() {
		return new TestLsEntry[] {
				new TestLsEntry("f4", 12345, false, false, 123456, "-rw-r--r--")
		};
	}

	@Test
	public void testLs_f_R() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
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
		MessageBuilder<List<TestLsEntry>> out = (MessageBuilder<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/x"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(4);
		assertThat(out.getPayload().get(0).getFilename()).isEqualTo("f1");
		assertThat(out.getPayload().get(1).getFilename()).isEqualTo("d1/d2/f4");
		assertThat(out.getPayload().get(2).getFilename()).isEqualTo("d1/f3");
		assertThat(out.getPayload().get(3).getFilename()).isEqualTo("f2");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/x/");
	}

	@Test
	public void testLs_f_R_dirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
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
		MessageBuilder<List<TestLsEntry>> out = (MessageBuilder<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/x"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(5);
		assertThat(out.getPayload().get(0).getFilename()).isEqualTo("f1");
		assertThat(out.getPayload().get(1).getFilename()).isEqualTo("d1");
		assertThat(out.getPayload().get(2).getFilename()).isEqualTo("d1/d2");
		assertThat(out.getPayload().get(3).getFilename()).isEqualTo("d1/f3");
		assertThat(out.getPayload().get(4).getFilename()).isEqualTo("f2");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/x/");
	}

	@Test
	public void testLs_None() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = new TestLsEntry[0];
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<TestLsEntry>> out = (MessageBuilder<List<TestLsEntry>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(0);
	}

	@Test
	public void testLs_1() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-1");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<String>> out = (MessageBuilder<List<String>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0)).isEqualTo("f1");
		assertThat(out.getPayload().get(1)).isEqualTo("f2");
	}

	@Test
	public void testLs_1_f() throws Exception { //no sort
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -f");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<String>> out = (MessageBuilder<List<String>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0)).isEqualTo("f2");
		assertThat(out.getPayload().get(1)).isEqualTo("f1");
	}

	@Test
	public void testLs_1_dirs() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -dirs");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<String>> out = (MessageBuilder<List<String>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out.getPayload()).hasSize(3);
		assertThat(out.getPayload().get(0)).isEqualTo("f1");
		assertThat(out.getPayload().get(1)).isEqualTo("f2");
		assertThat(out.getPayload().get(2)).isEqualTo("f3");
	}

	@Test
	public void testLs_1_dirs_links() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -dirs -links");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<String>> out = (MessageBuilder<List<String>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out.getPayload()).hasSize(4);
		assertThat(out.getPayload().get(0)).isEqualTo("f1");
		assertThat(out.getPayload().get(1)).isEqualTo("f2");
		assertThat(out.getPayload().get(2)).isEqualTo("f3");
		assertThat(out.getPayload().get(3)).isEqualTo("f4");
	}

	@Test
	public void testLs_1_a_f_dirs_links() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -a -f -dirs -links");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<String>> out = (MessageBuilder<List<String>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out.getPayload()).hasSize(6);
		assertThat(out.getPayload().get(0)).isEqualTo("f2");
		assertThat(out.getPayload().get(1)).isEqualTo("f1");
		assertThat(out.getPayload().get(2)).isEqualTo("f3");
		assertThat(out.getPayload().get(3)).isEqualTo("f4");
		assertThat(out.getPayload().get(4)).isEqualTo(".f5");
		assertThat(out.getPayload().get(5)).isEqualTo(".f6");
	}

	@Test
	public void testLs_1_a_f_dirs_links_filtered() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "ls", "payload");
		gw.setOptions("-1 -a -f -dirs -links");
		gw.setFilter(new TestPatternFilter("*4"));
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		TestLsEntry[] files = fileList();
		when(session.list("testremote/")).thenReturn(files);
		@SuppressWarnings("unchecked")
		MessageBuilder<List<String>> out = (MessageBuilder<List<String>>) gw
				.handleRequestMessage(new GenericMessage<>("testremote"));
		assertThat(out.getPayload()).hasSize(1);
		assertThat(out.getPayload().get(0)).isEqualTo("f4");
	}

	@Test
	public void testGet() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] {
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
		MessageBuilder<File> out = (MessageBuilder<File>) gw.handleRequestMessage(new GenericMessage<>("f1"));
		File outFile = new File(this.tmpDir + "/f1");
		assertThat(out.getPayload()).isEqualTo(outFile);
		assertThat(outFile.exists()).isTrue();
		outFile.delete();
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isNull();
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("f1");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetExists() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		File outFile = new File(this.tmpDir + "/f1");
		FileOutputStream fos = new FileOutputStream(outFile);
		fos.write("foo".getBytes());
		fos.close();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] {
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream)
					throws IOException {
				outputStream.write("testfile".getBytes());
			}

		});

		// default (null)
		MessageBuilder<File> out;

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> gw.handleRequestMessage(new GenericMessage<>("f1")))
				.withMessageContaining("already exists");

		gw.setFileExistsMode(FileExistsMode.FAIL);

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> gw.handleRequestMessage(new GenericMessage<>("f1")))
				.withMessageContaining("already exists");

		gw.setFileExistsMode(FileExistsMode.IGNORE);
		out = (MessageBuilder<File>) gw.handleRequestMessage(new GenericMessage<>("f1"));
		assertThat(out.getPayload()).isEqualTo(outFile);
		assertContents("foo", outFile);

		gw.setFileExistsMode(FileExistsMode.APPEND);
		out = (MessageBuilder<File>) gw.handleRequestMessage(new GenericMessage<>("f1"));
		assertThat(out.getPayload()).isEqualTo(outFile);
		assertContents("footestfile", outFile);

		gw.setFileExistsMode(FileExistsMode.REPLACE);
		out = (MessageBuilder<File>) gw.handleRequestMessage(new GenericMessage<>("f1"));
		assertThat(out.getPayload()).isEqualTo(outFile);
		assertContents("testfile", outFile);

		outFile.delete();
	}

	private void assertContents(String expected, File outFile) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(outFile));
		assertThat(reader.readLine()).isEqualTo(expected);
		reader.close();
	}

	@Test
	public void testGetTempFileDelete() {
		@SuppressWarnings("unchecked")
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] {
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream) {
				throw new RuntimeException("test remove .writing");
			}

		});

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> gw.handleRequestMessage(new GenericMessage<>("f1")))
				.withCauseInstanceOf(RuntimeException.class)
				.withMessageContaining("test remove .writing");

		RemoteFileTemplate<?> template = new RemoteFileTemplate<>(sessionFactory);
		File outFile = new File(this.tmpDir + "/f1" + template.getTemporaryFileSuffix());
		assertThat(outFile.exists()).isFalse();
	}


	@Test
	public void testGet_P() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir));
		gw.setOptions("-P");
		gw.afterPropertiesSet();
		new File(this.tmpDir + "/f1").delete();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		final Date modified = new Date(cal.getTime().getTime() / 1000 * 1000);
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] {
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
		MessageBuilder<File> out = (MessageBuilder<File>) gw.handleRequestMessage(new GenericMessage<>("x/f1"));
		File outFile = new File(this.tmpDir + "/f1");
		assertThat(out.getPayload()).isEqualTo(outFile);
		assertThat(outFile.exists()).isTrue();
		assertThat(outFile.lastModified()).isEqualTo(modified.getTime());
		outFile.delete();
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("x/");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("f1");
	}

	@Test
	public void testGet_create_dir() {
		new File(this.tmpDir + "/x/f1").delete();
		new File(this.tmpDir + "/x").delete();
		SessionFactory sessionFactory = mock(SessionFactory.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "get", "payload");
		gw.setLocalDirectory(new File(this.tmpDir + "/x"));
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(new TestSession() {

			@Override
			public TestLsEntry[] list(String path) {
				return new TestLsEntry[] {
						new TestLsEntry("f1", 1234, false, false, 12345, "-rw-r--r--")
				};
			}

			@Override
			public void read(String source, OutputStream outputStream) throws IOException {
				outputStream.write("testfile".getBytes());
			}

		});
		gw.handleRequestMessage(new GenericMessage<>("f1"));
		File out = new File(this.tmpDir + "/x/f1");
		assertThat(out.exists()).isTrue();
		out.delete();
	}

	@Test
	public void testRm() throws Exception {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "rm", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		when(session.remove("testremote/x/f1")).thenReturn(Boolean.TRUE);
		@SuppressWarnings("unchecked")
		MessageBuilder<Boolean> out = (MessageBuilder<Boolean>) gw
				.handleRequestMessage(new GenericMessage<>("testremote/x/f1"));
		assertThat(out.getPayload()).isTrue();
		verify(session).remove("testremote/x/f1");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_DIRECTORY)).isEqualTo("testremote/x/");
		assertThat(out.getHeaders().get(FileHeaders.REMOTE_FILE)).isEqualTo("f1");
	}

	@Test
	public void testPut() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestLsEntry> session = mock(Session.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<TestLsEntry>(sessionFactory) {

			@Override
			public boolean exists(String path) {
				return false;
			}

		};
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "put", "payload");
		FileTransferringMessageHandler<TestLsEntry> handler = new FileTransferringMessageHandler<>(sessionFactory);
		handler.setRemoteDirectoryExpressionString("'foo/'");
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		Message<String> requestMessage = MessageBuilder.withPayload("hello")
				.setHeader(FileHeaders.FILENAME, "bar.txt")
				.build();
		String path = (String) gw.handleRequestMessage(requestMessage);
		assertThat(path).isEqualTo("foo/bar.txt");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(session).write(any(InputStream.class), captor.capture());
		assertThat(captor.getValue()).isEqualTo("foo/bar.txt.writing");
		verify(session).rename("foo/bar.txt.writing", "foo/bar.txt");
	}

	@Test
	public void testPutExists() throws Exception {
		@SuppressWarnings("unchecked")
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		@SuppressWarnings("unchecked")
		Session<TestLsEntry> session = mock(Session.class);
		willReturn(Boolean.TRUE)
				.given(session)
				.exists(anyString());
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<>(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "put", "payload");
		FileTransferringMessageHandler<TestLsEntry> handler = new FileTransferringMessageHandler<>(sessionFactory);
		handler.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		Message<String> requestMessage = MessageBuilder.withPayload("hello")
				.setHeader(FileHeaders.FILENAME, "bar.txt")
				.build();

		// default (null) == REPLACE
		String path = (String) gw.handleRequestMessage(requestMessage);
		assertThat(path).isEqualTo("foo/bar.txt");
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(session).write(any(InputStream.class), captor.capture());
		assertThat(captor.getValue()).isEqualTo("foo/bar.txt.writing");
		verify(session).rename("foo/bar.txt.writing", "foo/bar.txt");

		gw.setFileExistsMode(FileExistsMode.FAIL);

		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> gw.handleRequestMessage(requestMessage))
				.withMessageContaining("The destination file already exists");

		gw.setFileExistsMode(FileExistsMode.REPLACE);
		path = (String) gw.handleRequestMessage(requestMessage);
		assertThat(path).isEqualTo("foo/bar.txt");
		captor = ArgumentCaptor.forClass(String.class);
		verify(session, times(2)).write(any(InputStream.class), captor.capture());
		assertThat(captor.getValue()).isEqualTo("foo/bar.txt.writing");
		verify(session, times(2)).rename("foo/bar.txt.writing", "foo/bar.txt");

		gw.setFileExistsMode(FileExistsMode.APPEND);
		path = (String) gw.handleRequestMessage(requestMessage);
		assertThat(path).isEqualTo("foo/bar.txt");
		captor = ArgumentCaptor.forClass(String.class);
		verify(session).append(any(InputStream.class), captor.capture());
		assertThat(captor.getValue()).isEqualTo("foo/bar.txt");

		gw.setFileExistsMode(FileExistsMode.IGNORE);
		path = (String) gw.handleRequestMessage(requestMessage);
		assertThat(path).isEqualTo("foo/bar.txt");
		// no more writes/appends
		verify(session, times(2)).write(any(InputStream.class), anyString());
		verify(session, times(1)).append(any(InputStream.class), anyString());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMput() throws Exception {
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		Session<TestLsEntry> session = mock(Session.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<>(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "mput", "payload");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		final AtomicReference<String> written = new AtomicReference<>();
		doAnswer(invocation -> {
			written.set(invocation.getArgument(1));
			return null;
		}).when(session).write(any(InputStream.class), anyString());
		tempFolder.newFile("baz.txt");
		tempFolder.newFile("qux.txt");
		Message<File> requestMessage = MessageBuilder.withPayload(tempFolder.getRoot())
				.build();
		List<String> out = (List<String>) gw.handleRequestMessage(requestMessage);
		assertThat(out).hasSize(2);
		assertThat(out.get(0)).isNotEqualTo(out.get(1));
		assertThat(out.get(0)).isIn("foo/baz.txt", "foo/qux.txt");
		assertThat(out.get(1)).isIn("foo/baz.txt", "foo/qux.txt");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMputRecursive() throws Exception {
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		Session<TestLsEntry> session = mock(Session.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<>(sessionFactory);
		template.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "mput", null);
		gw.setOptions("-R");
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		final AtomicReference<String> written = new AtomicReference<>();
		doAnswer(invocation -> {
			written.set(invocation.getArgument(1));
			return null;
		}).when(session).write(any(InputStream.class), anyString());
		tempFolder.newFile("baz.txt");
		tempFolder.newFile("qux.txt");
		File dir1 = tempFolder.newFolder();
		File file3 = File.createTempFile("foo", ".txt", dir1);

		Message<File> requestMessage = MessageBuilder.withPayload(tempFolder.getRoot())
				.build();
		List<String> out = (List<String>) gw.handleRequestMessage(requestMessage);
		assertThat(out).hasSize(3);
		assertThat(out.get(0)).isNotEqualTo(out.get(1));
		assertThat(out.get(0)).isIn("foo/baz.txt", "foo/qux.txt", "foo/" + dir1.getName() + "/" + file3.getName());
		assertThat(out.get(1)).isIn("foo/baz.txt", "foo/qux.txt", "foo/" + dir1.getName() + "/" + file3.getName());
		assertThat(out.get(2)).isIn("foo/baz.txt", "foo/qux.txt", "foo/" + dir1.getName() + "/" + file3.getName());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRemoteFileTemplateImmutability() {
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		RemoteFileTemplate<TestLsEntry> template = new RemoteFileTemplate<>(sessionFactory);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(template, "mput", null);
		assertThatIllegalStateException()
				.isThrownBy(() -> gw.setRemoteDirectoryExpression(new LiteralExpression("testRemoteDirectory")))
				.withMessageContaining("The 'remoteDirectoryExpression' must be set on the externally provided");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMputCollection() throws Exception {
		SessionFactory<TestLsEntry> sessionFactory = mock(SessionFactory.class);
		Session<TestLsEntry> session = mock(Session.class);
		TestRemoteFileOutboundGateway gw = new TestRemoteFileOutboundGateway(sessionFactory, "mput", "payload");
		gw.setRemoteDirectoryExpression(new LiteralExpression("foo/"));
		gw.setBeanFactory(mock(BeanFactory.class));
		gw.afterPropertiesSet();
		when(sessionFactory.getSession()).thenReturn(session);
		final AtomicReference<String> written = new AtomicReference<>();
		doAnswer(invocation -> {
			written.set(invocation.getArgument(1));
			return null;
		}).when(session).write(any(InputStream.class), anyString());
		List<File> files = new ArrayList<>();
		files.add(tempFolder.newFile("fiz.txt"));
		files.add(tempFolder.newFile("buz.txt"));
		Message<List<File>> requestMessage = MessageBuilder.withPayload(files)
				.build();
		List<String> out = (List<String>) gw.handleRequestMessage(requestMessage);
		assertThat(out).isNotNull().hasSize(2);
		assertThat(out.get(0)).isNotEqualTo(out.get(1));
		assertThat(out.get(0)).isEqualTo("foo/fiz.txt");
		assertThat(out.get(1)).isEqualTo("foo/buz.txt");
		assertThat(written.get()).isEqualTo("foo/buz.txt.writing");
		verify(session).rename("foo/buz.txt.writing", "foo/buz.txt");
	}

	abstract static class TestSession implements Session<TestLsEntry> {

		private boolean open;


		@Override
		public boolean remove(String path) {
			return false;
		}

		@Override
		public TestLsEntry[] list(String path) {
			return null;
		}

		@Override
		public void read(String source, OutputStream outputStream)
				throws IOException {
		}

		@Override
		public void write(InputStream inputStream, String destination) {
		}

		@Override
		public void append(InputStream inputStream, String destination) {
		}

		@Override
		public boolean mkdir(String directory) {
			return true;
		}

		@Override
		public boolean rmdir(String directory) {
			return true;
		}

		@Override
		public void rename(String pathFrom, String pathTo) {
		}

		@Override
		public void close() {
			open = false;
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}

		@Override
		public boolean exists(String path) {
			return true;
		}

		@Override
		public String[] listNames(String path) {
			return null;
		}

		@Override
		public InputStream readRaw(String source) {
			return null;
		}

		@Override
		public boolean finalizeRaw() {
			return false;
		}

		@Override
		public Object getClientInstance() {
			return null;
		}

		@Override
		public String getHostPort() {
			return null;
		}

	}


	static class TestRemoteFileOutboundGateway extends AbstractRemoteFileOutboundGateway<TestLsEntry> {

		@SuppressWarnings("unchecked")
		TestRemoteFileOutboundGateway(SessionFactory sessionFactory,
				String command, String expression) {
			super(sessionFactory, Command.toCommand(command), expression);
			this.setBeanFactory(mock(BeanFactory.class));
			remoteFileTemplateExplicitlySet(false);
		}

		TestRemoteFileOutboundGateway(RemoteFileTemplate<TestLsEntry> remoteFileTemplate, String command,
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

			return new ArrayList<>(files);
		}

		@Override
		protected TestLsEntry enhanceNameWithSubDirectory(TestLsEntry file, String directory) {
			file.setFilename(directory + file.getFilename());
			return file;
		}

	}

	static class TestLsEntry extends AbstractFileInfo<TestLsEntry> {

		private volatile String filename;

		private final long size;

		private final boolean dir;

		private final boolean link;

		private final long modified;

		private final String permissions;

		TestLsEntry(String filename, long size, boolean dir, boolean link,
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

	static class TestPatternFilter extends AbstractSimplePatternFileListFilter<TestLsEntry> {

		TestPatternFilter(String path) {
			super(path);
		}

		@Override
		protected String getFilename(TestLsEntry file) {
			return file.getFilename();
		}

		@Override
		protected boolean isDirectory(TestLsEntry file) {
			return file.isDirectory();
		}

	}

}
