/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.security.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.integration.security.TestHandler;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.SecuredChannel;
import org.springframework.integration.security.context.SecurityContextCleanupAdvice;
import org.springframework.integration.security.context.SecurityContextCleanupChannelInterceptor;
import org.springframework.integration.security.context.SecurityContextPropagationChannelInterceptor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChannelSecurityInterceptorSecuredChannelAnnotationTests {

	@Autowired
	MessageChannel securedChannel;

	@Autowired
	MessageChannel securedChannel2;

	@Autowired
	MessageChannel unsecuredChannel;

	@Autowired
	@Qualifier("securedChannelQueue")
	MessageChannel securedChannelQueue;

	@Autowired
	@Qualifier("resultChannel")
	PollableChannel resultChannel;

	@Autowired
	@Qualifier("securedChannelExecutor")
	MessageChannel securedChannelExecutor;

	@Autowired
	@Qualifier("cleanupSecurityContextChannel")
	ChannelInterceptorAware cleanupSecurityContextChannel;

	@Autowired
	TestHandler testConsumer;


	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}


	@Test(expected = AccessDeniedException.class)
	public void testSecuredWithNotEnoughPermission() {
		login("bob", "bobspassword", "ROLE_ADMINA");
		securedChannel.send(new GenericMessage<String>("test"));
	}

	@Test
	public void testSecuredWithPermission() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		securedChannel.send(new GenericMessage<String>("test"));
		securedChannel2.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 2, testConsumer.sentMessages.size());
	}

	@Test(expected = AccessDeniedException.class)
	public void testSecuredWithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		securedChannel.send(new GenericMessage<String>("test"));
	}

	@Test(expected = AccessDeniedException.class)
	public void testSecured2WithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		securedChannel2.send(new GenericMessage<String>("test"));
	}

	@Test(expected = AuthenticationException.class)
	public void testSecuredWithoutAuthenticating() {
		securedChannel.send(new GenericMessage<String>("test"));
	}

	@Test
	public void testUnsecuredAsAdmin() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		unsecuredChannel.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	public void testUnsecuredAsUser() {
		login("bob", "bobspassword", "ROLE_USER");
		unsecuredChannel.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	public void testUnsecuredWithoutAuthenticating() {
		unsecuredChannel.send(new GenericMessage<String>("test"));
		assertEquals("Wrong size of message list in target", 1, testConsumer.sentMessages.size());
	}

	@Test
	public void testSecurityContextPropagationQueueChannel() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		List<ChannelInterceptor> channelInterceptors =
				((ChannelInterceptorAware) this.securedChannelQueue).getChannelInterceptors();


		assertTrue(channelInterceptors.size() == 1);
		assertThat(channelInterceptors.get(0), instanceOf(SecurityContextPropagationChannelInterceptor.class));
		this.securedChannelQueue.send(new GenericMessage<String>("test"));
		Message<?> receive = this.resultChannel.receive(10000);
		assertNotNull(receive);

		assertNotEquals(Thread.currentThread().getId(), receive.getHeaders().get("threadId"));

		// Without SecurityContext propagation we end up here with: AuthenticationCredentialsNotFoundException
		assertEquals(1, testConsumer.sentMessages.size());

		assertThat(receive.getPayload(), instanceOf(SecurityContext.class));
		SecurityContext securityContext = (SecurityContext) receive.getPayload();
		// we don't get here an empty SecurityContext from the taskScheduler Thread because
		// the SecurityContextCleanupChannelInterceptor bypasses DirectChannel for its cleanup functionality
		assertNotNull(securityContext.getAuthentication());
	}

	@Test
	public void testSecurityContextPropagationExecutorChannel() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		ChannelInterceptor cleanupInterceptor = new ChannelInterceptorAdapter() {

			@Override
			public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
				SecurityContextCleanupAdvice.cleanup();
			}

		};
		this.cleanupSecurityContextChannel.addInterceptor(cleanupInterceptor);
		this.securedChannelExecutor.send(new GenericMessage<String>("test"));
		Message<?> receive = this.resultChannel.receive(10000);
		assertNotNull(receive);

		assertNotEquals(Thread.currentThread().getId(), receive.getHeaders().get("threadId"));

		// Without SecurityContext propagation we end up here with: AuthenticationCredentialsNotFoundException
		assertEquals(1, testConsumer.sentMessages.size());

		assertThat(receive.getPayload(), instanceOf(SecurityContext.class));
		SecurityContext securityContext = (SecurityContext) receive.getPayload();
		// Without SecurityContext cleanup we don't get here an empty SecurityContext from the Executor Thread
		assertNull(securityContext.getAuthentication());
		this.cleanupSecurityContextChannel.removeInterceptor(cleanupInterceptor);
	}


	private void login(String username, String password, String... roles) {
		SecurityContext context = SecurityTestUtils.createContext(username, password, roles);
		SecurityContextHolder.setContext(context);
	}


	@Configuration
	@EnableIntegration
	@ImportResource("classpath:org/springframework/integration/security/config/commonSecurityConfiguration.xml")
	public static class ContextConfiguration {

		@Bean
		@SecuredChannel(interceptor = "channelSecurityInterceptor", sendAccess = {"ROLE_ADMIN", "ROLE_PRESIDENT"})
		public SubscribableChannel securedChannel() {
			return new DirectChannel();
		}

		@Bean
		@SecuredChannel(interceptor = "channelSecurityInterceptor", sendAccess = {"ROLE_ADMIN", "ROLE_PRESIDENT"})
		public SubscribableChannel securedChannel2() {
			return new DirectChannel();
		}

		@Bean
		public SubscribableChannel unsecuredChannel() {
			return new DirectChannel();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = {"securedChannelQueue", "securedChannelExecutor"})
		public ChannelInterceptor securityContextPropagationInterceptor() {
			return new SecurityContextPropagationChannelInterceptor();
		}

		@Bean
		@SecuredChannel(interceptor = "channelSecurityInterceptor", sendAccess = {"ROLE_ADMIN", "ROLE_PRESIDENT"})
		public PollableChannel securedChannelQueue() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "securedChannelQueue", poller = @Poller(fixedDelay = "1000"))
		public MessageHandler recipientListRouter() {
			RecipientListRouter router = new RecipientListRouter();
			router.setChannels(
					Arrays.<MessageChannel>asList(securedChannel(),
							cleanupSecurityContextChannel(),
							checkSecurityContextChannel()));
			return router;
		}

		@Bean
		@BridgeTo("nullChannel")
		public SubscribableChannel cleanupSecurityContextChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.addInterceptor(new SecurityContextCleanupChannelInterceptor());
			return directChannel;
		}

		@Bean
		public SubscribableChannel checkSecurityContextChannel() {
			return new DirectChannel();
		}

		@ServiceActivator(inputChannel = "checkSecurityContextChannel", outputChannel = "resultChannel")
		public Message<SecurityContext> checkSecurityContext(Object payload) {
			return MessageBuilder.withPayload(SecurityContextHolder.getContext())
					.setHeader("threadId", Thread.currentThread().getId())
					.build();
		}

		@Bean
		public PollableChannel resultChannel() {
			return new QueueChannel();
		}

		@Bean
		@SecuredChannel(interceptor = "channelSecurityInterceptor", sendAccess = {"ROLE_ADMIN", "ROLE_PRESIDENT"})
		@BridgeTo("securedChannelQueue")
		public SubscribableChannel securedChannelExecutor() {
			return new ExecutorChannel(Executors.newSingleThreadExecutor());
		}


		@Bean
		public TestHandler testHandler() {
			TestHandler testHandler = new TestHandler();
			this.securedChannel().subscribe(testHandler);
			this.securedChannel2().subscribe(testHandler);
			this.unsecuredChannel().subscribe(testHandler);
			return testHandler;
		}

		@Bean
		public ChannelSecurityInterceptor channelSecurityInterceptor(AuthenticationManager authenticationManager,
				AccessDecisionManager accessDecisionManager) {
			ChannelSecurityInterceptor channelSecurityInterceptor = new ChannelSecurityInterceptor();
			channelSecurityInterceptor.setAuthenticationManager(authenticationManager);
			channelSecurityInterceptor.setAccessDecisionManager(accessDecisionManager);
			return channelSecurityInterceptor;
		}

	}

}
