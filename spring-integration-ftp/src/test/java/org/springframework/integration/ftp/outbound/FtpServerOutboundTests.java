/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.ftp.outbound;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.InputStreamCallback;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.PartialSuccessException;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class FtpServerOutboundTests extends FtpTestSupport {

	@Autowired
	private SessionFactory<FTPFile> ftpSessionFactory;

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
	private DirectChannel appending;

	@Autowired
	private DirectChannel ignoring;

	@Autowired
	private DirectChannel failing;

	@Autowired
	private DirectChannel inboundGetStream;

	@Autowired
	private DirectChannel inboundCallback;

	@Autowired
	private DirectChannel inboundLs;

	@Autowired
	private SourcePollingChannelAdapter ftpInbound;

	@Autowired
	private Config config;

	@Before
	public void setup() {
		this.config.targetLocalDirectoryName = getTargetLocalDirectoryName();
	}

	@Test
	public void testInt2866LocalDirectoryExpressionGET() {
		String dir = "ftpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + " ftpSource1.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		File localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir.toUpperCase()));

		dir = "ftpSource/subFtpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "subFtpSource1.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir.toUpperCase()));
	}

	@Test
	public void testInt2866InvalidLocalDirectoryExpression() {
		try {
			this.invalidDirExpression.send(new GenericMessage<Object>("/ftpSource/ ftpSource1.txt"));
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
		String dir = "ftpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();

		assertThat(localFiles.size(), Matchers.greaterThan(0));

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}

		dir = "ftpSource/subFtpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		localFiles = (List<File>) result.getPayload();

		assertThat(localFiles.size(), Matchers.greaterThan(0));

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMGETOnNullDir() throws IOException {
		Session<FTPFile> session = ftpSessionFactory.getSession();
		((FTPClient) session.getClientInstance()).changeWorkingDirectory("ftpSource");
		session.close();

		this.inboundMGet.send(new GenericMessage<Object>(""));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();

		assertThat(localFiles.size(), Matchers.greaterThan(0));

		for (File file : localFiles) {
			assertThat(file.getName(), isOneOf(" localTarget1.txt", "localTarget2.txt"));
			assertThat(file.getName(), not(containsString("null")));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3172LocalDirectoryExpressionMGETRecursive() {
		String dir = "ftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>("*"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();
		assertEquals(3, localFiles.size());

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
		assertThat(localFiles.get(2).getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir + "subFtpSource"));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3172LocalDirectoryExpressionMGETRecursiveFiltered() {
		String dir = "ftpSource/";
		this.inboundMGetRecursiveFiltered.send(new GenericMessage<Object>(dir + "*"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();
		// should have filtered ftpSource2.txt
		assertEquals(2, localFiles.size());

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
		assertThat(localFiles.get(1).getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir + "subFtpSource"));

	}

	@Test
	public void testInt3100RawGET() throws Exception {
		Session<?> session = this.ftpSessionFactory.getSession();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("ftpSource/ ftpSource1.txt"), baos);
		assertTrue(session.finalizeRaw());
		assertEquals("source1", new String(baos.toByteArray()));

		baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("ftpSource/ftpSource2.txt"), baos);
		assertTrue(session.finalizeRaw());
		assertEquals("source2", new String(baos.toByteArray()));

		session.close();
	}

	@Test
	public void testRawGETWithTemplate() throws Exception {
		RemoteFileTemplate<FTPFile> template = new RemoteFileTemplate<FTPFile>(this.ftpSessionFactory);
		template.setFileNameExpression(new SpelExpressionParser().parseExpression("payload"));
		template.setBeanFactory(mock(BeanFactory.class));
		template.afterPropertiesSet();
		final ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
		assertTrue(template.get(new GenericMessage<String>("ftpSource/ ftpSource1.txt"), new InputStreamCallback() {

			@Override
			public void doWithInputStream(InputStream stream) throws IOException {
				FileCopyUtils.copy(stream, baos1);
			}
		}));
		assertEquals("source1", new String(baos1.toByteArray()));

		final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
		assertTrue(template.get(new GenericMessage<String>("ftpSource/ftpSource2.txt"), new InputStreamCallback() {

			@Override
			public void doWithInputStream(InputStream stream) throws IOException {
				FileCopyUtils.copy(stream, baos2);
			}
		}));
		assertEquals("source2", new String(baos2.toByteArray()));
	}

	@Test
	public void testInt3088MPutNotRecursive() {
		this.inboundMPut.send(new GenericMessage<File>(getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertNotNull(out);
		assertEquals(2, out.getPayload().size());
		assertThat(out.getPayload().get(0),
				not(equalTo(out.getPayload().get(1))));
		assertThat(
				out.getPayload().get(0),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt")));
		assertThat(
				out.getPayload().get(1),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt")));
	}

	@Test
	public void testInt3088MPutRecursive() {
		this.inboundMPutRecursive.send(new GenericMessage<File>(getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertNotNull(out);
		assertEquals(3, out.getPayload().size());
		assertThat(out.getPayload().get(0),
				not(equalTo(out.getPayload().get(1))));
		assertThat(
				out.getPayload().get(0),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt"),
						equalTo("ftpTarget/subLocalSource/subLocalSource1.txt")));
		assertThat(
				out.getPayload().get(1),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt"),
						equalTo("ftpTarget/subLocalSource/subLocalSource1.txt")));
		assertThat(
				out.getPayload().get(2),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt"),
						equalTo("ftpTarget/subLocalSource/subLocalSource1.txt")));
	}

	@Test
	public void testInt3088MPutRecursiveFiltered() {
		this.inboundMPutRecursiveFiltered.send(new GenericMessage<File>(getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertNotNull(out);
		assertEquals(2, out.getPayload().size());
		assertThat(out.getPayload().get(0),
				not(equalTo(out.getPayload().get(1))));
		assertThat(
				out.getPayload().get(0),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt"),
						equalTo("ftpTarget/subLocalSource/subLocalSource1.txt")));
		assertThat(
				out.getPayload().get(1),
				anyOf(equalTo("ftpTarget/localSource1.txt"), equalTo("ftpTarget/localSource2.txt"),
						equalTo("ftpTarget/subLocalSource/subLocalSource1.txt")));
	}

	@Test
	public void testInt3412FileMode() {
		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(ftpSessionFactory);
		assertFalse(template.exists("ftpTarget/appending.txt"));
		Message<String> m = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.FILENAME, "appending.txt")
				.build();
		appending.send(m);
		appending.send(m);

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

	@Test
	public void testStream() {
		String dir = "ftpSource/";
		this.inboundGetStream.send(new GenericMessage<Object>(dir + " ftpSource1.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		assertEquals("source1", result.getPayload());
		assertEquals("ftpSource/", result.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals(" ftpSource1.txt", result.getHeaders().get(FileHeaders.REMOTE_FILE));

		Session<?> session = (Session<?>) result.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE);
		// Returned to cache
		assertTrue(session.isOpen());
		// Raw reading is finished
		assertFalse(TestUtils.getPropertyValue(session, "targetSession.readingRaw", AtomicBoolean.class).get());

		// Check that we can use the same session from cache to read another remote InputStream
		this.inboundGetStream.send(new GenericMessage<Object>(dir + "ftpSource2.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		assertEquals("source2", result.getPayload());
		assertEquals("ftpSource/", result.getHeaders().get(FileHeaders.REMOTE_DIRECTORY));
		assertEquals("ftpSource2.txt", result.getHeaders().get(FileHeaders.REMOTE_FILE));
		assertSame(TestUtils.getPropertyValue(session, "targetSession"),
				TestUtils.getPropertyValue(result.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE),
						"targetSession"));
	}

	@Test
	public void testMgetPartial() throws Exception {
		Session<FTPFile> session = spyOnSession();
		doAnswer(new Answer<FTPFile[]>() {

			@Override
			public FTPFile[] answer(InvocationOnMock invocation) throws Throwable {
				FTPFile[] files = (FTPFile[]) invocation.callRealMethod();
				// add an extra file where the get will fail
				files = Arrays.copyOf(files, files.length + 1);
				FTPFile bogusFile = new FTPFile();
				bogusFile.setName("bogus.txt");
				files[files.length - 1] = bogusFile;
				return files;
			}
		}).when(session).list("ftpSource/subFtpSource/*");
		String dir = "ftpSource/subFtpSource/";
		try {
			this.inboundMGet.send(new GenericMessage<Object>(dir + "*"));
			fail("expected exception");
		}
		catch (PartialSuccessException e) {
			assertEquals(2, e.getDerivedInput().size());
			assertEquals(1, e.getPartialResults().size());
			assertThat(e.getCause().getMessage(),
					containsString("/ftpSource/subFtpSource/bogus.txt: No such file or directory."));
		}

	}

	@Test
	public void testMgetRecursivePartial() throws Exception {
		Session<FTPFile> session = spyOnSession();
		doAnswer(new Answer<FTPFile[]>() {

			@Override
			public FTPFile[] answer(InvocationOnMock invocation) throws Throwable {
				FTPFile[] files = (FTPFile[]) invocation.callRealMethod();
				// add an extra file where the get will fail
				files = Arrays.copyOf(files, files.length + 1);
				FTPFile bogusFile = new FTPFile();
				bogusFile.setName("bogus.txt");
				bogusFile.setTimestamp(Calendar.getInstance());
				files[files.length - 1] = bogusFile;
				return files;
			}
		}).when(session).list("ftpSource/subFtpSource/");
		String dir = "ftpSource/";
		try {
			this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
			fail("expected exception");
		}
		catch (PartialSuccessException e) {
			assertEquals(4, e.getDerivedInput().size());
			assertEquals(2, e.getPartialResults().size());
			assertThat(e.getCause().getMessage(),
					containsString("/ftpSource/subFtpSource/bogus.txt: No such file or directory."));
		}
	}

	@Test
	public void testMputPartial() throws Exception {
		Session<FTPFile> session = spyOnSession();
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				throw new IOException("Failed to send localSource2");
			}

		}).when(session).write(Mockito.any(InputStream.class), Mockito.contains("localSource2"));
		try {
			this.inboundMPut.send(new GenericMessage<File>(getSourceLocalDirectory()));
			fail("expected exception");
		}
		catch (PartialSuccessException e) {
			assertEquals(3, e.getDerivedInput().size());
			assertEquals(1, e.getPartialResults().size());
			assertEquals("ftpTarget/localSource1.txt", e.getPartialResults().iterator().next());
			assertThat(e.getCause().getMessage(),
					containsString("Failed to send localSource2"));
		}
	}

	@Test
	public void testMputRecursivePartial() throws Exception {
		Session<FTPFile> session = spyOnSession();
		File sourceLocalSubDirectory =  new File(getSourceLocalDirectory(), "subLocalSource");
		assertTrue(sourceLocalSubDirectory.isDirectory());
		File extra = new File(sourceLocalSubDirectory, "subLocalSource2.txt");
		FileOutputStream writer = new FileOutputStream(extra);
		writer.write("foo".getBytes());
		writer.close();
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				throw new IOException("Failed to send subLocalSource2");
			}

		}).when(session).write(Mockito.any(InputStream.class), Mockito.contains("subLocalSource2"));
		try {
			this.inboundMPutRecursive.send(new GenericMessage<File>(getSourceLocalDirectory()));
			fail("expected exception");
		}
		catch (PartialSuccessException e) {
			assertEquals(3, e.getDerivedInput().size());
			assertEquals(2, e.getPartialResults().size());
			assertThat(e.getCause(), Matchers.instanceOf(PartialSuccessException.class));
			PartialSuccessException cause = (PartialSuccessException) e.getCause();
			assertEquals(2, cause.getDerivedInput().size());
			assertEquals(1, cause.getPartialResults().size());
			assertThat(cause.getCause().getMessage(), containsString("Failed to send subLocalSource2"));
		}
		extra.delete();
	}

	private Session<FTPFile> spyOnSession() {
		Session<FTPFile> session = spy(this.ftpSessionFactory.getSession());
		session.close();
		@SuppressWarnings("unchecked")
		BlockingQueue<Session<FTPFile>> cache = TestUtils.getPropertyValue(ftpSessionFactory, "pool.available",
				BlockingQueue.class);
		assertNotNull(cache.poll());
		cache.offer(session);
		@SuppressWarnings("unchecked")
		Set<Session<FTPFile>> allocated = TestUtils.getPropertyValue(ftpSessionFactory, "pool.allocated",
				Set.class);
		allocated.clear();
		allocated.add(session);
		return session;
	}

	private void assertLength6(FtpRemoteFileTemplate template) {
		FTPFile[] files = template.execute(new SessionCallback<FTPFile, FTPFile[]>() {

			@Override
			public FTPFile[] doInSession(Session<FTPFile> session) throws IOException {
				return session.list("ftpTarget/appending.txt");
			}
		});
		assertEquals(1, files.length);
		assertEquals(6, files[0].getSize());
	}

	@Test
	public void testMessageSessionCallback() {
		this.inboundCallback.send(new GenericMessage<String>("foo"));
		Message<?> receive = this.output.receive(10000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testLsForNullDir() throws IOException {
		Session<FTPFile> session = ftpSessionFactory.getSession();
		((FTPClient) session.getClientInstance()).changeWorkingDirectory("ftpSource");
		session.close();

		this.inboundLs.send(new GenericMessage<String>("foo"));
		Message<?> receive = this.output.receive(10000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(List.class));
		List<String> files = (List<String>) receive.getPayload();
		assertEquals(2, files.size());
		assertThat(files, containsInAnyOrder(" ftpSource1.txt", "ftpSource2.txt"));

		FTPFile[] ftpFiles = ftpSessionFactory.getSession().list(null);
		for (FTPFile ftpFile : ftpFiles) {
			if (!ftpFile.isDirectory()) {
				assertTrue(files.contains(ftpFile.getName()));
			}
		}
	}

	@Test
	public void testInboundChannelAdapterWithNullDir() throws IOException {
		Session<FTPFile> session = ftpSessionFactory.getSession();
		((FTPClient) session.getClientInstance()).changeWorkingDirectory("ftpSource");
		session.close();
		this.ftpInbound.start();

		Message<?> message = this.output.receive(10000);
		assertNotNull(message);
		assertThat(message.getPayload(), instanceOf(File.class));
		assertEquals(" ftpSource1.txt", ((File) message.getPayload()).getName());

		message = this.output.receive(10000);
		assertNotNull(message);
		assertThat(message.getPayload(), instanceOf(File.class));
		assertEquals("ftpSource2.txt", ((File) message.getPayload()).getName());

		assertNull(this.output.receive(10));

		this.ftpInbound.stop();
	}

	public static class SortingFileListFilter implements FileListFilter<File> {

		@Override
		public List<File> filterFiles(File[] files) {
			File[] sorted = Arrays.copyOf(files, files.length);
			Arrays.sort(sorted, new Comparator<File>() {

				@Override
				public int compare(File o1, File o2) {
					if (o1.isDirectory() && !o2.isDirectory()) {
						return 1;
					}
					else if (!o1.isDirectory() && o2.isDirectory()) {
						return -1;
					}
					else {
						return o1.getName().compareTo(o2.getName());
					}
				}

			});
			return Arrays.asList(sorted);
		}

	}

	@SuppressWarnings("unused")
	private static final class TestMessageSessionCallback
			implements MessageSessionCallback<FTPFile, Object> {

		@Override
		public Object doInSession(Session<FTPFile> session, Message<?> requestMessage) throws IOException {
			return ((String) requestMessage.getPayload()).toUpperCase();
		}

	}

	public static class Config {

		private volatile String targetLocalDirectoryName;

		@Bean
		public SessionFactory<FTPFile> ftpSessionFactory() {
			return FtpServerOutboundTests.sessionFactory();
		}

		public String getTargetLocalDirectoryName() {
			return this.targetLocalDirectoryName;
		}

	}

}
