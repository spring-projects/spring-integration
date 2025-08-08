/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class XPathSplitterParserTests {

	@Autowired
	@Qualifier("xpathSplitter.handler")
	private MessageHandler xpathSplitter;

	@Autowired
	@Qualifier("xpathSplitter")
	private EventDrivenConsumer consumer;

	@Autowired
	@Qualifier("outputProperties")
	private Properties outputProperties;

	@Autowired
	SmartLifecycleRoleController roleController;

	@Test
	public void testXpathSplitterConfig() {
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "createDocuments", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "applySequence", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "returnIterator", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "outputProperties")).isSameAs(this.outputProperties);
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "xpathExpression").toString())
				.isEqualTo("/orders/order");
		assertThat(TestUtils.getPropertyValue(xpathSplitter, "order")).isEqualTo(2);
		assertThat(TestUtils.getPropertyValue(xpathSplitter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "discardChannelName")).isEqualTo("nullChannel");
		assertThat(TestUtils.getPropertyValue(consumer, "phase")).isEqualTo(-1);
		assertThat(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class)).isFalse();
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.getPropertyValue(roleController, "lifecycles",
				MultiValueMap.class).get("foo");
		assertThat(list).containsExactly(consumer);
	}

}
