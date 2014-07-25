/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.gemfire.config.xml;

import static org.junit.Assert.assertEquals;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.createFakeParserContext;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.loadXMLFrom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.integration.gemfire.inbound.CacheListeningMessageProducer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Oxlade
 * @author Liujiong
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GemfireInboundChannelAdapterParserTests {

	private GemfireInboundChannelAdapterParser underTest = new GemfireInboundChannelAdapterParser();

	@Autowired
	@Qualifier("channel1.adapter")
	CacheListeningMessageProducer adapter1;

	@Test(expected = BeanDefinitionParsingException.class)
	public void regionIsARequiredAttribute() throws Exception {
		String xml = "<inbound-channel-adapter />";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.doParse(element, createFakeParserContext(), null);
	}

	@Test
	public void testPhase() {
		assertEquals(2, adapter1.getPhase());
	}

	@Test
	public void testAutoStart() {
		assertEquals(false, adapter1.isAutoStartup());
	}
}
