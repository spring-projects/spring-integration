/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.xml.config;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class XPathSplitterParserTests {

	@Autowired @Qualifier("xpathSplitter.handler")
	private MessageHandler xpathSplitter;

	@Autowired @Qualifier("xpathSplitter")
	private EventDrivenConsumer consumer;

	@Autowired @Qualifier("outputProperties")
	private Properties outputProperties;

	@Autowired
	SmartLifecycleRoleController roleController;

	@Test
	public void testXpathSplitterConfig() {
		assertTrue(TestUtils.getPropertyValue(this.xpathSplitter, "createDocuments", Boolean.class));
		assertFalse(TestUtils.getPropertyValue(this.xpathSplitter, "applySequence", Boolean.class));
		assertFalse(TestUtils.getPropertyValue(this.xpathSplitter, "iterator", Boolean.class));
		assertSame(this.outputProperties, TestUtils.getPropertyValue(this.xpathSplitter, "outputProperties"));
		assertEquals("/orders/order",
				TestUtils.getPropertyValue(this.xpathSplitter,
						"xpathExpression.xpathExpression.xpath.m_patternString",
						String.class));
		assertEquals(2, TestUtils.getPropertyValue(xpathSplitter, "order"));
		assertEquals(123L, TestUtils.getPropertyValue(xpathSplitter, "messagingTemplate.sendTimeout"));
		assertEquals(-1, TestUtils.getPropertyValue(consumer, "phase"));
		assertFalse(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class));
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.getPropertyValue(roleController, "lifecycles",
				MultiValueMap.class).get("foo");
		assertThat(list, contains((SmartLifecycle) consumer));
	}

}
