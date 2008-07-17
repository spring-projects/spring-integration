/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.xml.router.XPathSingleChannelNameResolver;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;
import org.w3c.dom.Document;

/**
 * 
 * @author Jonas Partner
 *
 */
public class XPathSingleChannelNameResolverTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testSimpleDocType() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\" />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathSingleChannelNameResolver resolver = new XPathSingleChannelNameResolver(expression);
		String channelName = resolver.resolve(new GenericMessage(doc));
		assertEquals("Wrong channel name", "one", channelName);
	}

	@Test(expected = MessagingException.class)
	public void testNonNodePayload() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathSingleChannelNameResolver resolver = new XPathSingleChannelNameResolver(expression);
		resolver.resolve(new StringMessage("test"));
	}

}
