/*
 * Copyright 2022-present the original author or authors.
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

package org.springframework.integration.smb.dsl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.codelibs.jcifs.smb.impl.SmbException;
import org.codelibs.jcifs.smb.impl.SmbFile;
import org.codelibs.jcifs.smb.impl.SmbFileInputStream;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.smb.SmbTestSupport;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizingMessageSource;
import org.springframework.integration.smb.inbound.SmbStreamingMessageSource;
import org.springframework.integration.smb.session.SmbFileInfo;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * The intent is tests retrieve from smbSource and verify arrival in localTarget or
 * send from localSource and verify arrival in remoteTarget.
 *
 * @author Gregory Bragg
 * @author Artem Vozhdayenko
 * @author Artem Bilan
 * @author Daniel Frey
 * @author Glenn Renfro
 *
 * @since 6.0
 */

@SpringJUnitConfig
@DirtiesContext
public class SmbTests extends SmbTestSupport {

	@Autowired
	private IntegrationFlowContext flowContext;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testSmbInboundFlow() {
		QueueChannel out = new QueueChannel();
		DirectoryScanner scanner = new DefaultDirectoryScanner();
		IntegrationFlow flow = IntegrationFlow.from(Smb.inboundAdapter(sessionFactory())
								.preserveTimestamp(true)
								.remoteDirectory("smbSource/subSmbSource/")
								.maxFetchSize(10)
								.scanner(scanner)
								.regexFilter(".*\\.txt$")
								.localFilename(f -> f.toUpperCase() + ".a")
								.localDirectory(getTargetLocalDirectory()),
						e -> e.id("smbInboundAdapter").poller(Pollers.fixedDelay(100)))
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Map<?, ?> components = TestUtils.getPropertyValue(registration, "integrationFlow.integrationComponents");
		Iterator<?> iterator = components.keySet().iterator();
		iterator.next();
		Object spcafb = iterator.next();
		assertThat(TestUtils.<Object>getPropertyValue(spcafb, "source.fileSource.scanner")).isSameAs(scanner);
		Message<?> message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getHeaders())
				.containsKeys(FileHeaders.REMOTE_HOST_PORT, FileHeaders.REMOTE_DIRECTORY, FileHeaders.REMOTE_FILE);
		Object payload = message.getPayload();
		assertThat(payload).isInstanceOf(File.class);
		File file = (File) payload;
		assertThat(file.getName()).isEqualTo("SUBSMBSOURCE1.TXT.a", "SUBSMBSOURCE2.TXT.a");
		assertThat(file.getAbsolutePath()).contains("localTarget");

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		file = (File) message.getPayload();
		assertThat(file.getName()).isIn("SUBSMBSOURCE1.TXT.a", "SUBSMBSOURCE2.TXT.a");
		assertThat(file.getAbsolutePath()).contains("localTarget");

		assertThat(out.receive(10)).isNull();

		MessageSource<?> source = context.getBean(SmbInboundFileSynchronizingMessageSource.class);
		assertThat(TestUtils.<Integer>getPropertyValue(source, "maxFetchSize")).isEqualTo(10);

