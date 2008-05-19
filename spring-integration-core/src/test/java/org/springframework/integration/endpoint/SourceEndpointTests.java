/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.junit.Test;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.Source;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class SourceEndpointTests {

	@Test
	public void testPolledSourceSendsToChannel() {
		TestSource source = new TestSource("testing", 1);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(100);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		endpoint.run();
		Message<?> message = channel.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("testing.1", message.getPayload());
	}

	@Test
	public void testSendTimeout() {
		TestSource source = new TestSource("testing", 1);
		QueueChannel channel = new QueueChannel(1);
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		endpoint.setSendTimeout(10);
		endpoint.run();
		Message<?> message1 = channel.receive(1000);
		assertNotNull("message should not be null", message1);
		assertEquals("testing.1", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNull("second message should be null", message2);
		source.resetCounter();
		endpoint.run();
		Message<?> message3 = channel.receive(100);
		assertNotNull("third message should not be null", message3);
		assertEquals("testing.1", message3.getPayload());
	}

	@Test
	public void testMultipleMessagesPerPoll() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		Message<?> message1 = channel.receive(0);
		assertNotNull("message should not be null", message1);
		assertEquals("testing.1", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNotNull("message should not be null", message2);
		assertEquals("testing.2", message2.getPayload());
		Message<?> message3 = channel.receive(0);
		assertNotNull("message should not be null", message3);
		assertEquals("testing.3", message3.getPayload());		
		Message<?> message4 = channel.receive(0);
		assertNull("message should be null", message4);
	}

	@Test
	public void testTaskAdviceChain() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		final StringBuffer buffer = new StringBuffer();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(1);
			}
		});
		taskAdviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				buffer.append(2);
				Object retval = invocation.proceed();
				buffer.append(4);
				return retval;
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(3);
			}
		});
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("1234", buffer.toString());
	}

	@Test
	public void testDispatchAdviceChain() {
		TestSource source = new TestSource("testing", 2);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		final StringBuffer buffer = new StringBuffer();
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		dispatchAdviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				buffer.append("b");
				Object retval = invocation.proceed();
				buffer.append("d");
				return retval;
			}
		});
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("c");
			}
		});
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("abcdabcd", buffer.toString());
	}

	@Test
	public void testTaskAndDispatchAdviceChains() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		final StringBuffer buffer = new StringBuffer();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		dispatchAdviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				buffer.append("b");
				Object retval = invocation.proceed();
				buffer.append("c");
				return retval;
			}
		});
		taskAdviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				buffer.append(1);
				Object retval = invocation.proceed();
				buffer.append(3);
				return retval;
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(2);
			}
		});
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("12abcabcabc3", buffer.toString());
	}

	@Test
	public void testRefreshTaskAtRuntime() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		final StringBuffer buffer = new StringBuffer();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		dispatchAdviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				buffer.append("b");
				Object retval = invocation.proceed();
				buffer.append("c");
				return retval;
			}
		});
		taskAdviceChain.add(new MethodInterceptor() {
			public Object invoke(MethodInvocation invocation) throws Throwable {
				buffer.append(1);
				Object retval = invocation.proceed();
				buffer.append(3);
				return retval;
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(2);
			}
		});
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("abcabcabc", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.refreshTask();
		endpoint.run();
		assertEquals("12abcabcabc3", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setDispatchAdviceChain(null);
		endpoint.refreshTask();
		endpoint.run();
		assertEquals("123", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setTaskAdviceChain(null);
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.refreshTask();
		endpoint.run();
		assertEquals("abcabcabc", buffer.toString());
	}

	@Test
	public void testInitializeTaskDoesNotRefreshWithDispatchAdviceOnly() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		final StringBuffer buffer = new StringBuffer();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(1);
			}
		});
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("aaa", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("aaa", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setDispatchAdviceChain(null);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("aaa", buffer.toString());
	}

	@Test
	public void testInitializeTaskDoesNotRefreshWithTaskAdviceOnly() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		final StringBuffer buffer = new StringBuffer();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(1);
			}
		});
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("1", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("1", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setTaskAdviceChain(null);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("1", buffer.toString());
	}

	@Test
	public void testInitializeTaskDoesNotRefreshWithTaskAndDispatchAdvice() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		final StringBuffer buffer = new StringBuffer();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(1);
			}
		});
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("1aaa", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setDispatchAdviceChain(null);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("1aaa", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setTaskAdviceChain(null);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("1aaa", buffer.toString());
	}

	@Test
	public void testInitializeTaskDoesNotRefreshWithNoAdvice() {
		TestSource source = new TestSource("testing", 3);
		QueueChannel channel = new QueueChannel();
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel, schedule);
		List<Advice> dispatchAdviceChain = new ArrayList<Advice>();
		List<Advice> taskAdviceChain = new ArrayList<Advice>();
		final StringBuffer buffer = new StringBuffer();
		dispatchAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append("a");
			}
		});
		taskAdviceChain.add(new MethodBeforeAdvice() {
			public void before(Method method, Object[] args, Object target) throws Throwable {
				buffer.append(1);
			}
		});
		endpoint.afterPropertiesSet();
		endpoint.setMaxMessagesPerTask(5);
		endpoint.run();
		assertEquals("", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setDispatchAdviceChain(dispatchAdviceChain);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("", buffer.toString());
		buffer.delete(0, buffer.length());
		source.resetCounter();
		endpoint.setTaskAdviceChain(taskAdviceChain);
		endpoint.initializeTask();
		endpoint.run();
		assertEquals("", buffer.toString());
	}


	private static class TestSource implements Source<String> {

		private String message;

		private int limit;

		private AtomicInteger count = new AtomicInteger();

		public TestSource(String message, int limit) {
			this.message = message;
			this.limit = limit;
		}

		public void resetCounter() {
			this.count.set(0);
		}

		public Message<String> receive() {
			if (count.get() >= limit) {
				return null;
			}
			return new GenericMessage<String>(message + "." + count.incrementAndGet());
		}
	}

}
