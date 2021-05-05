/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.mail.dsl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.search.AndTerm;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.SearchTerm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.mail.TestMailServer;
import org.springframework.integration.test.mail.TestMailServer.ImapServer;
import org.springframework.integration.test.mail.TestMailServer.Pop3Server;
import org.springframework.integration.test.mail.TestMailServer.SmtpServer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class MailTests {

	private static final SmtpServer smtpServer = TestMailServer.smtp(0);

	private static final Pop3Server pop3Server = TestMailServer.pop3(0);

	private static final ImapServer imapServer = TestMailServer.imap(0);

	private static final ImapServer imapIdleServer = TestMailServer.imap(0);


	@BeforeAll
	public static void setup() throws InterruptedException {
		int n = 0;
		while (n++ < 100 && (!smtpServer.isListening() || !pop3Server.isListening()
				|| !imapServer.isListening()) || !imapIdleServer.isListening()) {
			Thread.sleep(100);
		}
		assertThat(n < 100).isTrue();
	}

	@Autowired
	private MessageChannel sendMailChannel;

	@Autowired
	@Qualifier("sendMailEndpoint.handler")
	private MessageHandler sendMailHandler;

	@Autowired
	private PollableChannel pop3Channel;

	@Autowired
	private PollableChannel imapChannel;

	@Autowired
	private PollableChannel imapIdleChannel;

	@Autowired
	private ImapIdleChannelAdapter imapIdleAdapter;

	@Test
	public void testSmtp() throws Exception {
		assertThat(TestUtils.getPropertyValue(this.sendMailHandler, "mailSender.host")).isEqualTo("localhost");

		Properties javaMailProperties = TestUtils.getPropertyValue(this.sendMailHandler,
				"mailSender.javaMailProperties", Properties.class);
		assertThat(javaMailProperties.getProperty("mail.debug")).isEqualTo("false");

		this.sendMailChannel.send(MessageBuilder.withPayload("foo").build());

		int n = 0;
		while (n++ < 100 && smtpServer.getMessages().size() == 0) {
			Thread.sleep(100);
		}

		assertThat(smtpServer.getMessages().size() > 0).isTrue();
		String message = smtpServer.getMessages().get(0);
		assertThat(message).endsWith("foo\n");
		assertThat(message).contains("foo@bar");
		assertThat(message).contains("bar@baz");
		assertThat(message).contains("user:user");
		assertThat(message).contains("password:pw");

	}

	@Test
	public void testPop3() throws IOException {
		Message<?> message = this.pop3Channel.receive(10000);
		assertThat(message).isNotNull();
		MessageHeaders headers = message.getHeaders();
		assertThat(headers.get(MailHeaders.TO, String[].class)).containsExactly("Foo <foo@bar>");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>,Bar2 <bar2@baz>");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
		assertThat(message.getPayload()).isEqualTo("foo\r\n\r\n");
		assertThat(message.getHeaders().containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE)).isTrue();
		message.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class).close();
	}

	@Test
	public void testImap() throws Exception {
		Message<?> message = this.imapChannel.receive(10000);
		assertThat(message).isNotNull();
		MimeMessage mm = (MimeMessage) message.getPayload();
		assertThat(mm.getRecipients(RecipientType.TO)[0].toString()).isEqualTo("Foo <foo@bar>");
		assertThat(mm.getFrom()[0].toString()).isEqualTo("Bar <bar@baz>");
		assertThat(mm.getSubject()).isEqualTo("Test Email");
		assertThat(mm.getContent()).isEqualTo(TestMailServer.MailServer.MailHandler.BODY + "\r\n");
		assertThat(message.getHeaders().containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE)).isTrue();
		message.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class).close();
	}

	@Test
	public void testImapIdle() {
		Message<?> message = this.imapIdleChannel.receive(10000);
		assertThat(message).isNotNull();
		MessageHeaders headers = message.getHeaders();
		assertThat(headers.get(MailHeaders.TO, String[].class)).containsExactly("Foo <foo@bar>");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
		assertThat(message.getPayload()).isEqualTo(TestMailServer.MailServer.MailHandler.BODY + "\r\n");
		assertThat(message.getHeaders().containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE)).isTrue();
		this.imapIdleAdapter.stop();
		assertThat(TestUtils.getPropertyValue(this.imapIdleAdapter, "shouldReconnectAutomatically", Boolean.class))
				.isFalse();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public SmartLifecycle serverStopper() {
			return new SmartLifecycle() {

				@Override
				public int getPhase() {
					return Integer.MAX_VALUE;
				}

				@Override
				public void stop() {
					smtpServer.stop();
					pop3Server.stop();
					imapServer.stop();
					imapIdleServer.stop();
				}

				@Override
				public void start() {
				}

				@Override
				public boolean isRunning() {
					return true;
				}

			};
		}

		@Bean
		public IntegrationFlow sendMailFlow() {
			return IntegrationFlows.from("sendMailChannel")
					.enrichHeaders(Mail.headers()
							.subjectFunction(m -> "foo")
							.from("foo@bar")
							.toFunction(m -> new String[]{ "bar@baz" }))
					.handle(Mail.outboundAdapter("localhost")
									.port(smtpServer.getPort())
									.credentials("user", "pw")
									.protocol("smtp")
									.javaMailProperties(p -> p.put("mail.debug", "false")),
							e -> e.id("sendMailEndpoint"))
					.get();
		}

		@Bean
		public IntegrationFlow pop3MailFlow() {
			return IntegrationFlows
					.from(Mail.pop3InboundAdapter("localhost", pop3Server.getPort(), "user", "pw")
									.javaMailProperties(p -> p.put("mail.debug", "false"))
									.autoCloseFolder(false)
									.headerMapper(mailHeaderMapper()),
							e -> e.autoStartup(true).poller(p -> p.fixedDelay(1000)))
					.enrichHeaders(s -> s.headerExpressions(c -> c.put(MailHeaders.SUBJECT, "payload.subject")
							.put(MailHeaders.FROM, "payload.from[0].toString()")))
					.channel(MessageChannels.queue("pop3Channel"))
					.get();
		}

		@Bean
		public IntegrationFlow imapMailFlow() {
			return IntegrationFlows
					.from(Mail.imapInboundAdapter("imap://user:pw@localhost:" + imapServer.getPort() + "/INBOX")
									.searchTermStrategy(this::fromAndNotSeenTerm)
									.userFlag("testSIUserFlag")
									.autoCloseFolder(false)
									.simpleContent(true)
									.javaMailProperties(p -> p.put("mail.debug", "false")),
							e -> e.autoStartup(true)
									.poller(p -> p.fixedDelay(1000)))
					.channel(MessageChannels.queue("imapChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow imapIdleFlow() {
			return IntegrationFlows
					.from(Mail.imapIdleAdapter("imap://user:pw@localhost:" + imapIdleServer.getPort() + "/INBOX")
							.autoStartup(true)
							.searchTermStrategy(this::fromAndNotSeenTerm)
							.userFlag("testSIUserFlag")
							.autoCloseFolder(false)
							.javaMailProperties(p -> p.put("mail.debug", "false")
									.put("mail.imap.connectionpoolsize", "5"))
							.shouldReconnectAutomatically(false)
							.simpleContent(true)
							.headerMapper(mailHeaderMapper()))
					.channel(MessageChannels.queue("imapIdleChannel"))
					.get();
		}

		@Bean
		public HeaderMapper<MimeMessage> mailHeaderMapper() {
			return new DefaultMailHeaderMapper();
		}

		private SearchTerm fromAndNotSeenTerm(Flags supportedFlags, Folder folder) {
			try {
				FromTerm fromTerm = new FromTerm(new InternetAddress("bar@baz"));
				return new AndTerm(fromTerm, new FlagTerm(new Flags(Flags.Flag.SEEN), false));
			}
			catch (AddressException e) {
				throw new RuntimeException(e);
			}

		}

	}

}
