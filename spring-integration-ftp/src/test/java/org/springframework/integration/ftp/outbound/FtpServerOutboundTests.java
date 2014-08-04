/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.ftp.outbound;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.InputStreamCallback;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.ftp.TestFtpServer;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
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
public class FtpServerOutboundTests {

	@Autowired
	private TestFtpServer ftpServer;

	@Autowired
	private DefaultFtpSessionFactory ftpSessionFactory;

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

	@Before
	public void setup() {
		this.ftpServer.recursiveDelete(ftpServer.getTargetLocalDirectory());
		this.ftpServer.recursiveDelete(ftpServer.getTargetFtpDirectory());
	}

	@Test
	public void testInt2866LocalDirectoryExpressionGET() {
		String dir = "ftpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "ftpSource1.txt"));
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
			this.invalidDirExpression.send(new GenericMessage<Object>("/ftpSource/ftpSource1.txt"));
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

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}

		dir = "ftpSource/subFtpSource/";
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
		String dir = "ftpSource/";
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
		FileCopyUtils.copy(session.readRaw("ftpSource/ftpSource1.txt"), baos);
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
		assertTrue(template.get(new GenericMessage<String>("ftpSource/ftpSource1.txt"), new InputStreamCallback() {

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
		this.inboundMPut.send(new GenericMessage<File>(this.ftpServer.getSourceLocalDirectory()));
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
		this.inboundMPutRecursive.send(new GenericMessage<File>(this.ftpServer.getSourceLocalDirectory()));
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
		this.inboundMPutRecursiveFiltered.send(new GenericMessage<File>(this.ftpServer.getSourceLocalDirectory()));
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
		Message<String> m = MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.FILENAME, "appending.txt")
				.build();
		appending.send(m);
		appending.send(m);

		FtpRemoteFileTemplate template = new FtpRemoteFileTemplate(ftpSessionFactory);
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

}
