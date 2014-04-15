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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class GatewayProxyFactoryBeanTests {

	@Test
	public void testRequestReplyWithAnonymousChannel() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		String result = service.requestReply("foo");
		assertEquals("foobar", result);
	}

	@Test
	public void testRequestReplyWithAnonymousChannelConvertedTypeViaConversionService() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GenericConversionService cs = new DefaultConversionService();
		Converter<String, byte[]> stringToByteConverter = new Converter<String, byte[]>() {
			@Override
			public byte[] convert(String source) {
				return source.getBytes();
			}
		};
		stringToByteConverter = Mockito.spy(stringToByteConverter);
		cs.addConverter(stringToByteConverter);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME, cs);

		proxyFactory.setBeanFactory(bf);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		byte[] result = service.requestReplyInBytes("foo");
		assertEquals(6, result.length);
		Mockito.verify(stringToByteConverter, Mockito.times(1)).convert(Mockito.any(String.class));
	}

	@Test
	public void testOneWay() throws Exception {
		final QueueChannel requestChannel = new QueueChannel();
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		service.oneWay("test");
		Message<?> message = requestChannel.receive(1000);
		assertNotNull(message);
		assertEquals("test", message.getPayload());
	}

	@Test
	public void testSolicitResponse() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		replyChannel.send(new GenericMessage<String>("foo"));
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setDefaultRequestChannel(new DirectChannel());
		proxyFactory.setDefaultReplyChannel(replyChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		String result = service.solicitResponse();
		assertNotNull(result);
		assertEquals("foo", result);
	}

	@Test
	public void testRequestReplyWithTypeConversion() throws Exception {
		final QueueChannel requestChannel = new QueueChannel();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> input = requestChannel.receive();
				GenericMessage<String> reply = new GenericMessage<String>(input.getPayload() + "456");
				((MessageChannel) input.getHeaders().getReplyChannel()).send(reply);
			}
		}).start();
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		Integer result = service.requestReplyWithIntegers(123);
		assertEquals(new Integer(123456), result);
	}

	@Test
	public void testRequestReplyWithRendezvousChannelInApplicationContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"gatewayWithRendezvousChannel.xml", GatewayProxyFactoryBeanTests.class);
		TestService service = (TestService) context.getBean("proxy");
		String result = service.requestReply("foo");
		assertEquals("foo!!!", result);
	}

	@Test
	public void testRequestReplyWithResponseCorrelatorInApplicationContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"gatewayWithResponseCorrelator.xml", GatewayProxyFactoryBeanTests.class);
		TestService service = (TestService) context.getBean("proxy");
		String result = service.requestReply("foo");
		assertEquals("foo!!!", result);
		TestChannelInterceptor interceptor = (TestChannelInterceptor) context.getBean("interceptor");
		assertEquals(1, interceptor.getSentCount());
		assertEquals(1, interceptor.getReceivedCount());
	}

	@Test
	public void testMultipleMessagesWithResponseCorrelator() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"gatewayWithResponseCorrelator.xml", GatewayProxyFactoryBeanTests.class);
		int numRequests = 500;
		final TestService service = (TestService) context.getBean("proxy");
		final String[] results = new String[numRequests];
		final CountDownLatch latch = new CountDownLatch(numRequests);
		Executor executor = Executors.newFixedThreadPool(numRequests);
		for (int i = 0; i < numRequests; i++) {
			final int count = i;
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// add some randomness to the ordering of requests
					try {
						Thread.sleep(new Random().nextInt(100));
					}
					catch (InterruptedException e) {
						// ignore
					}
					results[count] = service.requestReply("test-" + count);
					latch.countDown();
				}
			});
		}
		latch.await(10, TimeUnit.SECONDS);
		for (int i = 0; i < numRequests; i++) {
			assertEquals("test-" + i + "!!!", results[i]);
		}
		TestChannelInterceptor interceptor = (TestChannelInterceptor) context.getBean("interceptor");
		assertEquals(numRequests, interceptor.getSentCount());
		assertEquals(numRequests, interceptor.getReceivedCount());
	}

	@Test
	public void testMessageAsMethodArgument() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		String result = service.requestReplyWithMessageParameter(new GenericMessage<String>("foo"));
		assertEquals("foobar", result);
	}

	@Test
	public void testNoArgMethodWithPayloadAnnotation() throws Exception {
		QueueChannel requestChannel = new QueueChannel();
		startResponder(requestChannel);
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		String result = service.requestReplyWithPayloadAnnotation();
		assertEquals("requestReplyWithPayloadAnnotation0bar", result);
	}

	@Test
	public void testMessageAsReturnValue() throws Exception {
		final QueueChannel requestChannel = new QueueChannel();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> input = requestChannel.receive();
				GenericMessage<String> reply = new GenericMessage<String>(input.getPayload() + "bar");
				((MessageChannel) input.getHeaders().getReplyChannel()).send(reply);
			}
		}).start();
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setDefaultRequestChannel(requestChannel);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestService service = (TestService) proxyFactory.getObject();
		Message<?> result = service.requestReplyWithMessageReturnValue("foo");
		assertEquals("foobar", result.getPayload());
	}

	@Test
	public void testServiceMustBeInterface() {
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		int count = 0;
		try {
			proxyFactory.setServiceInterface(TestService.class);
			count++;
			proxyFactory.setServiceInterface(String.class);
			count++;
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		assertEquals(1, count);
	}

	@Test
	public void testProxiedToStringMethod() throws Exception {
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		proxyFactory.setDefaultRequestChannel(new DirectChannel());
		proxyFactory.setServiceInterface(TestService.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		Object proxy = proxyFactory.getObject();
		String expected = "gateway proxy for";
		assertEquals(expected, proxy.toString().substring(0, expected.length()));
	}

	@Test(expected = TestException.class)
	public void testCheckedExceptionRethrownAsIs() throws Exception {
		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
		DirectChannel channel = new DirectChannel();
		EventDrivenConsumer consumer = new EventDrivenConsumer(channel, new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				Method method = ReflectionUtils.findMethod(
						GatewayProxyFactoryBeanTests.class, "throwTestException");
				ReflectionUtils.invokeMethod(method, this);
			}
		});
		consumer.start();
		proxyFactory.setDefaultRequestChannel(channel);
		proxyFactory.setServiceInterface(TestExceptionThrowingInterface.class);
		proxyFactory.setBeanName("testGateway");
		proxyFactory.setBeanFactory(mock(BeanFactory.class));
		proxyFactory.afterPropertiesSet();
		TestExceptionThrowingInterface proxy = (TestExceptionThrowingInterface) proxyFactory.getObject();
		proxy.throwCheckedException("test");
	}


	private static void startResponder(final PollableChannel requestChannel) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message<?> input = requestChannel.receive();
				GenericMessage<String> reply = new GenericMessage<String>(input.getPayload() + "bar");
				((MessageChannel) input.getHeaders().getReplyChannel()).send(reply);
			}
		}).start();
	}

	@Test
	public void testProgrammaticWiring() throws Exception {
		GatewayProxyFactoryBean gpfb = new GatewayProxyFactoryBean();
		gpfb.setBeanFactory(mock(BeanFactory.class));
		gpfb.setServiceInterface(TestEchoService.class);
		QueueChannel drc = new QueueChannel();
		gpfb.setDefaultRequestChannel(drc);
		gpfb.setDefaultReplyTimeout(0L);
		GatewayMethodMetadata meta = new GatewayMethodMetadata();
		meta.setHeaderExpressions(Collections. <String, Expression> singletonMap("foo", new LiteralExpression("bar")));
		gpfb.setGlobalMethodMetadata(meta);
		gpfb.afterPropertiesSet();
		((TestEchoService) gpfb.getObject()).echo("foo");
		Message<?> message = drc.receive(0);
		assertNotNull(message);
		String bar = (String) message.getHeaders().get("foo");
		assertNotNull(bar);
		assertThat(bar, equalTo("bar"));
	}