		registration.destroy();
	}

	@Test
	public void testSmbInboundStreamFlow() throws Exception {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlow.from(
						Smb.inboundStreamingAdapter(new SmbRemoteFileTemplate(sessionFactory()))
								.remoteDirectory("smbSource")
								.maxFetchSize(11)
								.regexFilter(".*\\.txt$"),
						e -> e.id("smbInboundAdapter").poller(Pollers.fixedDelay(100)))
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Message<?> message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isIn("smbSource1.txt", "smbSource2.txt");
		assertThat(message.getHeaders().get(
				FileHeaders.REMOTE_HOST_PORT, String.class)).contains(sessionFactory().getSession().getHostPort());
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isIn("smbSource1.txt", "smbSource2.txt");
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		MessageSource<?> source = context.getBean(SmbStreamingMessageSource.class);
		assertThat(TestUtils.<Integer>getPropertyValue(source, "maxFetchSize")).isEqualTo(11);
		registration.destroy();
	}

	@Test
	public void testSmbOutboundFlow() throws SmbException {
		IntegrationFlow flow = f -> f
				.handle(Smb.outboundAdapter(sessionFactory(), FileExistsMode.REPLACE)
						.useTemporaryFileName(false)
						.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
						.remoteDirectory("smbTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		Message<ByteArrayInputStream> message = MessageBuilder
				.withPayload(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		registration.getInputChannel().send(message);
		RemoteFileTemplate<SmbFile> template = new RemoteFileTemplate<>(sessionFactory());
		SmbFile[] files = template.execute(session ->
				session.list(getTargetRemoteDirectory().getName()));
		assertThat(files).hasSize(1);
		assertThat(files[0].length()).isEqualTo(3);

		registration.destroy();
	}

	@Test
	public void testSmbOutboundFlowWithSmbRemoteTemplate() throws SmbException {
		SmbRemoteFileTemplate smbTemplate = new SmbRemoteFileTemplate(sessionFactory());
		IntegrationFlow flow = f -> f
				.handle(Smb.outboundAdapter(smbTemplate)
						.useTemporaryFileName(false)
						.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
						.remoteDirectory("smbTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		Message<ByteArrayInputStream> message = MessageBuilder
				.withPayload(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		registration.getInputChannel().send(message);
		SmbFile[] files = smbTemplate.execute(session ->
				session.list(getTargetRemoteDirectory().getName()));
		assertThat(files).hasSize(1);
		assertThat(files[0].length()).isEqualTo(3);

		registration.destroy();
	}

	@Test
	public void testSmbOutboundFlowWithSmbRemoteTemplateAndMode() throws SmbException {
		SmbRemoteFileTemplate smbTemplate = new SmbRemoteFileTemplate(sessionFactory());
		IntegrationFlow flow = f -> f
				.handle(Smb.outboundAdapter(smbTemplate, FileExistsMode.APPEND)
						.useTemporaryFileName(false)
						.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
						.remoteDirectory("smbTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		Message<ByteArrayInputStream> message1 = MessageBuilder
				.withPayload(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		Message<ByteArrayInputStream> message2 = MessageBuilder
				.withPayload(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		registration.getInputChannel().send(message1);
		registration.getInputChannel().send(message2);
		SmbFile[] files = smbTemplate.execute(session ->
				session.list(getTargetRemoteDirectory().getName()));
		assertThat(files).hasSize(1);
		assertThat(files[0].length()).isEqualTo(9);

		registration.destroy();
	}

	@Test
	public void testSmbGetFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.GET, "payload")
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "'"))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "smbSource/subSmbSource/subSmbSource2.txt";
		registration.getInputChannel().send(new GenericMessage<>(fileName));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		File sfis = (File) result.getPayload();
		assertThat(sfis).hasFileName("subSmbSource2.txt");

		registration.destroy();
	}

	@Test
	public void testSmbGetStreamFlow() throws IOException {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.GET, "payload")
								.options(AbstractRemoteFileOutboundGateway.Option.STREAM)
								.fileExistsMode(FileExistsMode.IGNORE)
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
								.localFilenameExpression("#remoteFileName.replaceFirst('smbSource', 'localTarget')")
								.charset(StandardCharsets.UTF_8.name())
								.useTemporaryFileName(true))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "smbSource/subSmbSource/subSmbSource2.txt";
		registration.getInputChannel().send(new GenericMessage<>(fileName));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		try (SmbFileInputStream sfis = (SmbFileInputStream) result.getPayload()) {
			assertThat(sfis).isNotNull();
		}

		registration.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSmbMgetFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.MGET, "payload")
								.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
								.fileExistsMode(FileExistsMode.IGNORE)
								.filterExpression("name matches 'smbSource/|subSmbSource/|subSmbSource\\d\\.txt'")
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
								.localFilenameExpression("#remoteFileName.replaceFirst('subSmbSource', 'localTarget')")
								.charset(StandardCharsets.UTF_8.name())
								.useTemporaryFileName(true))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		registration.getInputChannel().send(new GenericMessage<>("*"));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		List<File> localFiles = (List<File>) result.getPayload();
		assertThat(localFiles).as("unexpected local files " + localFiles).hasSize(2);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
					.matches(".*smbSource/subSmbSource/localTarget\\d.txt");
		}

		registration.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSmbLsFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.LS, "payload")
								.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
								.fileExistsMode(FileExistsMode.IGNORE)
								.filterExpression("name matches 'subSmbSource|.*.txt'")
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
								.localFilenameExpression("#remoteFileName.replaceFirst('smbSource', 'localTarget')")
								.charset(StandardCharsets.UTF_8.name())
								.useTemporaryFileName(true))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String dir = "smbSource/subSmbSource/";
		registration.getInputChannel().send(new GenericMessage<>(dir));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		List<SmbFileInfo> localFiles = (List<SmbFileInfo>) result.getPayload();
		assertThat(localFiles).as("unexpected local files " + localFiles).hasSize(2);

		for (SmbFileInfo fileInfo : localFiles) {
			SmbFile file = fileInfo.getFileInfo();
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
		}

		assertThat(localFiles.get(1).getFileInfo().getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir + "subSmbSource");

		registration.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSmbNlstFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.NLST, "payload")
								.options(AbstractRemoteFileOutboundGateway.Option.NOSORT)
								.fileExistsMode(FileExistsMode.IGNORE)
								.filterExpression("name matches 'subSmbSource|.*.txt'")
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
								.localFilenameExpression("#remoteFileName.replaceFirst('smbSource', 'localTarget')")
								.charset(StandardCharsets.UTF_8.name())
								.useTemporaryFileName(true))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String dir = "smbSource/subSmbSource/";
		registration.getInputChannel().send(new GenericMessage<>(dir));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		List<String> localFilenames = (List<String>) result.getPayload();
		assertThat(localFilenames).as("unexpected local filenames " + localFilenames).hasSize(2);

		for (String filename : localFilenames) {
			assertThat(filename).contains("subSmbSource");
		}

		registration.destroy();
	}

	@Test
	public void testSmbPutFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.PUT, "payload")
								.useTemporaryFileName(false)
								.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
								.remoteDirectoryExpression("'smbSource/subSmbSource2/'")
								.autoCreateDirectory(true))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "subSmbSource2-1.txt";
		Message<ByteArrayInputStream> message = MessageBuilder
				.withPayload(new ByteArrayInputStream("subSmbSource2-1".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		registration.getInputChannel().send(message);
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		String path = (String) result.getPayload();
		assertThat(path)
				.isNotNull()
				.contains("subSmbSource2");

		registration.destroy();
	}

	@Test
	public void testSmbRmFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.RM, "payload"))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "smbSource/subSmbSource2/subSmbSource2-1.txt";
		registration.getInputChannel().send(new GenericMessage<>(fileName));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);

		registration.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSmbMputFlow() throws IOException {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.MPUT, "payload")
								.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
								.useTemporaryFileName(false)
								.remoteDirectoryExpression("'smbSource/subSmbSource2/'")
								.autoCreateDirectory(true)
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
								.localFilenameExpression("#remoteFileName.replaceFirst('smbSource', 'localTarget')"))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		File file1 = new File(getTargetLocalDirectoryName(), "subSmbSource2-2.txt");
		File file2 = new File(getTargetLocalDirectoryName(), "subSmbSource2-3.txt");
		file1.createNewFile();
		file2.createNewFile();

		List<File> files = new ArrayList<>();
		files.add(file1);
		files.add(file2);

		Message<List<File>> message = MessageBuilder
				.withPayload(files)
				.build();
		registration.getInputChannel().send(message);
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		List<String> remoteFilenames = (List<String>) result.getPayload();
		assertThat(remoteFilenames)
				.isNotNull()
				.as("unexpected remote filenames " + remoteFilenames).hasSize(2);

		for (String filename : remoteFilenames) {
			assertThat(filename).contains("subSmbSource2");
		}

		registration.destroy();
	}

	@Test
	public void testSmbMvFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Smb.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.MV, "payload")
								.renameExpression("'smbSource/subSmbSource2/subSmbSource-MV-Flow-Renamed.txt'"))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "smbSource/subSmbSource2/subSmbSource2-3.txt";
		registration.getInputChannel().send(new GenericMessage<>(fileName));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();

		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);

		registration.destroy();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

	}

}
