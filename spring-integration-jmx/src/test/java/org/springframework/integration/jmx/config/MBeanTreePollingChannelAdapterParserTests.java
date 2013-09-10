/*
 * Copyright 2013 the original author or authors.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jmx.MBeanObjectConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Stuart Williams
 * @author Gary Russell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MBeanTreePollingChannelAdapterParserTests {

	@Autowired
	@Qualifier("channel1")
	private PollableChannel channel1;

	@Autowired
	@Qualifier("channel2")
	private PollableChannel channel2;

	@Autowired
	@Qualifier("channel3")
	private PollableChannel channel3;

	@Autowired
	@Qualifier("channel4")
	private PollableChannel channel4;

	@Autowired
	@Qualifier("channel5")
	private PollableChannel channel5;

	@Autowired
	@Qualifier("channel6")
	private PollableChannel channel6;

	@Autowired
	@Qualifier("adapter-default")
	private SourcePollingChannelAdapter adapterDefault;

	@Autowired
	@Qualifier("adapter-inner")
	private SourcePollingChannelAdapter adapterInner;

	@Autowired
	@Qualifier("adapter-query-name")
	private SourcePollingChannelAdapter adapterQueryName;

	@Autowired
	@Qualifier("adapter-query-name-bean")
	private SourcePollingChannelAdapter adapterQueryNameBean;

	@Autowired
	@Qualifier("adapter-query-expr-bean")
	private SourcePollingChannelAdapter adapterQueryExprBean;

	@Autowired
	@Qualifier("adapter-converter")
	private SourcePollingChannelAdapter adapterConverter;

	@Autowired
	private MBeanObjectConverter converter;

	@Autowired
	private MBeanServer mbeanServer;

	private final long testTimeout = 2000L;

	@Test
	public void pollDefaultAdapter() throws Exception {
		adapterDefault.start();

		Message<?> result = channel1.receive(testTimeout);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));

		adapterDefault.stop();
	}

	@Test
	public void pollInnerAdapter() throws Exception {
		adapterInner.start();

		Message<?> result = channel2.receive(testTimeout);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));

		adapterDefault.stop();
	}

	@Test
	public void pollQueryNameAdapter() throws Exception {
		adapterQueryName.start();

		ObjectName queryName = TestUtils.getPropertyValue(adapterQueryName, "source.queryName", ObjectName.class);
		assertEquals("java.lang:type=Runtime", queryName.getCanonicalName());

		QueryExp queryExp = TestUtils.getPropertyValue(adapterQueryName, "source.queryExpression", QueryExp.class);
		assertTrue(queryExp.apply(new ObjectName("java.lang:type=Runtime")));

		Message<?> result = channel3.receive(testTimeout);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertFalse(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));

		adapterDefault.stop();
	}

	@Test
	public void pollQueryNameBeanAdapter() throws Exception {
		adapterQueryNameBean.start();

		Message<?> result = channel4.receive(testTimeout);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertFalse(beans.containsKey("java.lang:type=Runtime"));

		adapterDefault.stop();
	}

	@Test
	public void pollQueryExprBeanAdapter() throws Exception {
		adapterQueryExprBean.start();

		Message<?> result = channel5.receive(testTimeout);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertFalse(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));

		adapterDefault.stop();
	}

	@Test
	public void pollConverterAdapter() throws Exception {
		adapterConverter.start();

		Message<?> result = channel6.receive(testTimeout);
		assertNotNull(result);

		assertEquals(HashMap.class, result.getPayload().getClass());

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertTrue(beans.containsKey("java.lang:type=OperatingSystem"));
		assertTrue(beans.containsKey("java.lang:type=Runtime"));

		adapterDefault.stop();
		assertSame(converter, TestUtils.getPropertyValue(adapterConverter, "source.converter"));
	}

}
