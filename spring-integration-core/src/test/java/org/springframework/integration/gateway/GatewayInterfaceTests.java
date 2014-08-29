/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.gateway;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.annotation.AnnotationConstants;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@ContextConfiguration(classes = GatewayInterfaceTests.TestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class GatewayInterfaceTests {

	@Autowired
	private Int2634Gateway int2634Gateway;

	@Autowired
	private ExecGateway execGateway;

	@Autowired
	private NoExecGateway noExecGateway;

	@Autowired
	@Qualifier("&gatewayInterfaceTests$ExecGateway")
	private GatewayProxyFactoryBean execGatewayFB;

	@Autowired
	@Qualifier("&gatewayInterfaceTests$NoExecGateway")
	private GatewayProxyFactoryBean noExecGatewayFB;

	@Autowired
	private SimpleAsyncTaskExecutor exec;


	@Test
	public void testWithServiceSuperclassAnnotatedMethod() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		final Method fooMethod = Foo.class.getMethod("foo", String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("foo"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Foo.foo(java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(fooMethod));
				assertThat((String) message.getPayload(), equalTo("hello"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.foo("hello");
		assertTrue(called.get());
		Map<?,?> gateways = TestUtils.getPropertyValue(ac.getBean("&sampleGateway"), "gatewayMap", Map.class);
		Object mbf = ac.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertSame(mbf, TestUtils.getPropertyValue(gateways.values().iterator().next(),
				"messageConverter.messageBuilderFactory"));
		ac.close();
	}

	@Test
	public void testWithServiceSuperclassAnnotatedMethodOverridePE() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests2-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		final Method fooMethod = Foo.class.getMethod("foo", String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("foo"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Foo.foo(java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(fooMethod));
				assertThat((String) message.getPayload(), equalTo("foo"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.foo("hello");
		assertTrue(called.get());
		ac.close();
	}

	@Test
	public void testWithServiceAnnotatedMethod() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBar", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.bar("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testWithServiceSuperclassUnAnnotatedMethod() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		final Method bazMethod = Foo.class.getMethod("baz", String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("overrideGlobal"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Foo.baz(java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(bazMethod));
				assertThat((String) message.getPayload(), equalTo("hello"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.baz("hello");
		assertTrue(called.get());
		ac.close();
	}

	@Test
	public void testWithServiceUnAnnotatedMethodGlobalHeaderDoesntOverride() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		final Method quxMethod = Bar.class.getMethod("qux", String.class, String.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getHeaders().get("name"), equalTo("arg1"));
				assertThat(
						(String) message.getHeaders().get("string"),
						equalTo("public abstract void org.springframework.integration.gateway.GatewayInterfaceTests$Bar.qux(java.lang.String,java.lang.String)"));
				assertThat((Method) message.getHeaders().get("object"), equalTo(quxMethod));
				assertThat((String) message.getPayload(), equalTo("hello"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.qux("hello", "arg1");
		assertTrue(called.get());
		ac.close();
	}

	@Test
	public void testWithServiceCastAsSuperclassAnnotatedMethod() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelFoo", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Foo foo = ac.getBean(Foo.class);
		foo.foo("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testWithServiceCastAsSuperclassUnAnnotatedMethod() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Foo foo = ac.getBean(Foo.class);
		foo.baz("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testWithServiceHashcode() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		assertEquals(bar.hashCode(), ac.getBean(Bar.class).hashCode());
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testWithServiceToString() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.toString();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testWithServiceEquals() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		assertTrue(bar.equals(ac.getBean(Bar.class)));
		GatewayProxyFactoryBean fb = new GatewayProxyFactoryBean(Bar.class);
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton("requestChannelBar", channel);
		bf.registerSingleton("requestChannelBaz", channel);
		bf.registerSingleton("requestChannelFoo", channel);
		fb.setBeanFactory(bf);
		fb.afterPropertiesSet();
		assertFalse(bar.equals(fb.getObject()));
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testWithServiceGetClass() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		MessageHandler handler = mock(MessageHandler.class);
		channel.subscribe(handler);
		Bar bar = ac.getBean(Bar.class);
		bar.getClass();
		verify(handler, times(0)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithServiceAsNotAnInterface(){
		new GatewayProxyFactoryBean(NotAnInterface.class);
	}

	@Test
	public void testWithCustomMapper() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		DirectChannel channel = ac.getBean("requestChannelBaz", DirectChannel.class);
		final AtomicBoolean called = new AtomicBoolean();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				assertThat((String) message.getPayload(), equalTo("fizbuz"));
				called.set(true);
			}
		};
		channel.subscribe(handler);
		Baz baz = ac.getBean(Baz.class);
		baz.baz("hello");
		assertTrue(called.get());
		ac.close();
	}

	@Test
	public void testLateReply() throws Exception {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("GatewayInterfaceTests-context.xml", this.getClass());
		Bar baz = ac.getBean(Bar.class);
		String reply = baz.lateReply("hello");
		assertNull(reply);
		PollableChannel errorChannel = ac.getBean("errorChannel", PollableChannel.class);
		Message<?> receive = errorChannel.receive(5000);
		assertNotNull(receive);
		MessagingException messagingException = (MessagingException) receive.getPayload();
		assertThat(messagingException.getMessage(), Matchers.startsWith("Reply message received but the receiving thread has exited due to a timeout"));
		ac.close();
	}

	@Test
	public void testInt2634() {
		Map<Object, Object> param = Collections.<Object, Object>singletonMap(1, 1);
		Object result = this.int2634Gateway.test2(param);
		assertEquals(param, result);

		result = this.int2634Gateway.test1(param);
		assertEquals(param, result);
	}

	/*
	 * Tests use current thread in payload and reply has the thread that actually
	 * performed the send() on gatewayThreadChannel.
	 */
	@Test
	public void testExecs() throws Exception {
		assertSame(exec, TestUtils.getPropertyValue(execGatewayFB, "asyncExecutor"));
		assertNull(TestUtils.getPropertyValue(noExecGatewayFB, "asyncExecutor"));

		Future<Thread> result = this.int2634Gateway.test3(Thread.currentThread());
		assertNotEquals(Thread.currentThread(), result.get());
		assertThat(result.get().getName(), startsWith("SimpleAsync"));

		result = this.execGateway.test1(Thread.currentThread());
		assertNotEquals(Thread.currentThread(), result.get());
		assertThat(result.get().getName(), startsWith("exec-"));

		result = this.noExecGateway.test1(Thread.currentThread());
		assertEquals(Thread.currentThread(), result.get());

		ListenableFuture<Thread> result2 = this.execGateway.test2(Thread.currentThread());
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Thread> thread = new AtomicReference<Thread>();
		result2.addCallback(new ListenableFutureCallback<Thread>() {

			@Override
			public void onSuccess(Thread result) {
				thread.set(result);
				latch.countDown();
			}

			@Override
			public void onFailure(Throwable t) {
			}
		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		assertThat(result2.get().getName(), startsWith("exec-"));
	}


	public interface Foo {

		@Gateway(requestChannel="requestChannelFoo")
		public void foo(String payload);

		public void baz(String payload);

		public String lateReply(String payload);

	}

	public static interface Bar extends Foo {
		@Gateway(requestChannel="requestChannelBar")
		public void bar(String payload);

		public void qux(String payload, @Header("name") String nameHeader);
	}

	public static class NotAnInterface {
		public void fail(String payload){}
	}

	public interface Baz {

		public void baz(String payload);
	}

	public static class BazMapper implements MethodArgsMessageMapper {

		@Override
		public Message<?> toMessage(MethodArgsHolder object) throws Exception {
			return MessageBuilder.withPayload("fizbuz").build();
		}

	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	public static class TestConfig {

		@Bean
		@BridgeTo
		public MessageChannel gatewayChannel() {
			return new DirectChannel();
		}

		@Bean
		@BridgeTo
		public MessageChannel gatewayThreadChannel() {
			DirectChannel channel = new DirectChannel();
			channel.addInterceptor(new ChannelInterceptorAdapter() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					Object payload;
					if (Thread.currentThread().equals(message.getPayload())) {
						// running on calling thread - need to return a Future.
						payload = new AsyncResult<Thread>(Thread.currentThread());
					}
					else {
						payload = Thread.currentThread();
					}
					return MessageBuilder.withPayload(payload)
							.copyHeaders(message.getHeaders())
							.build();
				}

			});
			return channel;
		}

		@Bean
		public AsyncTaskExecutor exec() {
			SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor();
			simpleAsyncTaskExecutor.setThreadNamePrefix("exec-");
			return simpleAsyncTaskExecutor;
		}

	}

	@MessagingGateway
	public interface Int2634Gateway {

		@Gateway(requestChannel = "gatewayChannel", payloadExpression = "#args[0]")
		Object test1(Map<Object, ?> map);

		@Gateway(requestChannel = "gatewayChannel")
		Object test2(@Payload Map<Object, ?> map);

		@Gateway(requestChannel = "gatewayThreadChannel")
		Future<Thread> test3(Thread caller);

	}

	@MessagingGateway(asyncExecutor = "exec")
	public interface ExecGateway {

		@Gateway(requestChannel = "gatewayThreadChannel")
		Future<Thread> test1(Thread caller);

		@Gateway(requestChannel = "gatewayThreadChannel")
		ListenableFuture<Thread> test2(Thread caller);

	}

	@MessagingGateway(asyncExecutor = AnnotationConstants.NULL)
	public interface NoExecGateway {

		@Gateway(requestChannel = "gatewayThreadChannel")
		Future<Thread> test1(Thread caller);

	}

}
