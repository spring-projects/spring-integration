/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Message;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromTerm;
import org.eclipse.angus.mail.imap.protocol.IMAPProtocol;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junitpioneer.jupiter.RetryingTest;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mail.support.DefaultMailHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Alexander Pinske
 * @author Dominik Simmen
 * @author Filip Hrisafov
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@ContextConfiguration(
		"classpath:org/springframework/integration/mail/config/ImapIdleChannelAdapterParserTests-context.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ImapMailReceiverNoDebugTests {

	private static final ImapSearchLoggingHandler imapSearches = new ImapSearchLoggingHandler();

	private GreenMail imapIdleServer;

	private GreenMailUser user;

	@Autowired
	private ApplicationContext context;

	static {
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
	}

	@BeforeAll
	static void setup() {
		Logger logger = LogManager.getLogManager().getLogger("");
		logger.setLevel(Level.ALL);
		logger.addHandler(imapSearches);
	}

	@AfterAll
	static void teardown() {
		LogManager.getLogManager().getLogger("").removeHandler(imapSearches);
	}

	@BeforeEach
	void startImapServer() {
		imapSearches.searches.clear();
		imapSearches.stores.clear();
		ServerSetup imap = ServerSetupTest.IMAP.verbose(true).dynamicPort();
		imap.setServerStartupTimeout(10000);
		imap.setReadTimeout(10000);
		imapIdleServer = new GreenMail(imap);
		user = imapIdleServer.setUser("user", "pw");
		imapIdleServer.start();
	}

	@AfterEach
	void stopImapServer() {
		imapIdleServer.stop();
	}

	@RetryingTest(10)
	public void testIdleWithServerCustomSearch() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setSearchTermStrategy((supportedFlags, folder) -> {
			try {
				FromTerm fromTerm = new FromTerm(new InternetAddress("bar@baz"));
				return new AndTerm(fromTerm, new FlagTerm(new Flags(Flag.SEEN), false));
			}
			catch (AddressException e) {
				throw new RuntimeException(e);
			}
		});
		testIdleWithServerGuts(receiver, false);
	}

	@RetryingTest(10)
	public void testIdleWithMessageMapping() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true);
	}

	@RetryingTest(10)
	public void testIdleWithServerDefaultSearchSimple() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setSimpleContent(true);
		testIdleWithServerGuts(receiver, false, true);
		assertThat(imapSearches.searches.get(0)).contains("testSIUserFlag");
	}

	@RetryingTest(10)
	public void testIdleWithMessageMappingSimple() throws Exception {
		ImapMailReceiver receiver =
				new ImapMailReceiver("imap://user:pw@localhost:" + imapIdleServer.getImap().getPort() + "/INBOX");
		receiver.setSimpleContent(true);
		receiver.setHeaderMapper(new DefaultMailHeaderMapper());
		testIdleWithServerGuts(receiver, true, true);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped) throws Exception {
		testIdleWithServerGuts(receiver, mapped, false);
	}

	public void testIdleWithServerGuts(ImapMailReceiver receiver, boolean mapped, boolean simple) throws Exception {
		receiver.setMaxFetchSize(1);
		receiver.setShouldDeleteMessages(false);
		receiver.setShouldMarkMessagesAsRead(true);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		setUpScheduler(receiver, taskScheduler);
		receiver.setUserFlag("testSIUserFlag");
		receiver.afterPropertiesSet();
		ImapIdleChannelAdapter adapter = new ImapIdleChannelAdapter(receiver);
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		adapter.setReconnectDelay(10);
		adapter.afterPropertiesSet();
		adapter.start();
		MimeMessage message =
				GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "Test Email", "foo\r\n",
						imapIdleServer.getImap().getServerSetup());
		message.setRecipients(Message.RecipientType.CC, "a@b, c@d");
		message.setRecipients(Message.RecipientType.BCC, "e@f, g@h");
		user.deliver(message);
		if (!mapped) {
			@SuppressWarnings("unchecked")
			org.springframework.messaging.Message<MimeMessage> received =
					(org.springframework.messaging.Message<MimeMessage>) channel.receive(20000);
			assertThat(received).isNotNull();
			assertThat(received.getPayload().getReceivedDate()).isNotNull();
			assertThat(received.getPayload().getLineCount() > -1).isTrue();
			if (simple) {
				assertThat(received.getPayload().getContent())
						.isEqualTo("foo\r\n");
			}
			else {
				assertThat(received.getPayload().getContent())
						.isEqualTo("foo");
			}
		}
		else {
			org.springframework.messaging.Message<?> received = channel.receive(20000);
			assertThat(received).isNotNull();
			MessageHeaders headers = received.getHeaders();
			assertThat(headers.get(MailHeaders.RAW_HEADERS)).isNotNull();
			assertThat(headers.get(MailHeaders.CONTENT_TYPE)).isEqualTo("text/plain; charset=us-ascii");
			assertThat(headers.get(MessageHeaders.CONTENT_TYPE)).isEqualTo(MimeTypeUtils.TEXT_PLAIN_VALUE);
			assertThat(headers.get(MailHeaders.FROM)).isEqualTo("Bar <bar@baz>");
			String[] toHeader = headers.get(MailHeaders.TO, String[].class);
			assertThat(toHeader).isNotEmpty();
			assertThat(toHeader[0]).isEqualTo("Foo <foo@bar>");
			assertThat(Arrays.toString(headers.get(MailHeaders.CC, String[].class))).isEqualTo("[a@b, c@d]");
			assertThat(Arrays.toString(headers.get(MailHeaders.BCC, String[].class))).isEqualTo("[e@f, g@h]");
			assertThat(headers.get(MailHeaders.SUBJECT)).isEqualTo("Test Email");
			if (simple) {
				assertThat(received.getPayload()).isEqualTo("foo\r\n");
			}
			else {
				assertThat(received.getPayload()).isEqualTo("foo");
			}
		}
		user.deliver(GreenMailUtil.createTextEmail("Foo <foo@bar>", "Bar <bar@baz>", "subject", "body\r\n",
				imapIdleServer.getImap().getServerSetup()));
		assertThat(channel.receive(30000)).isNotNull(); // new message after idle
		assertThat(channel.receive(100)).isNull(); // no new message after second and third idle

		adapter.stop();
		taskScheduler.shutdown();
		assertThat(imapSearches.stores.get(0)).contains("testSIUserFlag");
	}

	private void setUpScheduler(ImapMailReceiver mailReceiver, ThreadPoolTaskScheduler taskScheduler) {
		taskScheduler.setPoolSize(5);
		taskScheduler.initialize();
		BeanFactory bf = mock(BeanFactory.class);
		given(bf.containsBean("taskScheduler")).willReturn(true);
		given(bf.getBean("taskScheduler", TaskScheduler.class)).willReturn(taskScheduler);
		mailReceiver.setBeanFactory(bf);
	}

	private static class ImapSearchLoggingHandler extends Handler {

		private final List<String> searches = new ArrayList<>();

		private final List<String> stores = new ArrayList<>();

		private static final String SEARCH = " SEARCH ";

		private static final String STORE = " STORE ";

		@Override
		public void publish(LogRecord record) {
			if (IMAPProtocol.class.getPackageName().equals(record.getLoggerName())) {
				String message = record.getMessage();
				if (!message.startsWith("*")) {
					if (message.contains(SEARCH) && !message.contains(" OK ")) {
						searches.add(message.substring(message.indexOf(SEARCH) + SEARCH.length()));
					}
					else if (message.contains(STORE) && !message.contains(" OK ")) {
						stores.add(message.substring(message.indexOf(STORE) + STORE.length()));
					}
				}
			}
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() throws SecurityException {
		}

	}

}
