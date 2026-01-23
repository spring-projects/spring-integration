/*
 * Copyright 2014-present the original author or authors.
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

import java.io.Closeable;
import java.util.Properties;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromTerm;
import jakarta.mail.search.SearchTerm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.integration.mail.inbound.ImapIdleChannelAdapter;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class MailTests {

	private static GreenMail mailServer;

	@BeforeAll
	public static void setup() {
		ServerSetup smtp = ServerSetupTest.SMTP.dynamicPort();
		smtp.setServerStartupTimeout(10000);
		ServerSetup imap = ServerSetupTest.IMAP.dynamicPort();
		imap.setServerStartupTimeout(10000);
		ServerSetup pop3 = ServerSetupTest.POP3.dynamicPort();
		pop3.setServerStartupTimeout(10000);
		mailServer = new GreenMail(new ServerSetup[] {smtp, pop3, imap});
		mailServer.setUser("bar@baz", "smtpuser", "pw");
		mailServer.setUser("popuser", "pw");
		mailServer.setUser("imapuser", "pw");
		mailServer.setUser("imapidleuser", "pw");
		mailServer.start();
	}

	@AfterAll
	static void tearDown() {
		mailServer.stop();
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
		assertThat(TestUtils.<String>getPropertyValue(this.sendMailHandler, "mailSender.host")).isEqualTo("localhost");

		Properties javaMailProperties =
				TestUtils.getPropertyValue(this.sendMailHandler, "mailSender.javaMailProperties");
		assertThat(javaMailProperties.getProperty("mail.debug")).isEqualTo("false");

		this.sendMailChannel.send(MessageBuilder.withPayload("foo").build());

		mailServer.waitForIncomingEmail(10000, 1);

		assertThat(mailServer.getReceivedMessagesForDomain("baz").length > 0).isTrue();
		MimeMessage message = mailServer.getReceivedMessagesForDomain("baz")[0];
		assertThat(message.getFrom()).containsOnly(new InternetAddress("foo@bar"));
		assertThat(message.getRecipients(RecipientType.TO)).containsOnly(new InternetAddress("bar@baz"));
		assertThat(message.getSubject()).isEqualTo("foo");
		assertThat(message.getContent()).asString().isEqualTo("foo");

	}

	@Test
	public void testPop3() throws Exception {
		MimeMessage mimeMessage =
				GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>, Bar2 <bar2@baz>", "Test Email",
						"foo\r\n", mailServer.getPop3().getServerSetup());
		mimeMessage.setRecipients(RecipientType.CC, "a@b, c@d");
		mimeMessage.setRecipients(RecipientType.BCC, "e@f, g@h");
		mailServer.getUserManager().getUser("popuser").deliver(mimeMessage);

		Message<?> message = this.pop3Channel.receive(10000);
		assertThat(message).isNotNull();
		MessageHeaders headers = message.getHeaders();
		assertThat(headers.get(MailHeaders.TO, String[].class)).containsExactly("Foo <foo@bar>");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>,Bar2 <bar2@baz>");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
		assertThat(message.getPayload()).isEqualTo("foo\r\n");
		assertThat(message.getHeaders()).doesNotContainKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE);
	}

	@Test
	public void testImap() throws Exception {
		MimeMessage mimeMessage =
				GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "Test Email", "foo\r\n",
						mailServer.getImap().getServerSetup());
		mimeMessage.setRecipients(RecipientType.CC, "a@b, c@d");
		mimeMessage.setRecipients(RecipientType.BCC, "e@f, g@h");
		mailServer.getUserManager().getUser("imapuser").deliver(mimeMessage);

		Message<?> message = this.imapChannel.receive(10000);
		assertThat(message).isNotNull();
		MimeMessage mm = (MimeMessage) message.getPayload();
		assertThat(mm.getRecipients(RecipientType.TO)[0].toString()).isEqualTo("Foo <foo@bar>");
		assertThat(mm.getFrom()[0].toString()).isEqualTo("Bar <bar@baz>");
		assertThat(mm.getSubject()).isEqualTo("Test Email");
		assertThat(mm.getContent()).isEqualTo("foo\r\n");
		assertThat(message.getHeaders()).containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE);
		message.getHeaders().get(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, Closeable.class).close();
	}

	@Test
	public void testImapIdle() throws Exception {
		MimeMessage mimeMessage =
				GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "Test Email", "foo\r\n",
						mailServer.getImap().getServerSetup());
		mimeMessage.setRecipients(RecipientType.CC, "a@b, c@d");
		mimeMessage.setRecipients(RecipientType.BCC, "e@f, g@h");
		mailServer.getUserManager().getUser("imapidleuser").deliver(mimeMessage);

		Message<?> message = this.imapIdleChannel.receive(10000);
		assertThat(message).isNotNull();
		MessageHeaders headers = message.getHeaders();
		assertThat(headers.get(MailHeaders.TO, String[].class)).containsExactly("Foo <foo@bar>");
		assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>");
		assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
		assertThat(message.getPayload()).isEqualTo("foo\r\n");
		assertThat(message.getHeaders()).containsKey(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE);
		this.imapIdleAdapter.stop();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.imapIdleAdapter, "shouldReconnectAutomatically"))
				.isFalse();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow sendMailFlow() {
			return IntegrationFlow.from("sendMailChannel")
					.enrichHeaders(Mail.headers()
							.subjectFunction(m -> "foo")
							.from("foo@bar")
							.toFunction(m -> new String[] {"bar@baz"}))
					.handle(Mail.outboundAdapter("localhost")
									.port(mailServer.getSmtp().getPort())
									.credentials("smtpuser", "pw")
									.protocol("smtp")
									.javaMailProperties(p -> p.put("mail.debug", "false")),
							e -> e.id("sendMailEndpoint"))
					.get();
		}

		@Bean
		public IntegrationFlow pop3MailFlow() {
			return IntegrationFlow
					.from(Mail.pop3InboundAdapter("localhost", mailServer.getPop3().getPort(), "popuser", "pw")
									.javaMailProperties(p -> p.put("mail.debug", "false"))
									.autoCloseFolder(true)
									.headerMapper(mailHeaderMapper()),
							e -> e.autoStartup(true).poller(p -> p.fixedDelay(1000)))
					.enrichHeaders(s -> s.headerExpressions(c -> c.put(MailHeaders.SUBJECT, "payload.subject")
							.put(MailHeaders.FROM, "payload.from[0].toString()")))
					.channel(MessageChannels.queue("pop3Channel"))
					.get();
		}

		@Bean
		public IntegrationFlow imapMailFlow() {
			return IntegrationFlow
					.from(Mail.imapInboundAdapter("imap://imapuser:pw@localhost:" + mailServer.getImap().getPort() + "/INBOX")
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
			return IntegrationFlow
					.from(Mail.imapIdleAdapter("imap://imapidleuser:pw@localhost:" + mailServer.getImap().getPort() + "/INBOX")
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
