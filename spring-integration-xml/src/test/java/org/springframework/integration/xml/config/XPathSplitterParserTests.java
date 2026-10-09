/*
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
 * @author Glenn Renfro
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
		assertThat(TestUtils.<Boolean>getPropertyValue(this.xpathSplitter, "createDocuments")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.xpathSplitter, "applySequence")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.xpathSplitter, "returnIterator")).isFalse();
		assertThat(TestUtils.<Object>getPropertyValue(this.xpathSplitter, "outputProperties"))
				.isSameAs(this.outputProperties);
		assertThat(TestUtils.getPropertyValue(this.xpathSplitter, "xpathExpression").toString())
				.isEqualTo("/orders/order");
		assertThat(TestUtils.<Integer>getPropertyValue(xpathSplitter, "order")).isEqualTo(2);
		assertThat(TestUtils.<Long>getPropertyValue(xpathSplitter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.<String>getPropertyValue(this.xpathSplitter, "discardChannelName"))
				.isEqualTo("nullChannel");
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "phase")).isEqualTo(-1);
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "autoStartup")).isFalse();
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.<MultiValueMap<?, ?>>getPropertyValue(
				roleController, "lifecycles").get("foo");
		assertThat(list).containsExactly(consumer);
	}

}
