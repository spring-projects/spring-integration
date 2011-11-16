/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.Test;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.w3c.dom.Element;

import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.createFakeParserContext;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.loadXMLFrom;

/**
 * @author Dan Oxlade
 */
public class GemfireInboundChannelAdapterParserTest {
    private GemfireInboundChannelAdapterParser underTest = new GemfireInboundChannelAdapterParser();

    @Test(expected = BeanDefinitionParsingException.class)
    public void regionIsARequiredAttribute() throws Exception {
        String xml = "<inbound-channel-adapter />";
        Element element = loadXMLFrom(xml).getDocumentElement();
        underTest.doParse(element, createFakeParserContext(), null);
    }
}
