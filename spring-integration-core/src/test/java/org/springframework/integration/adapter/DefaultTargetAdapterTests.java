/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class DefaultTargetAdapterTests {

	@Test
	public void testAdapterSendsToChannel() throws Exception {
		SynchronousQueue<String> queue = new SynchronousQueue<String>();
		TestBean testBean = new TestBean(queue);
		MethodInvokingTarget<TestBean> target = new MethodInvokingTarget<TestBean>();
		target.setObject(testBean);
		target.setMethod("foo");
		target.afterPropertiesSet();
		DefaultTargetAdapter adapter = new DefaultTargetAdapter(target);
		SimpleChannel channel = new SimpleChannel();
		adapter.setChannel(channel);
		Message<String> message = new GenericMessage<String>("123", "testing");
		channel.send(message);
		assertNull(queue.poll());
		MessageBus bus = new MessageBus();
		bus.registerChannel("channel", channel);
		bus.registerTargetAdapter("targetAdapter", adapter);
		bus.start();
		String result = queue.poll(100, TimeUnit.MILLISECONDS);
		assertNotNull(result);
		assertEquals("testing", result);
		bus.stop();
	}


	public static class TestBean {

		private BlockingQueue<String> queue;

		public TestBean(BlockingQueue<String> queue) {
			this.queue = queue;
		}

		public void foo(String s) {
			try {
				this.queue.put(s);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
