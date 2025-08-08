/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the "p:namespace" is working for inner "bean" definition within SI components.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
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
