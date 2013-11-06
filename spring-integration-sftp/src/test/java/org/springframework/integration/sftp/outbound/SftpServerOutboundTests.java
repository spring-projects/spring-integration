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

package org.springframework.integration.sftp.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * Run with -Dspring-profiles-active=realSSH to run with a real SSH server.
 *
 * Assumes ftptest account on localhost with the following directory tree in the user's root...
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
	private SessionFactory<SftpFileInfo> sessionFactory;

	@Before
	public void setup() throws Exception {
		purge();
		setUpMocksIfNeeded();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setUpMocksIfNeeded() throws IOException {
		if (sessionFactory.toString().startsWith("Mock for")) {
			Session session = mock(Session.class);
			when(sessionFactory.getSession()).thenReturn(session);
			LsEntry entry1 = mock(LsEntry.class);
			SftpATTRS attrs1 = mock(SftpATTRS.class);
			when(entry1.getAttrs()).thenReturn(attrs1);
			when(entry1.getFilename()).thenReturn("sftpSource1.txt");
			LsEntry entry2 = mock(LsEntry.class);
			SftpATTRS attrs2 = mock(SftpATTRS.class);
			when(entry2.getAttrs()).thenReturn(attrs2);
			when(entry2.getFilename()).thenReturn("sftpSource2.txt");
			LsEntry entry3 = mock(LsEntry.class);
			when(entry3.getFilename()).thenReturn("subSftpSource");
			SftpATTRS attrs3 = mock(SftpATTRS.class);
			when(entry3.getAttrs()).thenReturn(attrs3);
			when(attrs3.isDir()).thenReturn(true);
			LsEntry entry4 = mock(LsEntry.class);
			SftpATTRS attrs4 = mock(SftpATTRS.class);
			when(entry4.getAttrs()).thenReturn(attrs4);
			// recursion uses a DFA to update the filename to include the subdirectory
			new DirectFieldAccessor(entry4).setPropertyValue("filename", "subSftpSource1.txt");
			when(entry4.getFilename()).thenCallRealMethod();
			when(session.list("sftpSource/sftpSource1.txt")).thenReturn(new LsEntry[] {
					entry1
			});
			when(session.list("sftpSource/")).thenReturn(new LsEntry[] {
					entry1, entry2, entry3
			});
			when(session.list("sftpSource/subSftpSource/")).thenReturn(new LsEntry[] {
					entry4
			});
			when(session.list("sftpSource/subSftpSource/subSftpSource1.txt")).thenReturn(new LsEntry[] {
					entry4
			});
		}
	}

	@After
	public void purge() {
		File local = new File("/tmp/sftpOutboundTests/");
		purge(local);
		local.delete();
	}

	private void purge(File local) {
		File[] files = local.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					this.purge(file);
				}
				file.delete();
			}
		}
	}

	@Test
	public void testInt2866LocalDirectoryExpressionGET() {
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
	}

	@Test
	public void testInt2866InvalidLocalDirectoryExpression() {
		try {
			this.invalidDirExpression.send(new GenericMessage<Object>("sftpSource/sftpSource1.txt"));
			fail("Exception expected.");
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
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
		if (!sessionFactory.toString().startsWith("Mock for")) {
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
	}


}
