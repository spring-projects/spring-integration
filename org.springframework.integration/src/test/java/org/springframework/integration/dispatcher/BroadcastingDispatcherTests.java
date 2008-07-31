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

package org.springframework.integration.dispatcher;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class BroadcastingDispatcherTests {

	private BroadcastingDispatcher dispatcher;

	private TaskExecutor taskExecutorMock = createMock(TaskExecutor.class);

	private Message<?> messageMock = createMock(Message.class);

	private MessageTarget targetMock = createMock(MessageTarget.class);

	private Object[] globalMocks = new Object[] { messageMock, taskExecutorMock, targetMock };


	@Before
	public void init() {
		dispatcher = new BroadcastingDispatcher();
		dispatcher.setTaskExecutor(taskExecutorMock);
		reset(globalMocks);
		defaultTaskExecutorMock();
	}


	@Test
	public void publishSubcribe() throws Exception {
		dispatcher.setTaskExecutor(null);
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		expect(targetMock.send(messageMock)).andReturn(true).times(2);
		replay(globalMocks);
		dispatcher.send(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsWithExecutor() {
		// should the same target be allowed to be added twice?
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		expect(targetMock.send(messageMock)).andReturn(true).times(3);
		replay(globalMocks);
		dispatcher.send(messageMock);
		verify(globalMocks);
	}

	@Test
	public void multipleTargetsPartialFailure() {
		reset(taskExecutorMock);
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		partialFailingExecutorMock(true, false, true);
		expect(targetMock.send(messageMock)).andReturn(true).times(2);
		replay(globalMocks);
		dispatcher.send(messageMock);
		verify(globalMocks);
	}

	@Test(timeout = 500)
	public void multipleTargetsPartialTimeout() throws Exception {
		reset(taskExecutorMock);
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		dispatcher.addTarget(targetMock);
		dispatcher.setTimeout(50);
		// three threads invoking targets
		final CountDownLatch latch = new CountDownLatch(3);
		threadedExecutorMock(3);
		final AtomicBoolean timingOutStarted = new AtomicBoolean(false);
		final AtomicBoolean testNotTimedOut = new AtomicBoolean(false);

		expect(targetMock.send(messageMock)).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				latch.countDown();
				return true;
			}
		}).times(2);
		/*
		 * Watch out, this is tricky. The send() method will be invoked but due
		 * to the faked time out it will never return. Therefore the expectation
		 * needs to be there, but during the verify it will be called 0 times.
		 * This is something that EasyMock doesn't support so I've worked around
		 * it with an AtomicBoolean and a latch. It isn't pretty, but it sort of works
		 */
		expect(targetMock.send(messageMock)).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				// this should happen
				timingOutStarted.compareAndSet(false, true);
				latch.countDown();
				// cause timeout here
				Thread.sleep(1000);
				testNotTimedOut.compareAndSet(false, true);
				//fail("There is a bug in this Test");
				//in a long running suite this will run until the end, but the test will already be over
				return true;
			}
		}).anyTimes();
		replay(globalMocks);
		dispatcher.send(messageMock);
		latch.await();
		verify(globalMocks);
		assertFalse("Test not timed out properly", testNotTimedOut.get());
		assertTrue("Timing out Runnable not executed", timingOutStarted.get());
	}

	@Test
	public void applySequenceDisabledByDefault() {
		BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		MessageTarget target = new MessageTarget() {
			public boolean send(Message<?> message) {
				messages.add(message);
				return true;
			}
		};
		dispatcher.addTarget(target);
		dispatcher.addTarget(target);
		dispatcher.send(new StringMessage("test"));
		assertEquals(2, messages.size());
		assertEquals(0, (int) messages.get(0).getHeaders().getSequenceNumber());
		assertEquals(0, (int) messages.get(0).getHeaders().getSequenceSize());
		assertEquals(0, (int) messages.get(1).getHeaders().getSequenceNumber());
		assertEquals(0, (int) messages.get(1).getHeaders().getSequenceSize());
	}

	@Test
	public void applySequenceEnabled() {
		BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();
		dispatcher.setApplySequence(true);
		final List<Message<?>> messages = new ArrayList<Message<?>>();
		MessageTarget target = new MessageTarget() {
			public boolean send(Message<?> message) {
				messages.add(message);
				return true;
			}
		};
		dispatcher.addTarget(target);
		dispatcher.addTarget(target);
		dispatcher.addTarget(target);
		dispatcher.send(new StringMessage("test"));
		assertEquals(3, messages.size());
		assertEquals(1, (int) messages.get(0).getHeaders().getSequenceNumber());
		assertEquals(3, (int) messages.get(0).getHeaders().getSequenceSize());
		assertEquals(2, (int) messages.get(1).getHeaders().getSequenceNumber());
		assertEquals(3, (int) messages.get(1).getHeaders().getSequenceSize());
		assertEquals(3, (int) messages.get(2).getHeaders().getSequenceNumber());
		assertEquals(3, (int) messages.get(2).getHeaders().getSequenceSize());
	}


	private void defaultTaskExecutorMock() {
		taskExecutorMock.execute(isA(Runnable.class));
		expectLastCall().andAnswer(new IAnswer<Object>() {
			public Object answer() throws Throwable {
				((Runnable) getCurrentArguments()[0]).run();
				return null;
			}
		}).anyTimes();
	}

	/*
	 * runs the runnable based on the array of passes
	 */
	private void partialFailingExecutorMock(boolean... passes) {
		taskExecutorMock.execute(isA(Runnable.class));
		for (final boolean pass : passes)
			expectLastCall().andAnswer(new IAnswer<Object>() {
				public Object answer() throws Throwable {
					if (pass) {
						((Runnable) getCurrentArguments()[0]).run();
					}
					return null;
				}
			});
	}

	/*
	 * expect count calls to the taskExecutorMock.execute and have them run the runnable
	 * in a new Thread.
	 */
	private void threadedExecutorMock(int count) {
		taskExecutorMock.execute(isA(Runnable.class));
		expectLastCall().andAnswer(new IAnswer<Object>() {
			public Object answer() throws Throwable {
				final Runnable runnable = (Runnable) getCurrentArguments()[0];
				new Thread(runnable).start();
				return null;
			}
		}).times(count);
	}
}