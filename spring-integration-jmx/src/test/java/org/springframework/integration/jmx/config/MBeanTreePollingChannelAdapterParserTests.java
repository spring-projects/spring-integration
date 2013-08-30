/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stuart Williams
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MBeanTreePollingChannelAdapterParserTests {

	@Autowired
	private PollableChannel channel;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Test
	public void pollForBeans() throws Exception {
		adapter.start();

		Message<?> result = channel.receive(1000);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());
		
		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));
	}

}
