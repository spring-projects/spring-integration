/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.ftp.dsl;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.net.ftp.FTPFile;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowRegistration;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.FtpTestSupport;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 5.0
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FtpTests extends FtpTestSupport {

	@Autowired
	private IntegrationFlowContext flowContext;

	@Test
	public void testFtpInboundFlow() {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlows.from(Ftp.inboundAdapter(sessionFactory())
						.preserveTimestamp(true)
						.remoteDirectory("ftpSource")
						.regexFilter(".*\\.txt$")
						.localFilename(f -> f.toUpperCase() + ".a")
						.localDirectory(getTargetLocalDirectory()),
				e -> e.id("ftpInboundAdapter").poller(Pollers.fixedDelay(100)))
			.channel(out)
			.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Message<?> message = out.receive(10_000);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertThat(payload, instanceOf(File.class));
		File file = (File) payload;
		assertThat(file.getName(), isOneOf(" FTPSOURCE1.TXT.a", "FTPSOURCE2.TXT.a"));
		assertThat(file.getAbsolutePath(), containsString("localTarget"));

		message = out.receive(10_000);
		assertNotNull(message);
		file = (File) message.getPayload();
		assertThat(file.getName(), isOneOf(" FTPSOURCE1.TXT.a", "FTPSOURCE2.TXT.a"));
		assertThat(file.getAbsolutePath(), containsString("localTarget"));

		this.flowContext.remove(registration.getId());
	}

	@Test
	public void testFtpInboundStreamFlow() throws Exception {
		QueueChannel out = new QueueChannel();
		StandardIntegrationFlow flow = IntegrationFlows.from(
				Ftp.inboundStreamingAdapter(new FtpRemoteFileTemplate(sessionFactory()))
						.remoteDirectory("ftpSource")
						.regexFilter(".*\\.txt$"),
				e -> e.id("ftpInboundAdapter").poller(Pollers.fixedDelay(100)))
			.channel(out)
			.get();
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		Message<?> message = out.receive(10_000);
		assertNotNull(message);
		assertThat(message.getPayload(), instanceOf(InputStream.class));
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE), isOneOf(" ftpSource1.txt", "ftpSource2.txt"));
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		message = out.receive(10_000);
		assertNotNull(message);
		assertThat(message.getPayload(), instanceOf(InputStream.class));
		assertThat(message.getHeaders().get(FileHeaders.REMOTE_FILE), isOneOf(" ftpSource1.txt", "ftpSource2.txt"));
		new IntegrationMessageHeaderAccessor(message).getCloseableResource().close();

		this.flowContext.remove(registration.getId());
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
		registration.getInputChannel().send(MessageBuilder.withPayload("foo")
			.setHeader(FileHeaders.FILENAME, fileName)
			.build());
		RemoteFileTemplate<FTPFile> template = new RemoteFileTemplate<>(sessionFactory());
		FTPFile[] files = template.execute(session ->
				session.list(getTargetRemoteDirectory().getName() + "/" + fileName));
		assertEquals(1, files.length);
		assertEquals(3, files[0].getSize());

		this.flowContext.remove(registration.getId());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFtpMgetFlow() {
		QueueChannel out = new QueueChannel();
		IntegrationFlow flow = f -> f
				.handle(Ftp.outboundGateway(sessionFactory(),
						AbstractRemoteFileOutboundGateway.Command.MGET, "payload")
							.options(AbstractRemoteFileOutboundGateway.Option.RECURSIVE)
							.regexFileNameFilter("(subFtpSource|.*1.txt)")
							.localDirectoryExpression("'" + getTargetLocalDirectoryName() + "' + #remoteDirectory")
							.localFilenameExpression("#remoteFileName.replaceFirst('ftpSource', 'localTarget')"))
				.channel(out);
		IntegrationFlowRegistration registration = this.flowContext.registration(flow).register();
		String dir = "ftpSource/";
		registration.getInputChannel().send(new GenericMessage<>(dir + "*"));
		Message<?> result = out.receive(10_000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();
		// should have filtered ftpSource2.txt
		assertEquals(2, localFiles.size());

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
		assertThat(localFiles.get(1).getPath().replaceAll(Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir + "subFtpSource"));

		this.flowContext.remove(registration.getId());
	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	public static class ContextConfiguration {

	}

}
