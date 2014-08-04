/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.outbound;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.sftp.TestSftpServer;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Runs against an embedded SFTP Server with the following directory tree:
 *
 * <pre class="code">
 *  $ tree sftpSource/
 *  sftpSource/
 *  ├── sftpSource1.txt - contains 'source1'
 *  ├── sftpSource2.txt - contains 'source2'
 *  └── subSftpSource
 *      └── subSftpSource1.txt - contains 'subSource1'
 * </pre>
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SftpServerOutboundTests {

	@Autowired
	private PollableChannel output;

	@Autowired
	private DirectChannel inboundGet;

	@Autowired
	private DirectChannel invalidDirExpression;

	@Autowired
	private DirectChannel inboundMGet;

	@Autowired
	private DirectChannel inboundMGetRecursive;

	@Autowired
	private DirectChannel inboundMGetRecursiveFiltered;

	@Autowired
	private DirectChannel inboundMPut;

	@Autowired
	private DirectChannel inboundMPutRecursive;

	@Autowired
	private DirectChannel inboundMPutRecursiveFiltered;

	@Autowired
	private DefaultSftpSessionFactory sessionFactory;

	@Autowired
	private DirectChannel appending;

	@Autowired
	private DirectChannel ignoring;

	@Autowired
	private DirectChannel failing;

	@Autowired
	private TestSftpServer sftpServer;

	@Before
	@After
	public void setup() {
		this.sftpServer.recursiveDelete(sftpServer.getTargetLocalDirectory());
		this.sftpServer.recursiveDelete(sftpServer.getTargetSftpDirectory());
	}

	@Test
	public void testInt2866LocalDirectoryExpressionGET() {
		Session<?> session = this.sessionFactory.getSession();
		String dir = "sftpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "sftpSource1.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		File localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir.toUpperCase()));

		dir = "sftpSource/subSftpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "subSftpSource1.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir.toUpperCase()));
		Session<?> session2 = this.sessionFactory.getSession();
		assertSame(TestUtils.getPropertyValue(session, "jschSession"),
				TestUtils.getPropertyValue(session2, "jschSession"));
	}

	@Test
	public void testInt2866InvalidLocalDirectoryExpression() {
		try {
			this.invalidDirExpression.send(new GenericMessage<Object>("sftpSource/sftpSource1.txt"));
			fail("Exception expected.");
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertNotNull(cause);
			cause = cause.getCause();
			assertThat(cause, Matchers.instanceOf(IllegalArgumentException.class));
			assertThat(cause.getMessage(), Matchers.startsWith("Failed to make local directory"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt2866LocalDirectoryExpressionMGET() {
		String dir = "sftpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}

		dir = "sftpSource/subSftpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		localFiles = (List<File>) result.getPayload();

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3172LocalDirectoryExpressionMGETRecursive() {
		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();
		assertEquals(3, localFiles.size());

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
		assertThat(localFiles.get(2).getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir + "subSftpSource"));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3172LocalDirectoryExpressionMGETRecursiveFiltered() {
		String dir = "sftpSource/";
		this.inboundMGetRecursiveFiltered.send(new GenericMessage<Object>(dir + "*"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();
		// should have filtered sftpSource2.txt
		assertEquals(2, localFiles.size());

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
		assertThat(localFiles.get(1).getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir + "subSftpSource"));

	}

	/**
	 * Only runs with a real server (see class javadocs).
	 */
	@Test
	public void testInt3100RawGET() throws Exception {
		Session<?> session = this.sessionFactory.getSession();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("sftpSource/sftpSource1.txt"), baos);
		assertTrue(session.finalizeRaw());
		assertEquals("source1", new String(baos.toByteArray()));

		baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("sftpSource/sftpSource2.txt"), baos);
		assertTrue(session.finalizeRaw());
		assertEquals("source2", new String(baos.toByteArray()));

		session.close();
	}

	@Test
	public void testInt3047ConcurrentSharedSession() throws Exception {
		final Session<?> session1 = this.sessionFactory.getSession();
		final Session<?> session2 = this.sessionFactory.getSession();
		final PipedInputStream pipe1 = new PipedInputStream();
		PipedOutputStream out1 = new PipedOutputStream(pipe1);
		final PipedInputStream pipe2 = new PipedInputStream();
		PipedOutputStream out2 = new PipedOutputStream(pipe2);
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					session1.write(pipe1, "foo.txt");
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				latch1.countDown();
			}
		});
		Executors.newSingleThreadExecutor().execute(new Runnable() {

			@Override
			public void run() {
				try {
					session2.write(pipe2, "bar.txt");
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				latch2.countDown();
			}
		});

		out1.write('a');
		out2.write('b');
		out1.write('c');
		out2.write('d');
		out1.write('e');
		out2.write('f');
		out1.close();
		out2.close();
		assertTrue(latch1.await(10, TimeUnit.SECONDS));
		assertTrue(latch2.await(10, TimeUnit.SECONDS));
		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		session1.read("foo.txt", bos1);
		session2.read("bar.txt", bos2);
		assertEquals("ace", new String(bos1.toByteArray()));
		assertEquals("bdf", new String(bos2.toByteArray()));
		session1.remove("foo.txt");
		session2.remove("bar.txt");
		session1.close();
		session2.close();
	}

	@Test
	public void testInt3088MPutNotRecursive() {
		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		while (output.receive(0) != null) { }
		this.inboundMPut.send(new GenericMessage<File>(this.sftpServer.getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertNotNull(out);
		assertEquals(2, out.getPayload().size());
		assertThat(out.getPayload().get(0),
				not(equalTo(out.getPayload().get(1))));
		assertThat(
				out.getPayload().get(0),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt")));
		assertThat(
				out.getPayload().get(1),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt")));
	}

	@Test
	public void testInt3088MPutRecursive() {
		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		while (output.receive(0) != null) { }
		this.inboundMPutRecursive.send(new GenericMessage<File>(this.sftpServer.getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertNotNull(out);
		assertEquals(3, out.getPayload().size());
		assertThat(out.getPayload().get(0),
				not(equalTo(out.getPayload().get(1))));
		assertThat(
				out.getPayload().get(0),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt"),
						equalTo("sftpTarget/subLocalSource/subLocalSource1.txt")));
		assertThat(
				out.getPayload().get(1),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt"),
						equalTo("sftpTarget/subLocalSource/subLocalSource1.txt")));
		assertThat(
				out.getPayload().get(2),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt"),
						equalTo("sftpTarget/subLocalSource/subLocalSource1.txt")));
	}

	@Test
	public void testInt3088MPutRecursiveFiltered() {
		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		while (output.receive(0) != null) { }
		this.inboundMPutRecursiveFiltered.send(new GenericMessage<File>(this.sftpServer.getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertNotNull(out);
		assertEquals(2, out.getPayload().size());
		assertThat(out.getPayload().get(0),
				not(equalTo(out.getPayload().get(1))));
		assertThat(
				out.getPayload().get(0),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt"),
						equalTo("sftpTarget/subLocalSource/subLocalSource1.txt")));
		assertThat(
				out.getPayload().get(1),
				anyOf(equalTo("sftpTarget/localSource1.txt"), equalTo("sftpTarget/localSource2.txt"),
						equalTo("sftpTarget/subLocalSource/subLocalSource1.txt")));
	}

	@Test
	public void testInt3412FileMode() {
		Message<String> m = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.FILENAME, "appending.txt")
				.build();
		appending.send(m);
		appending.send(m);

		SftpRemoteFileTemplate template = new SftpRemoteFileTemplate(sessionFactory);
		assertLength6(template);

		ignoring.send(m);
		assertLength6(template);
		try {
			failing.send(m);
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getCause().getCause().getMessage(), containsString("The destination file already exists"));
		}

	}

	private void assertLength6(SftpRemoteFileTemplate template) {
		LsEntry[] files = template.execute(new SessionCallback<LsEntry, LsEntry[]>() {

			@Override
			public LsEntry[] doInSession(Session<LsEntry> session) throws IOException {
				return session.list("sftpTarget/appending.txt");
			}
		});
		assertEquals(1, files.length);
		assertEquals(6, files[0].getAttrs().getSize());
	}

}
