/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.remote.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.handler.advice.ContextHolderRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
public class DelegatingSessionFactoryTests {

	@Autowired
	TestSessionFactory foo;

	@Autowired
	TestSessionFactory bar;

	@Autowired
	DelegatingSessionFactory<String> dsf;

	@Autowired
	MessageChannel in;

	@Autowired
	PollableChannel out;

	@Autowired
	DefaultSessionFactoryLocator<String> sessionFactoryLocator;

	@Test
	public void testDelegates() {
		assertThat(this.dsf.getSession("foo")).isEqualTo(foo.mockSession);
		assertThat(this.dsf.getSession("bar")).isEqualTo(bar.mockSession);
		assertThat(this.dsf.getSession("junk")).isEqualTo(bar.mockSession);
		assertThat(this.dsf.getSession()).isEqualTo(bar.mockSession);
		this.dsf.setThreadKey("foo");
		assertThat(this.dsf.getSession("foo")).isEqualTo(foo.mockSession);
		this.dsf.clearThreadKey();
		TestSessionFactory factory = new TestSessionFactory();
		this.sessionFactoryLocator.addSessionFactory((Object) "baz", factory);
		this.dsf.setThreadKey("baz");
		assertThat(this.dsf.getSession("baz")).isEqualTo(factory.mockSession);
		this.dsf.clearThreadKey();
		assertThat(sessionFactoryLocator.removeSessionFactory("baz")).isSameAs(factory);
	}

	@Test
	public void testFlow() throws Exception {
		given(foo.mockSession.list(anyString()))
				.willReturn(new String[0]);
		in.send(new GenericMessage<>("foo"));
		Message<?> received = out.receive(0);
		assertThat(received).isNotNull();
		verify(foo.mockSession).list("foo/");
		assertThat(TestUtils.getPropertyValue(dsf, "threadKey", ThreadLocal.class).get()).isNull();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		TestSessionFactory foo() {
			return new TestSessionFactory();
		}

		@Bean
		TestSessionFactory bar() {
			return new TestSessionFactory();
		}

		@Bean
		DelegatingSessionFactory<String> dsf() {
			SessionFactoryLocator<String> sff = sessionFactoryLocator();
			return new DelegatingSessionFactory<>(sff);
		}

		@Bean
		public SessionFactoryLocator<String> sessionFactoryLocator() {
			Map<Object, SessionFactory<String>> factories = new HashMap<>();
			factories.put("foo", foo());
			TestSessionFactory bar = bar();
			factories.put("bar", bar);
			return new DefaultSessionFactoryLocator<>(factories, bar);
		}

		@Bean
		QueueChannel out() {
			return new QueueChannel();
		}

		@Bean
		ContextHolderRequestHandlerAdvice contextHolderRequestHandlerAdvice(DelegatingSessionFactory<String> dsf) {
			return new ContextHolderRequestHandlerAdvice(Message::getPayload, dsf::setThreadKey, dsf::clearThreadKey);
		}

		@ServiceActivator(inputChannel = "in", adviceChain = "contextHolderRequestHandlerAdvice")
		@Bean
		MessageHandler handler() {
			AbstractRemoteFileOutboundGateway<String> gateway =
					new AbstractRemoteFileOutboundGateway<>(dsf(), "ls", "payload") {

						@Override
						protected boolean isDirectory(String file) {
							return false;
						}

						@Override
						protected boolean isLink(String file) {
							return false;
						}

						@Override
						protected String getFilename(String file) {
							return file;
						}

						@Override
						protected String getFilename(AbstractFileInfo<String> file) {
							return file.getFilename();
						}

						@Override
						protected long getModified(String file) {
							return 0;
						}

						@Override
						protected List<AbstractFileInfo<String>> asFileInfoList(Collection<String> files) {
							return null;
						}

						@Override
						protected String enhanceNameWithSubDirectory(String file, String directory) {
							return null;
						}

					};
			gateway.setOutputChannelName("out");
			gateway.setOptions("-1");
			return gateway;
		}

	}

	private static class TestSessionFactory implements SessionFactory<String> {

		@SuppressWarnings("unchecked")
		private final Session<String> mockSession = mock(Session.class);

		@Override
		public Session<String> getSession() {
			return this.mockSession;
		}

	}

}
