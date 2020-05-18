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

package org.springframework.integration.sftp.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.sftp.SftpTestSupport;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.jcraft.jsch.ChannelSftp;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Joaquin Santana
 *
 * @since 5.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class SftpTests extends SftpTestSupport {

	@Autowired
	private IntegrationFlowContext flowContext;

	@Test
	public void testSftpInboundFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = IntegrationFlows
				.from(Sftp.inboundAdapter(sessionFactory())
								.preserveTimestamp(true)
								.remoteDirectory("sftpSource")
								.regexFilter(".*\\.txt$")
								.localFilenameExpression("#this.toUpperCase() + '.a'")
								.localDirectory(getTargetLocalDirectory()),
						e -> e.id("sftpInboundAdapter").poller(Pollers.fixedDelay(100)))
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Message<?> message = out.receive(10_000);
		assertThat(message).isNotNull();
		Object payload = message.getPayload();
		assertThat(payload).isInstanceOf(File.class);
		File file = (File) payload;
		assertThat(file.getName()).isIn(" SFTPSOURCE1.TXT.a", "SFTPSOURCE2.TXT.a");
		assertThat(file.getAbsolutePath()).contains("localTarget");

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		file = (File) message.getPayload();
		assertThat(file.getName()).isIn(" SFTPSOURCE1.TXT.a", "SFTPSOURCE2.TXT.a");
		assertThat(file.getAbsolutePath()).contains("localTarget");

		registration.destroy();
	}

	@Test
	public void testSftpInboundStreamFlow() throws Exception {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlows.from(
				Sftp.inboundStreamingAdapter(new SftpRemoteFileTemplate(sessionFactory()))
						.remoteDirectory("sftpSource")
						.regexFilter(".*\\.txt$"),
				e -> e.id("sftpInboundAdapter").poller(Pollers.fixedDelay(100)))
				.channel(out)
				.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Message<?> message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isIn(" sftpSource1.txt", "sftpSource2.txt");
		((InputStream) message.getPayload()).close();
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		message = out.receive(10_000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isInstanceOf(InputStream.class);
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE)).isIn(" sftpSource1.txt", "sftpSource2.txt");
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_HOST_PORT, String.class)).contains("localhost:");
		((InputStream) message.getPayload()).close();
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		registration.destroy();
	}

	@Test
	public void testSftpOutboundFlow() {
		IntegrationFlow flow = f -> f.handle(Sftp.outboundAdapter(sessionFactory(), FileExistsMode.FAIL)
				.useTemporaryFileName(false)
				.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
				.remoteDirectory("sftpTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		registration.getInputChannel().send(MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.FILENAME, fileName)
				.build());

		RemoteFileTemplate<ChannelSftp.LsEntry> template = new RemoteFileTemplate<>(sessionFactory());
		ChannelSftp.LsEntry[] files = template.execute(session ->
				session.list(getTargetRemoteDirectory().getName() + "/" + fileName));
		assertThat(files.length).isEqualTo(1);
		assertThat(files[0].getAttrs().getSize()).isEqualTo(3);

		registration.destroy();
	}


	@Test
	@DisabledOnOs(OS.WINDOWS)
	public void testSftpOutboundFlowWithChmod() {
		IntegrationFlow flow = f -> f.handle(Sftp.outboundAdapter(sessionFactory(), FileExistsMode.FAIL)
				.useTemporaryFileName(false)
				.fileNameExpression("headers['" + FileHeaders.FILENAME + "']")
				.chmod(0644)
				.remoteDirectory("sftpTarget"));
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String fileName = "foo.file";
		registration.getInputChannel().send(MessageBuilder.withPayload("foo")
				.setHeader(FileHeaders.FILENAME, fileName)
				.build());

		RemoteFileTemplate<ChannelSftp.LsEntry> template = new RemoteFileTemplate<>(sessionFactory());
		ChannelSftp.LsEntry[] files = template.execute(session ->
				session.list(getTargetRemoteDirectory().getName() + "/" + fileName));
		assertThat(files.length).isEqualTo(1);
		assertThat(files[0].getAttrs().getSize()).isEqualTo(3);
		String[] permissions = files[0].getAttrs().getPermissionsString().substring(1).replaceAll("--", "-").split("-");
		assertThat(permissions[0]).isEqualTo("rw");
		assertThat(permissions[1]).isEqualTo("r");
		assertThat(permissions[2]).isEqualTo("r");

		registration.destroy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSftpMgetFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(Sftp.outboundGateway(sessionFactory(), AbstractRemoteFileOutboundGateway.Command.MGET,
						"payload")
						.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
						.regexFileNameFilter("(subSftpSource|.*1.txt)")
						.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
						.localFilenameExpression("#remoteFileName.replaceFirst('sftpSource', 'localTarget')"))
				.channel(out);
		String dir = "sftpSource/";
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		registration.getInputChannel().send(new GenericMessage<>(dir + "*"));
		Message<?> result = out.receive(10_000);
		assertThat(result).isNotNull();
		List<File> localFiles = (List<File>) result.getPayload();
		// should have filtered sftpSource2.txt
		assertThat(localFiles.size()).isEqualTo(2);

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/")).contains(dir);
		}
		assertThat(localFiles.get(1).getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"))
				.contains(dir + "subSftpSource");

		registration.destroy();
	}

	@Test
	public void testSftpSessionCallback() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.<String>handle((p, h) -> new SftpRemoteFileTemplate(sessionFactory()).execute(s -> s.list(p)))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		registration.getInputChannel().send(new GenericMessage<>("sftpSource"));
		Message<?> receive = out.receive(10_000);
		assertThat(receive).isNotNull();
		Object payload = receive.getPayload();
		assertThat(payload).isInstanceOf(ChannelSftp.LsEntry[].class);

		assertThat(((ChannelSftp.LsEntry[]) payload).length > 0).isTrue();

		registration.destroy();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

	}

}
