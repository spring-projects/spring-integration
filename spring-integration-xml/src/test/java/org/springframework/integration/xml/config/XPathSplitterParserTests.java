/*
 * Copyright 2014-2022 the original author or authors.
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
