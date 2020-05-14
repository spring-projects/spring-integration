/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.ftp.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
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
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.inbound.FtpStreamingMessageSource;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Joaquin Santana
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpTests extends FtpTestSupport {

	@Autowired
	private IntegrationFlowContext flowContext;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private IntegrationManagementConfigurer integrationManagementConfigurer;

	@Test
	public void testFtpInboundFlow() throws IOException {
		QueueChannel out = new QueueChannel();
		DirectoryScanner scanner = new DefaultDirectoryScanner();
		IntegrationFlow flow = IntegrationFlows.from(Ftp.inboundAdapter(sessionFactory())
						.preserveTimestamp(true)
						.remoteDirectory("ftpSource")
						.maxFetchSize(10)
						.scanner(scanner)
						.regexFilter(".*\\.txt$")
						.localFilename(f -> f.toUpperCase() + ".a")
						.localDirectory(getTargetLocalDirectory()),
				e -> e.id("ftpInboundAdapter").poller(Pollers.fixedDelay(100)))
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Map<?, ?> components = TestUtils.getPropertyValue(registration, "integrationFlow.integrationComponents", Map.class);
		Iterator<?> iterator = components.keySet().iterator();
		iterator.next();
		Object spcafb = iterator.next();
		assertThat(TestUtils.getPropertyValue(spcafb, "source.fileSource.scanner")).isSameAs(scanner);
		Message<?> message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getHeaders())
				.containsKeys(FileHeaders.REMOTE_HOST_PORT, FileHeaders.REMOTE_DIRECTORY, FileHeaders.REMOTE_FILE);
		Object payload = message.getPayload();
		assertThat(payload).isInstanceOf(File.class);
		File file = (File) payload;
		assertThat(file.getName()).isIn(" FTPSOURCE1.TXT.a", "FTPSOURCE2.TXT.a");
		assertThat(file.getAbsolutePath()).contains("localTarget");

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		file = (File) message.getPayload();
		assertThat(file.getName()).isIn(" FTPSOURCE1.TXT.a", "FTPSOURCE2.TXT.a");
		assertThat(file.getAbsolutePath()).contains("localTarget");

		assertThat(out.receive(10)).isNull();

		File remoteFile = new File(this.sourceRemoteDirectory, " " + prefix() + "Source1.txt");

		FileOutputStream fos = new FileOutputStream(remoteFile);
		fos.write("New content".getBytes());
		fos.close();
		remoteFile.setLastModified(System.currentTimeMillis() - 1000 * 60 * 60 * 24);

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		payload = message.getPayload();
		assertThat(payload).isInstanceOf(File.class);
		file = (File) payload;
		assertThat(file.getName()).isEqualTo(" FTPSOURCE1.TXT.a");
		assertThat(FileCopyUtils.copyToString(new FileReader(file))).isEqualTo("New content");

		MessageSource<?> source = context.getBean(FtpInboundFileSynchronizingMessageSource.class);
		assertThat(TestUtils.getPropertyValue(source, "maxFetchSize")).isEqualTo(10);

		assertThat(this.integrationManagementConfigurer.getSourceMetrics("ftpInboundAdapter.source")).isNotNull();

		registration.destroy();

		assertThat(this.integrationManagementConfigurer.getSourceMetrics("ftpInboundAdapter.source")).isNull();
	}

	@Test
	public void testFtpInboundStreamFlow() throws Exception {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlows.from(
				Ftp.inboundStreamingAdapter(new FtpRemoteFileTemplate(sessionFactory()))
						.remoteDirectory("ftpSource")
						.maxFetchSize(11)
						.regexFilter(".*\\.txt$"),
				e -> e.id("ftpInboundAdapter").poller(Pollers.fixedDelay(100)))
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Message<?> message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isIn(" ftpSource1.txt", "ftpSource2.txt");
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_HOST_PORT, String.class)).contains("localhost:");
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isIn(" ftpSource1.txt", "ftpSource2.txt");
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		MessageSource<?> source = context.getBean(FtpStreamingMessageSource.class);
		assertThat(TestUtils.getPropertyValue(source, "maxFetchSize")).isEqualTo(11);
		registration.destroy();
	}

	@Test
	public void testFtpOutboundFlow() {
		IntegrationFlow flow = f -> f
				.handle(Ftp.outboundAdapter(sessionFactory(), FileExistsMode.FAIL)
						.useTemporaryFileName(false)
						.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
						.remoteDirectory("ftpTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		Message<ByteArrayInputStream> message = MessageBuilder
				.withPayload(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		registration.getInputChannel().send(message);
		RemoteFileTemplate<FTPFile> template = new RemoteFileTemplate<>(sessionFactory());
		FTPFile[] files = template.execute(session ->
				session.list(getTargetRemoteDirectory().getName() + "/" + fileName));
		assertThat(files.length).isEqualTo(1);
		assertThat(files[0].getSize()).isEqualTo(3);

		registration.destroy();
	}

	@Test
	public void testFtpOutboundFlowWithChmod() {
		IntegrationFlow flow = f -> f
				.handle(Ftp.outboundAdapter(sessionFactory(), FileExistsMode.FAIL)
						.useTemporaryFileName(false)
						.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
						.chmod(0644)
						.remoteDirectory("ftpTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		Message<ByteArrayInputStream> message = MessageBuilder
				.withPayload(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)))
				.setHeader(FileHeaders.FILENAME, fileName)
				.build();
		registration.getInputChannel().send(message);
		RemoteFileTemplate<FTPFile> template = new RemoteFileTemplate<>(sessionFactory());
		FTPFile[] files = template.execute(session ->
				session.list(getTargetRemoteDirectory().getName() + "/" + fileName));
		assertThat(files.length).isEqualTo(1);
		assertThat(files[0].getSize()).isEqualTo(3);

		registration.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFtpMgetFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(
						Ftp.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.MGET, "payload")
								.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
								.fileExistsMode(FileExistsMode.IGNORE)
								.filterExpression("name matches 'subFtpSource|.*1.txt'")
								.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
								.localFilenameExpression("#remoteFileName.replaceFirst('ftpSource', 'localTarget')")
								.charset(StandardCharsets.UTF_8.name())
								.useTemporaryFileName(true))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String dir = "ftpSource/";
		registration.getInputChannel().send(new GenericMessage<>(dir + "*"));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();
		List<File> localFiles = (List<File>) result.getPayload();
		// should have filtered ftpSource2.txt
		assertThat(localFiles.size()).as("unexpected local files " + localFiles).isEqualTo(2);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
		}
		assertThat(localFiles.get(1).getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir + "subFtpSource");

		registration.destroy();
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class ContextConfiguration {

	}

}
