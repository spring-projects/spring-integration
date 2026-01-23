/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.jmx.config;

import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.QueryExp;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jmx.inbound.MBeanObjectConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stuart Williams
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig
@DirtiesContext
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

	private final long testTimeout = 20000L;

	@Test
	public void pollDefaultAdapter() {
		adapterDefault.start();

		Message<?> result = channel1.receive(testTimeout);
		assertThat(result)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(HashMap.class);

		adapterDefault.stop();

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertThat(beans).containsKeys("java.lang:type=OperatingSystem", "java.lang:type=Runtime");
	}

	@Test
	public void pollInnerAdapter() {
		adapterInner.start();

		Message<?> result = channel2.receive(testTimeout);
		assertThat(result)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(HashMap.class);

		adapterInner.stop();

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertThat(beans).containsKeys("java.lang:type=OperatingSystem", "java.lang:type=Runtime");
	}

	@Test
	public void pollQueryNameAdapter() throws Exception {
		adapterQueryName.start();

		ObjectName queryName = TestUtils.getPropertyValue(adapterQueryName, "source.queryName");
		assertThat(queryName.getCanonicalName()).isEqualTo("java.lang:type=Runtime");

		QueryExp queryExp = TestUtils.getPropertyValue(adapterQueryName, "source.queryExpression");
		assertThat(queryExp.apply(new ObjectName("java.lang:type=Runtime"))).isTrue();

		Message<?> result = channel3.receive(testTimeout);
		assertThat(result)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(HashMap.class);

		adapterQueryName.stop();

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertThat(beans)
				.containsKey("java.lang:type=Runtime")
				.doesNotContainKey("java.lang:type=OperatingSystem");
	}

	@Test
	public void pollQueryNameBeanAdapter() {
		adapterQueryNameBean.start();

		Message<?> result = channel4.receive(testTimeout);
		assertThat(result)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(HashMap.class);

		adapterQueryNameBean.stop();

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertThat(beans)
				.containsKey("java.lang:type=OperatingSystem")
				.doesNotContainKey("java.lang:type=Runtime");
	}

	@Test
	public void pollQueryExprBeanAdapter() {
		adapterQueryExprBean.start();

		Message<?> result = channel5.receive(testTimeout);
		assertThat(result)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(HashMap.class);

		adapterQueryExprBean.stop();

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertThat(beans)
				.containsKey("java.lang:type=Runtime")
				.doesNotContainKey("java.lang:type=OperatingSystem");
	}

	@Test
	public void pollConverterAdapter() {
		adapterConverter.start();

		Message<?> result = channel6.receive(testTimeout);
		assertThat(result)
				.isNotNull()
				.extracting(Message::getPayload)
				.isInstanceOf(HashMap.class);

		adapterConverter.stop();

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) result.getPayload();

		// test for a couple of MBeans
		assertThat(beans).containsKeys("java.lang:type=OperatingSystem", "java.lang:type=Runtime");

		assertThat(TestUtils.<Object>getPropertyValue(adapterConverter, "source.converter"))
				.isSameAs(converter);
	}

}
