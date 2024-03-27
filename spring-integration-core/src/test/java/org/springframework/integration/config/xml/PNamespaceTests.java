/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the "p:namespace" is working for inner "bean" definition within SI components.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PNamespaceTests {

	@Autowired
	@Qualifier("sa")
	EventDrivenConsumer serviceActivator;

	@Autowired
	@Qualifier("sp")
	EventDrivenConsumer splitter;

	@Autowired
	@Qualifier("rt")
	EventDrivenConsumer router;

	@Autowired
	@Qualifier("tr")
	EventDrivenConsumer transformer;

	@Autowired
	@Qualifier("sampleChain")
	EventDrivenConsumer sampleChain;

	@Test
	public void testPNamespaceServiceActivator() {
		TestBean bean = prepare(serviceActivator);
		assertThat(bean.getFname()).isEqualTo("paris");
		assertThat(bean.getLname()).isEqualTo("hilton");
	}

	@Test
	public void testPNamespaceSplitter() {
		TestBean bean = prepare(splitter);
		assertThat(bean.getFname()).isEqualTo("paris");
		assertThat(bean.getLname()).isEqualTo("hilton");
	}

	@Test
	public void testPNamespaceRouter() {
		TestBean bean = prepare(router);
		assertThat(bean.getFname()).isEqualTo("paris");
		assertThat(bean.getLname()).isEqualTo("hilton");
	}

	@Test
	public void testPNamespaceTransformer() {
		TestBean bean = prepare(transformer);
		assertThat(bean.getFname()).isEqualTo("paris");
		assertThat(bean.getLname()).isEqualTo("hilton");
	}

	@Test
	public void testPNamespaceChain() {
		List<?> handlers = (List<?>) TestUtils.getPropertyValue(sampleChain, "handler.handlers");
		AggregatingMessageHandler handler = (AggregatingMessageHandler) handlers.get(0);
		SampleAggregator aggregator =
				(SampleAggregator) TestUtils
						.getPropertyValue(handler, "outputProcessor.processor.delegate.targetObject");
		assertThat(aggregator.getName()).isEqualTo("Bill");
	}

	private TestBean prepare(EventDrivenConsumer edc) {
		return TestUtils.getPropertyValue(serviceActivator,
				"handler.processor.delegate.targetObject", TestBean.class);
	}

	public interface InboundGateway {

		String echo();

	}

	public static class TestBean {

		private String fname;

		private String lname;

		public String getFname() {
			return fname;
		}

		public void setFname(String fname) {
			this.fname = fname;
		}

		public String getLname() {
			return lname;
		}

		public void setLname(String lname) {
			this.lname = lname;
		}

		public String printWithPrefix(String prefix) {
			return prefix + fname + " " + lname;
		}

		public String toString() {
			return fname + lname;
		}

	}

	public static class SampleAggregator {

		private String name;

		public SampleAggregator() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
