/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.file.remote.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
		assertEquals(foo.mockSession, this.dsf.getSession("foo"));
		assertEquals(bar.mockSession, this.dsf.getSession("bar"));
		assertEquals(bar.mockSession, this.dsf.getSession("junk"));
		assertEquals(bar.mockSession, this.dsf.getSession());
		this.dsf.setThreadKey("foo");
		assertEquals(foo.mockSession, this.dsf.getSession("foo"));
		this.dsf.clearThreadKey();
		TestSessionFactory factory = new TestSessionFactory();
		this.sessionFactoryLocator.addSessionFactory("baz", factory);
		this.dsf.setThreadKey("baz");
		assertEquals(factory.mockSession, this.dsf.getSession("baz"));
		this.dsf.clearThreadKey();
		assertSame(factory, sessionFactoryLocator.removeSessionFactory("baz"));
	}

	@Test
	public void testFlow() throws Exception {
		in.send(new GenericMessage<String>("foo"));
		Message<?> received = out.receive(0);
		assertNotNull(received);
		verify(foo.mockSession).list("foo/");
		assertNull(TestUtils.getPropertyValue(dsf, "threadKey", ThreadLocal.class).get());
	}

	@Configuration
	@ImportResource("classpath:/org/springframework/integration/file/remote/session/delegating-session-factory-context.xml")
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
			return new DelegatingSessionFactory<String>(sff);
		}

		@Bean
		public SessionFactoryLocator<String> sessionFactoryLocator() {
			Map<Object, SessionFactory<String>> factories = new HashMap<Object, SessionFactory<String>>();
			factories.put("foo", foo());
			TestSessionFactory bar = bar();
			factories.put("bar", bar);
			SessionFactoryLocator<String> sff = new DefaultSessionFactoryLocator<String>(factories, bar);
			return sff;
		}

		@ServiceActivator(inputChannel = "c1")
		@Bean
		MessageHandler handler() {
			AbstractRemoteFileOutboundGateway<String> gateway = new AbstractRemoteFileOutboundGateway<String>(dsf(), "ls", "payload") {

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
			gateway.setOutputChannelName("c2");
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
