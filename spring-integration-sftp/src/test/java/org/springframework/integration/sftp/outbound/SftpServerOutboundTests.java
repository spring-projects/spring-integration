/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.integration.sftp.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.MessageSessionCallback;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.sftp.server.ApacheMinaSftpEvent;
import org.springframework.integration.sftp.server.DirectoryCreatedEvent;
import org.springframework.integration.sftp.server.FileWrittenEvent;
import org.springframework.integration.sftp.server.PathMovedEvent;
import org.springframework.integration.sftp.server.PathRemovedEvent;
import org.springframework.integration.sftp.server.SessionClosedEvent;
import org.springframework.integration.sftp.server.SessionOpenedEvent;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpServerOutboundTests extends SftpTestSupport {

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
	private DirectChannel inboundLSRecursive;

	@Autowired
	private DirectChannel inboundLSRecursiveALL;

	@Autowired
	private DirectChannel inboundLSRecursiveNoDirs;

	@Autowired
	private DirectChannel inboundMGetRecursiveFiltered;

	@Autowired
	private DirectChannel inboundMPut;

	@Autowired
	private DirectChannel inboundMPutRecursive;

	@Autowired
	private DirectChannel inboundMPutRecursiveFiltered;

	@Autowired
	private SessionFactory<LsEntry> sessionFactory;

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
	private Config config;

	@Autowired
	private SftpRemoteFileTemplate template;

	@Autowired
	private PropertiesPersistingMetadataStore store;

	@BeforeEach
	public void setup() {
		this.config.targetLocalDirectoryName = getTargetLocalDirectoryName();
	}

	@Test
	public void testInt2866LocalDirectoryExpressionGET() {
		Session<?> session = this.sessionFactory.getSession();
		String dir = "sftpSource/";
		long modified = setModifiedOnSource1();
		this.inboundGet.send(new GenericMessage<Object>(dir + " sftpSource1.txt"));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		File localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir.toUpperCase());
		assertPreserved(modified, localFile);

		dir = "sftpSource/subSftpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "subSftpSource1.txt"));
		result = this.output.receive(1000);
		assertThat(result).isNotNull();
		localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir.toUpperCase());
		Session<?> session2 = this.sessionFactory.getSession();
		assertThat(TestUtils.getPropertyValue(session2, "targetSession.jschSession"))
				.isSameAs(TestUtils.getPropertyValue(session, "targetSession.jschSession"));
	}

	@Test
	public void testInt2866InvalidLocalDirectoryExpression() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() ->
						this.invalidDirExpression.send(new GenericMessage<Object>("sftpSource/ sftpSource1.txt")))
				.withRootCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("Failed to make local directory");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt2866LocalDirectoryExpressionMGET() {
		String dir = "sftpSource/";
		long modified = setModifiedOnSource1();
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		List<File> localFiles = (List<File>) result.getPayload();

		assertThat(localFiles).hasSizeGreaterThan(0);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
			if (file.getPath().contains("localTarget1")) {
				assertPreserved(modified, file);
			}
		}
		dir = "sftpSource/subSftpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		result = this.output.receive(1000);
		assertThat(result).isNotNull();
		localFiles = (List<File>) result.getPayload();

		assertThat(localFiles).hasSizeGreaterThan(0);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3172LocalDirectoryExpressionMGETRecursive() throws IOException {
		String dir = "sftpSource/";
		long modified = setModifiedOnSource1();
		File secondRemote = new File(getSourceRemoteDirectory(), "sftpSource2.txt");
		secondRemote.setLastModified(System.currentTimeMillis() - 1_000_000);
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		List<File> localFiles = (List<File>) result.getPayload();
		assertThat(localFiles).hasSize(3);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
			if (file.getPath().contains("localTarget1")) {
				assertPreserved(modified, file);
			}
		}
		assertThat(localFiles.get(2).getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir + "subSftpSource");

		File secondTarget = new File(getTargetLocalDirectory() + File.separator + "sftpSource", "localTarget2.txt");
		ByteArrayOutputStream remoteContents = new ByteArrayOutputStream();
		ByteArrayOutputStream localContents = new ByteArrayOutputStream();
		FileUtils.copyFile(secondRemote, remoteContents);
		FileUtils.copyFile(secondTarget, localContents);
		String localAsString = new String(localContents.toByteArray());
		assertThat(localAsString).isEqualTo(new String(remoteContents.toByteArray()));
		long oldLastModified = secondRemote.lastModified();
		FileUtils.copyInputStreamToFile(new ByteArrayInputStream("junk".getBytes()), secondRemote);
		long newLastModified = secondRemote.lastModified();
		secondRemote.setLastModified(oldLastModified);
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		this.output.receive(0);
		localContents = new ByteArrayOutputStream();
		FileUtils.copyFile(secondTarget, localContents);
		assertThat(new String(localContents.toByteArray())).isEqualTo(localAsString);
		secondRemote.setLastModified(newLastModified);
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		this.output.receive(0);
		localContents = new ByteArrayOutputStream();
		FileUtils.copyFile(secondTarget, localContents);
		assertThat(new String(localContents.toByteArray())).isEqualTo("junk");
		// restore the remote file contents
		FileUtils.copyInputStreamToFile(new ByteArrayInputStream(localAsString.getBytes()), secondRemote);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLSRecursive() throws IOException {
		String dir = "sftpSource/";
		this.inboundLSRecursive.send(new GenericMessage<Object>(dir));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		List<SftpFileInfo> files = (List<SftpFileInfo>) result.getPayload();
		assertThat(files).hasSize(4);
		assertThat(files.stream()
				.map(fi -> fi.getFilename())
				.collect(Collectors.toList())).contains(
						" sftpSource1.txt",
						"sftpSource2.txt",
						"subSftpSource",
						"subSftpSource/subSftpSource1.txt");
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLSRecursiveALL() throws IOException {
		String dir = "sftpSource/";
		this.inboundLSRecursiveALL.send(new GenericMessage<Object>(dir));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		List<SftpFileInfo> files = (List<SftpFileInfo>) result.getPayload();
		assertThat(files).hasSize(8);
		assertThat(files.stream()
				.map(fi -> fi.getFilename())
				.collect(Collectors.toList())).contains(
						" sftpSource1.txt",
						"sftpSource2.txt",
						"subSftpSource",
						"subSftpSource/subSftpSource1.txt",
						".",
						"..",
						"subSftpSource/.",
						"subSftpSource/..");
	}

	@Test
	@SuppressWarnings("unchecked")
	void testLSRecursiveNoDirs() throws IOException {
		String dir = "sftpSource/";
		this.inboundLSRecursiveNoDirs.send(new GenericMessage<Object>(dir));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		List<SftpFileInfo> files = (List<SftpFileInfo>) result.getPayload();
		assertThat(files).hasSize(3);
		assertThat(files.stream()
				.map(fi -> fi.getFilename())
				.collect(Collectors.toList())).contains(
						" sftpSource1.txt",
						"sftpSource2.txt",
						"subSftpSource/subSftpSource1.txt");
		File newDeepFile = new File(this.sourceRemoteDirectory + "/subSftpSource/subSftpSource2.txt");
		OutputStream fos = new FileOutputStream(newDeepFile);
		fos.write("test".getBytes());
		fos.close();
		this.inboundLSRecursiveNoDirs.send(new GenericMessage<Object>(dir));
		result = this.output.receive(1000);
		assertThat(result).isNotNull();
		files = (List<SftpFileInfo>) result.getPayload();
		assertThat(files).hasSize(1);
		assertThat(files.get(0).getFilename()).isEqualTo("subSftpSource/subSftpSource2.txt");
		assertThat(this.store.get("testsubSftpSource/subSftpSource2.txt")).isNotNull();
	}

	private long setModifiedOnSource1() {
		File firstRemote = new File(getSourceRemoteDirectory(), " sftpSource1.txt");
		firstRemote.setLastModified(System.currentTimeMillis() - 1_000_000);
		long modified = firstRemote.lastModified();
		assertThat(modified).isGreaterThan(0);
		return modified;
	}

	private void assertPreserved(long modified, File file) {
		assertThat(Math.abs(file.lastModified() - modified))
				.as("lastModified wrong by " + (modified - file.lastModified())).isLessThan(1_000);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3172LocalDirectoryExpressionMGETRecursiveFiltered() {
		String dir = "sftpSource/";
		this.inboundMGetRecursiveFiltered.send(new GenericMessage<Object>(dir + "*"));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		List<File> localFiles = (List<File>) result.getPayload();
		// should have filtered sftpSource2.txt
		assertThat(localFiles).hasSize(2);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
		}
		assertThat(localFiles.get(1).getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir + "subSftpSource");

	}

	/**
	 * Only runs with a real server (see class javadocs).
	 */
	@Test
	public void testInt3100RawGET() throws Exception {
		Session<?> session = this.sessionFactory.getSession();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("sftpSource/ sftpSource1.txt"), baos);
		assertThat(session.finalizeRaw()).isTrue();
		assertThat(new String(baos.toByteArray())).isEqualTo("source1");

		baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("sftpSource/sftpSource2.txt"), baos);
		assertThat(session.finalizeRaw()).isTrue();
		assertThat(new String(baos.toByteArray())).isEqualTo("source2");

		session.close();
	}

	@Test
	public void testSftpCopy() throws Exception {
		this.template.execute(session -> {
			PipedInputStream in = new PipedInputStream();
			PipedOutputStream out = new PipedOutputStream(in);
			session.read("sftpSource/sftpSource2.txt", out);
			session.write(in, "sftpTarget/sftpTarget2.txt");
			return null;
		});

		Session<?> session = this.sessionFactory.getSession();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("sftpTarget/sftpTarget2.txt"), baos);
		assertThat(session.finalizeRaw()).isTrue();
		assertThat(new String(baos.toByteArray())).isEqualTo("source2");

		baos = new ByteArrayOutputStream();
		FileCopyUtils.copy(session.readRaw("sftpSource/sftpSource2.txt"), baos);
		assertThat(session.finalizeRaw()).isTrue();
		assertThat(new String(baos.toByteArray())).isEqualTo("source2");

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
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				session1.write(pipe1, "foo.txt");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			latch1.countDown();
		});
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				session2.write(pipe2, "bar.txt");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			latch2.countDown();
		});

		out1.write('a');
		out2.write('b');
		out1.write('c');
		out2.write('d');
		out1.write('e');
		out2.write('f');
		out1.close();
		out2.close();
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		session1.read("foo.txt", bos1);
		session2.read("bar.txt", bos2);
		assertThat(new String(bos1.toByteArray())).isEqualTo("ace");
		assertThat(new String(bos2.toByteArray())).isEqualTo("bdf");
		session1.remove("foo.txt");
		session2.remove("bar.txt");
		session1.close();
		session2.close();
	}

	@Test
	public void testInt3088MPutNotRecursive() throws Exception {
		resetSessionCache();
		this.config.events.clear();
		this.config.latch = new CountDownLatch(1);
		Session<?> session = sessionFactory.getSession();
		session.close();
		session = TestUtils.getPropertyValue(session, "targetSession", Session.class);
		ChannelSftp channel = spy(TestUtils.getPropertyValue(session, "channel", ChannelSftp.class));
		new DirectFieldAccessor(session).setPropertyValue("channel", channel);

		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		while (output.receive(0) != null) {
			// drain
		}
		this.inboundMPut.send(new GenericMessage<File>(getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0)).isNotEqualTo(out.getPayload().get(1));
		assertThat(out.getPayload().get(0))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt");
		assertThat(out.getPayload().get(1))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt");
		verify(channel).chmod(0600, "sftpTarget/localSource1.txt");
		verify(channel).chmod(0600, "sftpTarget/localSource2.txt");
		resetSessionCache();
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.events).hasSize(6);
		assertThat(this.config.events.get(0)).isInstanceOf(SessionOpenedEvent.class);
		assertThat(this.config.events.get(1)).isInstanceOf(FileWrittenEvent.class);
		assertThat(((FileWrittenEvent) this.config.events.get(1)).getFile().toString())
				.matches("/sftpTarget/localSource(1|2).txt.writing");
		assertThat(this.config.events.get(2)).isInstanceOf(PathMovedEvent.class);
		assertThat(((PathMovedEvent) this.config.events.get(2)).getSrcPath().toString())
				.matches("/sftpTarget/localSource(1|2).txt.writing");
		assertThat(((PathMovedEvent) this.config.events.get(2)).getDstPath().toString())
				.matches("/sftpTarget/localSource(1|2).txt");
		assertThat(this.config.events.get(3)).isInstanceOf(FileWrittenEvent.class);
		assertThat(((FileWrittenEvent) this.config.events.get(3)).getFile().toString())
				.matches("/sftpTarget/localSource(1|2).txt.writing");
		assertThat(this.config.events.get(4)).isInstanceOf(PathMovedEvent.class);
		assertThat(((PathMovedEvent) this.config.events.get(4)).getSrcPath().toString())
				.matches("/sftpTarget/localSource(1|2).txt.writing");
		assertThat(((PathMovedEvent) this.config.events.get(4)).getDstPath().toString())
				.matches("/sftpTarget/localSource(1|2).txt");
		assertThat(this.config.events.get(5)).isInstanceOf(SessionClosedEvent.class);
		this.config.events.clear();
		this.config.latch = null;
	}

	@Test
	public void allEvents() throws InterruptedException {
		resetSessionCache();
		this.config.events.clear();
		this.config.latch = new CountDownLatch(1);
		this.template.execute(session -> {
			assertThat(session.mkdir("/sftpTarget/allEventsDir")).isTrue();
			session.write(new ByteArrayInputStream("foo".getBytes()), "/sftpTarget/allEventsDir/file.txt");
			session.append(new ByteArrayInputStream("bar".getBytes()), "/sftpTarget/allEventsDir/file.txt");
			session.rename("/sftpTarget/allEventsDir/file.txt", "/sftpTarget/allEventsDir/file2.txt");
			assertThat(session.remove("/sftpTarget/allEventsDir/file2.txt")).isTrue();
			session.rename("/sftpTarget/allEventsDir", "/sftpTarget/allEventsDir2");
			session.rmdir("/sftpTarget/allEventsDir2");
			return null;
		});
		resetSessionCache();
		assertThat(this.config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.config.events).hasSize(9);
		assertThat(this.config.events.get(0)).isInstanceOf(SessionOpenedEvent.class);
		assertThat(this.config.events.get(1)).isInstanceOf(DirectoryCreatedEvent.class);
		DirectoryCreatedEvent dce = (DirectoryCreatedEvent) this.config.events.get(1);
		assertThat(dce.getPath().toString()).isEqualTo("/sftpTarget/allEventsDir");
		assertThat(this.config.events.get(2)).isInstanceOf(FileWrittenEvent.class);
		FileWrittenEvent fwe = (FileWrittenEvent) this.config.events.get(2);
		assertThat(fwe.getFile().toString()).isEqualTo("/sftpTarget/allEventsDir/file.txt");
		assertThat(fwe.getDataLen()).isEqualTo(3);
		assertThat(this.config.events.get(3)).isInstanceOf(FileWrittenEvent.class);
		fwe = (FileWrittenEvent) this.config.events.get(3);
		assertThat(fwe.getFile().toString()).isEqualTo("/sftpTarget/allEventsDir/file.txt");
		assertThat(fwe.getDataLen()).isEqualTo(3);
		assertThat(this.config.events.get(4)).isInstanceOf(PathMovedEvent.class);
		PathMovedEvent pme = (PathMovedEvent) this.config.events.get(4);
		assertThat(pme.getSrcPath().toString()).isEqualTo("/sftpTarget/allEventsDir/file.txt");
		assertThat(pme.getDstPath().toString()).isEqualTo("/sftpTarget/allEventsDir/file2.txt");
		assertThat(this.config.events.get(5)).isInstanceOf(PathRemovedEvent.class);
		PathRemovedEvent pre = (PathRemovedEvent) this.config.events.get(5);
		assertThat(pre.getPath().toString()).isEqualTo("/sftpTarget/allEventsDir/file2.txt");
		assertThat(pre.isDirectory()).isFalse();
		assertThat(this.config.events.get(6)).isInstanceOf(PathMovedEvent.class);
		pme = (PathMovedEvent) this.config.events.get(6);
		assertThat(pme.getSrcPath().toString()).isEqualTo("/sftpTarget/allEventsDir");
		assertThat(pme.getDstPath().toString()).isEqualTo("/sftpTarget/allEventsDir2");
		assertThat(this.config.events.get(7)).isInstanceOf(PathRemovedEvent.class);
		pre = (PathRemovedEvent) this.config.events.get(7);
		assertThat(pre.getPath().toString()).isEqualTo("/sftpTarget/allEventsDir2");
		assertThat(pre.isDirectory()).isTrue();
		assertThat(this.config.events.get(8)).isInstanceOf(SessionClosedEvent.class);
		this.config.events.clear();
		this.config.latch = null;
	}

	private void resetSessionCache() {
		((CachingSessionFactory<?>) this.sessionFactory).resetCache();
	}

	@Test
	public void testInt3088MPutRecursive() {
		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		while (output.receive(0) != null) {
			// drain
		}
		this.inboundMPutRecursive.send(new GenericMessage<>(getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(3);
		assertThat(out.getPayload().get(0)).isNotEqualTo(out.getPayload().get(1));
		assertThat(out.getPayload().get(0))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt",
						"sftpTarget/subLocalSource/subLocalSource1.txt");
		assertThat(out.getPayload().get(1))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt",
						"sftpTarget/subLocalSource/subLocalSource1.txt");
		assertThat(out.getPayload().get(2))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt",
						"sftpTarget/subLocalSource/subLocalSource1.txt");
	}

	@Test
	public void testInt3088MPutRecursiveFiltered() {
		String dir = "sftpSource/";
		this.inboundMGetRecursive.send(new GenericMessage<Object>(dir + "*"));
		while (output.receive(0) != null) {
			// drain
		}
		this.inboundMPutRecursiveFiltered.send(new GenericMessage<>(getSourceLocalDirectory()));
		@SuppressWarnings("unchecked")
		Message<List<String>> out = (Message<List<String>>) this.output.receive(1000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).hasSize(2);
		assertThat(out.getPayload().get(0)).isNotEqualTo(out.getPayload().get(1));
		assertThat(out.getPayload().get(0))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt",
						"sftpTarget/subLocalSource/subLocalSource1.txt");
		assertThat(out.getPayload().get(1))
				.isIn("sftpTarget/localSource1.txt", "sftpTarget/localSource2.txt",
						"sftpTarget/subLocalSource/subLocalSource1.txt");
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
			assertThat(e.getCause().getCause().getMessage()).contains("The destination file already exists");
		}

	}

	@Test
	public void testStream() {
		Session<?> session = spy(this.sessionFactory.getSession());
		session.close();

		String dir = "sftpSource/";
		this.inboundGetStream.send(new GenericMessage<>(dir + " sftpSource1.txt"));
		Message<?> result = this.output.receive(1000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("source1");
		assertThat(result.getHeaders())
				.containsEntry(FileHeaders.REMOTE_DIRECTORY, "sftpSource/")
				.containsEntry(FileHeaders.REMOTE_FILE, " sftpSource1.txt");
		verify(session).close();
	}

	@Test
	public void testMessageSessionCallback() {
		this.inboundCallback.send(new GenericMessage<>("foo"));
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("FOO");
	}

	private void assertLength6(SftpRemoteFileTemplate template) {
		LsEntry[] files = template.execute(session -> session.list("sftpTarget/appending.txt"));
		assertThat(files.length).isEqualTo(1);
		assertThat(files[0].getAttrs().getSize()).isEqualTo(6);
	}

	@Test
	public void testSessionExists() throws IOException {
		DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
		sessionFactory.setHost("localhost");
		sessionFactory.setPort(port);
		sessionFactory.setUser("foo");
		sessionFactory.setPassword("foo");
		sessionFactory.setAllowUnknownKeys(true);
		Session<LsEntry> session = sessionFactory.getSession();

		assertThat(session.exists("sftpSource")).isTrue();
		assertThat(session.exists("notExist")).isFalse();

		session.close();

		assertThatExceptionOfType(UncheckedIOException.class)
				.isThrownBy(() -> session.exists("any"))
				.withRootCauseInstanceOf(IOException.class)
				.withStackTraceContaining("Pipe closed");
	}

	@SuppressWarnings("unused")
	private static final class TestMessageSessionCallback
			implements MessageSessionCallback<LsEntry, Object> {

		@Override
		public Object doInSession(Session<ChannelSftp.LsEntry> session, Message<?> requestMessage) {
			return ((String) requestMessage.getPayload()).toUpperCase();
		}

	}

	public static class Config {

		final List<ApacheMinaSftpEvent> events = new ArrayList<>();

		private volatile String targetLocalDirectoryName;

		private volatile CountDownLatch latch;

		@Bean
		public SessionFactory<LsEntry> sftpSessionFactory(ApplicationContext context) {
			SftpServerOutboundTests.eventListener().setApplicationEventPublisher(context);
			return SftpServerOutboundTests.sessionFactory();
		}

		@Bean
		public SftpRemoteFileTemplate template(SessionFactory<LsEntry> sf) {
			return new SftpRemoteFileTemplate(sf);
		}

		public String getTargetLocalDirectoryName() {
			return this.targetLocalDirectoryName;
		}

		@Bean
		public ApplicationEventListeningMessageProducer events() {
			ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
			producer.setEventTypes(ApacheMinaSftpEvent.class);
			producer.setOutputChannel(eventChannel());
			return producer;
		}

		@Bean
		public MessageChannel eventChannel() {
			return (msg, timeout) -> {
				if (this.latch != null) {
					if (this.events.size() > 0 || msg.getPayload() instanceof SessionOpenedEvent) {
						this.events.add((ApacheMinaSftpEvent) msg.getPayload());
						if (msg.getPayload() instanceof SessionClosedEvent) {
							this.latch.countDown();
						}
					}
				}
				return true;
			};
		}

	}

}
