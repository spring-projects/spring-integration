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

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;

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

	@Test(timeout = 100)
	public void multipleTargetsPartialTimout() throws Exception {
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
				timingOutStarted.compareAndSet(false,true);
				latch.countDown();
				// cause timeout here
				Thread.sleep(1000);
				testNotTimedOut.compareAndSet(false, true);
				//fail("There is a bug in this Test");
				//in a long running suite this will run until the end, but the test will already be over
				return null;
			}
		}).anyTimes();
		replay(globalMocks);
		dispatcher.send(messageMock);
		latch.await();
		verify(globalMocks);
		assertFalse("Test not timed out properly", testNotTimedOut.get());
		assertTrue("Timing out Runnable not executed", timingOutStarted.get());
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