//	@Test
//	public void testHistory() throws Exception {
//		GenericApplicationContext context = new GenericApplicationContext();
//		context.getBeanFactory().registerSingleton("historyWriter", new MessageHistoryWriter());
//		GatewayProxyFactoryBean proxyFactory = new GatewayProxyFactoryBean();
//		proxyFactory.setBeanFactory(context);
//		proxyFactory.setBeanName("testGateway");
//		DirectChannel channel = new DirectChannel();
//		channel.setBeanName("testChannel");
//		channel.setBeanFactory(context);
//		channel.afterPropertiesSet();
//		BridgeHandler bridgeHandler = new BridgeHandler();
//		bridgeHandler.setBeanFactory(context);
//		bridgeHandler.afterPropertiesSet();
//		bridgeHandler.setBeanName("testBridge");
//		EventDrivenConsumer consumer = new EventDrivenConsumer(channel, bridgeHandler);
//		consumer.setBeanFactory(context);
//		consumer.afterPropertiesSet();
//		consumer.start();
//		proxyFactory.setDefaultRequestChannel(channel);
//		proxyFactory.setServiceInterface(TestEchoService.class);
//		proxyFactory.afterPropertiesSet();
//		TestEchoService proxy = (TestEchoService) proxyFactory.getObject();
//		Message<?> message = proxy.echo("test");
//		Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
//		MessageHistoryEvent event1 = historyIterator.next();
//		MessageHistoryEvent event2 = historyIterator.next();
//		MessageHistoryEvent event3 = historyIterator.next();
//
//		//assertEquals("echo", event1.getAttribute("method", String.class));
//		assertEquals("gateway", event1.getType());
//		assertEquals("testGateway", event1.getName());
//		assertEquals("channel", event2.getType());
//		assertEquals("testChannel", event2.getName());
//		assertEquals("bridge", event3.getType());
//		assertEquals("testBridge", event3.getName());
//	}

	@Test
	public void autowiredGateway() {
		new ClassPathXmlApplicationContext("gatewayAutowiring.xml", GatewayProxyFactoryBeanTests.class);
	}


	public static void throwTestException() throws TestException {
		throw new TestException();
	}


	static interface TestEchoService {

		Message<?> echo(String s);
	}


	static interface TestExceptionThrowingInterface {

		String throwCheckedException(String s) throws TestException;
	}


	@SuppressWarnings("serial")
	static class TestException extends Exception {
	}


	public static class TestClient {

		@SuppressWarnings("unused")
		private final TestService service;

		@Autowired
		public TestClient(TestService service) {
			this.service = service;
		}
	}

}
