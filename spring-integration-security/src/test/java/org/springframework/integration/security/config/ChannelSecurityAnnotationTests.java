/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.security.config;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.security.SecurityTestUtils;
import org.springframework.integration.security.TestHandler;
import org.springframework.integration.security.channel.SecurityContextPropagationChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChannelSecurityAnnotationTests {

	@Autowired
	MessageChannel securedChannel;

	@Autowired
	MessageChannel securedChannel2;

	@Autowired
	MessageChannel unsecuredChannel;

	@Autowired
	@Qualifier("queueChannel")
	MessageChannel queueChannel;

	@Autowired
	@Qualifier("securedChannelQueue")
	PollableChannel securedChannelQueue;

	@Autowired
	@Qualifier("executorChannel")
	MessageChannel executorChannel;

	@Autowired
	@Qualifier("publishSubscribeChannel")
	PublishSubscribeChannel publishSubscribeChannel;

	@Autowired
	@Qualifier("securedChannelQueue2")
	PollableChannel securedChannelQueue2;

	@Autowired
	@Qualifier("errorChannel")
	PollableChannel errorChannel;

	@Autowired
	TestHandler testConsumer;

	@Autowired
	TestGateway testGateway;

	@AfterEach
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}


	@Test
	public void testSecuredWithNotEnoughPermission() {
		login("bob", "bobspassword", "ROLE_ADMINA");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannel.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void testSecuredWithPermission() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		securedChannel.send(new GenericMessage<>("test"));
		securedChannel2.send(new GenericMessage<>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(2);
	}

	@Test
	public void testSecuredWithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannel.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void testSecured2WithoutPermision() {
		login("bob", "bobspassword", "ROLE_USER");
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannel2.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AccessDeniedException.class);
	}

	@Test
	public void testSecuredWithoutAuthenticating() {
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> this.securedChannel2.send(new GenericMessage<>("test")))
				.withRootCauseExactlyInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void testUnsecuredAsAdmin() {
		login("bob", "bobspassword", "ROLE_ADMIN");
		unsecuredChannel.send(new GenericMessage<>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(1);
	}

	@Test
	public void testUnsecuredAsUser() {
		login("bob", "bobspassword", "ROLE_USER");
		unsecuredChannel.send(new GenericMessage<>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(1);
	}

	@Test
	public void testUnsecuredWithoutAuthenticating() {
		unsecuredChannel.send(new GenericMessage<>("test"));
		assertThat(testConsumer.sentMessages.size()).as("Wrong size of message list in target").isEqualTo(1);
	}

	@Test
	public void testSecurityContextPropagationQueueChannel() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		this.queueChannel.send(new GenericMessage<>("test"));
		Message<?> receive = this.securedChannelQueue.receive(10000);
		assertThat(receive).isNotNull();

		SecurityContextHolder.clearContext();

		this.queueChannel.send(new GenericMessage<>("test"));
		Message<?> errorMessage = this.errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		Object payload = errorMessage.getPayload();
		assertThat(payload).isInstanceOf(MessageDeliveryException.class);
		assertThat(((MessageDeliveryException) payload).getCause())
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void testSecurityContextPropagationExecutorChannel() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		this.executorChannel.send(new GenericMessage<>("test"));
		Message<?> receive = this.securedChannelQueue.receive(10000);
		assertThat(receive).isNotNull();

		SecurityContextHolder.clearContext();

		this.executorChannel.send(new GenericMessage<>("test"));
		Message<?> errorMessage = this.errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		Object payload = errorMessage.getPayload();
		assertThat(payload).isInstanceOf(MessageDeliveryException.class);
		assertThat(((MessageDeliveryException) payload).getCause())
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void testSecurityContextPropagationPublishSubscribeChannel() {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");

		this.publishSubscribeChannel.send(new GenericMessage<>("test"));

		Message<?> receive = this.securedChannelQueue.receive(10000);
		assertThat(receive).isNotNull();
		IntegrationMessageHeaderAccessor headerAccessor = new IntegrationMessageHeaderAccessor(receive);
		assertThat(headerAccessor.getSequenceNumber()).isEqualTo(0);

		receive = this.securedChannelQueue2.receive(10000);
		assertThat(receive).isNotNull();
		headerAccessor = new IntegrationMessageHeaderAccessor(receive);
		assertThat(headerAccessor.getSequenceNumber()).isEqualTo(0);

		this.publishSubscribeChannel.setApplySequence(true);

		this.publishSubscribeChannel.send(new GenericMessage<>("test"));

		receive = this.securedChannelQueue.receive(10000);
		assertThat(receive).isNotNull();
		headerAccessor = new IntegrationMessageHeaderAccessor(receive);
		assertThat(headerAccessor.getSequenceNumber()).isEqualTo(1);

		receive = this.securedChannelQueue2.receive(10000);
		assertThat(receive).isNotNull();
		headerAccessor = new IntegrationMessageHeaderAccessor(receive);
		assertThat(headerAccessor.getSequenceNumber()).isEqualTo(2);

		this.publishSubscribeChannel.setApplySequence(false);

		SecurityContextHolder.clearContext();

		this.publishSubscribeChannel.send(new GenericMessage<>("test"));
		Message<?> errorMessage = this.errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		Object payload = errorMessage.getPayload();
		assertThat(payload).isInstanceOf(MessageDeliveryException.class);
		assertThat(((MessageDeliveryException) payload).getCause())
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	public void testSecurityContextPropagationAsyncGateway() throws Exception {
		login("bob", "bobspassword", "ROLE_ADMIN", "ROLE_PRESIDENT");
		Future<String> future = this.testGateway.test("foo");
		Message<?> receive = this.securedChannelQueue.receive(10000);
		assertThat(receive).isNotNull();

		MessageChannel replyChannel = receive.getHeaders().get(MessageHeaders.REPLY_CHANNEL, MessageChannel.class);
		replyChannel.send(new GenericMessage<>("bar"));

		String result = future.get(10, TimeUnit.SECONDS);
		assertThat(result).isNotNull();
		assertThat(result).isEqualTo("bar");
	}

	private void login(String username, String password, String... roles) {
		SecurityContext context = SecurityTestUtils.createContext(username, password, roles);
		SecurityContextHolder.setContext(context);
	}


	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	public static class ContextConfiguration {

		@Bean
		UserDetailsService userDetailsService() {
			return new InMemoryUserDetailsManager(
					User.withUsername("jimi")
							.password("jimispassword")
							.authorities("ROLE_USER", "ROLE_ADMIN")
							.build(),
					User.withUsername("bob")
							.password("bobspassword")
							.authorities("ROLE_USER")
							.build());
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = "secured*")
		AuthorizationChannelInterceptor authorizationChannelInterceptor() {
			return new AuthorizationChannelInterceptor(AuthorityAuthorizationManager.hasAnyRole("ADMIN", "PRESIDENT"));
		}

		@Bean
		public SubscribableChannel securedChannel() {
			return new DirectChannel();
		}

		@Bean
		public SubscribableChannel securedChannel2() {
			return new DirectChannel();
		}

		@Bean
		public SubscribableChannel unsecuredChannel() {
			return new DirectChannel();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = {
				"#{'queueChannel'}",
				"${security.channel:executorChannel}",
				"publishSubscribeChannel"})
		public ChannelInterceptor securityContextPropagationInterceptor() {
			return new SecurityContextPropagationChannelInterceptor();
		}

		@Bean
		@BridgeTo(value = "securedChannelQueue", poller = @Poller(fixedDelay = "1000"))
		public PollableChannel queueChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel securedChannelQueue() {
			return new QueueChannel();
		}

		@Bean
		@BridgeTo("securedChannelQueue")
		public SubscribableChannel executorChannel() {
			return new ExecutorChannel(Executors.newSingleThreadExecutor());
		}


		@Bean
		public PublishSubscribeChannel publishSubscribeChannel() {
			return new PublishSubscribeChannel(Executors.newCachedThreadPool());
		}

		@Bean
		@ServiceActivator(inputChannel = "publishSubscribeChannel")
		public MessageHandler securedChannelQueueBridge() {
			BridgeHandler handler = new BridgeHandler();
			handler.setOutputChannel(securedChannelQueue());
			handler.setOrder(1);
			return handler;
		}

		@Bean
		public PollableChannel securedChannelQueue2() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "publishSubscribeChannel")
		public MessageHandler securedChannelQueue2Bridge() {
			BridgeHandler handler = new BridgeHandler();
			handler.setOutputChannel(securedChannelQueue2());
			handler.setOrder(2);
			return handler;
		}

		@Bean
		public TaskScheduler taskScheduler() {
			return new ThreadPoolTaskScheduler();
		}

		@Bean
		public PollableChannel errorChannel() {
			return new QueueChannel();
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
		public AsyncTaskExecutor securityContextExecutor() {
			return new DelegatingSecurityContextAsyncTaskExecutor(new SimpleAsyncTaskExecutor());
		}

	}

	@MessagingGateway(asyncExecutor = "securityContextExecutor")
	public interface TestGateway {

		@Gateway(requestChannel = "queueChannel")
		Future<String> test(String payload);

	}

}
