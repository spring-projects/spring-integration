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
package org.springframework.integration.kafka.config.xml;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Soby Chacko
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaInboundAdapterParserTests {

	@Autowired
	private ApplicationContext appContext;

	/**
	 * Test method for {@link org.springframework.integration.kafka.config.xml.KafkaInboundChannelAdapterParser#parseSource(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)}.
	 */
	@Test
	public void testParseSourceElementParserContext() throws Exception {
		final SourcePollingChannelAdapter adapter = appContext.getBean("kafkaInboundChannelAdapter",
				SourcePollingChannelAdapter.class);

		Assert.assertNotNull(adapter);
		Assert.assertFalse(adapter.isAutoStartup());
	}
}
