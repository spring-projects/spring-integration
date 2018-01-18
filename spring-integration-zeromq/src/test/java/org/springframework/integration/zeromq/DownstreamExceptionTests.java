/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.zeromq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Subhobrata Dey
 * @since 5.1
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class DownstreamExceptionTests {

	@Autowired
	private Service service;

	private static org.springframework.integration.zeromq.outbound.ZeromqMessageHandler adapter1;
	private static org.springframework.integration.zeromq.outbound.ZeromqMessageHandler adapter2;

	@Autowired
	private org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter noErrorChannel;

	@Autowired
	private org.springframework.integration.zeromq.inbound.ZeromqMessageDrivenChannelAdapter withErrorChannel;

	@Autowired
	private PollableChannel errors;

	@BeforeClass
	public static void beforeClass() {
		if (adapter1 == null) {
			adapter1 = new org.springframework.integration.zeromq.outbound.ZeromqMessageHandler("tcp://*:5558", "serverId1");
			adapter1.setConverter(new org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter());
			adapter1.start();
		}
		if (adapter2 == null) {
			adapter2 = new org.springframework.integration.zeromq.outbound.ZeromqMessageHandler("tcp://*:5559", "serverId2");
			adapter2.setConverter(new org.springframework.integration.zeromq.support.DefaultZeromqMessageConverter());
			adapter2.start();
		}
	}

	@Ignore
	@Test
	public void testNoErrorChannel() throws Exception {
		service.n = 0;
		Log logger = spy(TestUtils.getPropertyValue(noErrorChannel, "logger", Log.class));
		final CountDownLatch latch = new CountDownLatch(1);
		doAnswer(invocation -> {
			if (((String) invocation.getArgument(0)).contains("Unhandled")) {
				latch.countDown();
			}
			return null;
		}).when(logger).error(anyString(), any(Throwable.class));
		new DirectFieldAccessor(noErrorChannel).setPropertyValue("logger", logger);
		adapter1.handleMessage(new GenericMessage<String>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		service.barrier.reset();
		adapter1.handleMessage(new GenericMessage<String>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		verify(logger).error(contains("Unhandled exception for"), any(Throwable.class));
		service.barrier.reset();
		adapter1.stop();
	}

	@Ignore
	@Test
	public void testWithErrorChannel() throws Exception {
		assertThat(this.errors).isEqualTo(TestUtils.getPropertyValue(withErrorChannel, "errorChannel"));
		service.n = 0;
		adapter2.handleMessage(new GenericMessage<String>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		service.barrier.reset();
		adapter2.handleMessage(new GenericMessage<String>("foo"));
		service.barrier.await(10, TimeUnit.SECONDS);
		Thread.sleep(10000);
		assertThat(errors.receive()).isNotNull();
		service.barrier.reset();
		adapter2.stop();
	}

	public static class Service {

		public CyclicBarrier barrier = new CyclicBarrier(2);

		public int n;

		public void foo(String foo) throws Exception {
			barrier.await(10, TimeUnit.SECONDS);
			if (n++ > 0) {
				throw new RuntimeException("bar");
			}
		}
	}
}
