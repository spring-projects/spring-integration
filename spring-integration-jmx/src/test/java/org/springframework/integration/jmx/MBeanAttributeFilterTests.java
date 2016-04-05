/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.integration.jmx;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stuart Williams
 * @author Gary Russell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class MBeanAttributeFilterTests {

	@Autowired
	@Qualifier("out")
	private PollableChannel channel;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Autowired
	private SourcePollingChannelAdapter adapterNot;

	@Autowired
	private String domain;

	private final long testTimeout = 10000L;

	@Test
	public void testAttributeFilter() {
		while (channel.receive(0) != null) {
			// drain
		}
		adapter.start();

		Message<?> result = channel.receive(testTimeout);
		assertNotNull(result);
		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> payload = (Map<String, Object>) result.getPayload();
		assertEquals(4, payload.size());

		@SuppressWarnings("unchecked")
		Map<String, Object> bean = (Map<String, Object>) payload
				.get(domain + ":name=in,type=MessageChannel");

		assertEquals(2, bean.size());
		assertTrue(bean.containsKey("SendCount"));
		assertTrue(bean.containsKey("SendErrorCount"));

		adapter.stop();
	}

	@Test
	public void testAttributeFilterNot() {
		while (channel.receive(0) != null) {
			// drain
		}
		adapterNot.start();

		Message<?> result = channel.receive(testTimeout);
		assertNotNull(result);
		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> payload = (Map<String, Object>) result.getPayload();
		assertEquals(4, payload.size());

		@SuppressWarnings("unchecked")
		Map<String, Object> bean = (Map<String, Object>) payload
				.get(domain + ":name=in,type=MessageChannel");

		List<String> keys = new ArrayList<String>(bean.keySet());
		Collections.sort(keys);
		assertThat(keys, contains("LoggingEnabled",
				"MaxSendDuration",
				"MeanErrorRate",
				"MeanErrorRatio",
				"MeanSendDuration",
				"MeanSendRate",
				"MinSendDuration",
				"StandardDeviationSendDuration",
				"TimeSinceLastSend"));

		adapterNot.stop();
	}

}
