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

package org.springframework.integration.xml.transformer;

import static org.junit.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.MessagingException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 * @author Dave Turanski
 * @author Gunnar Hillert
 */
public class ResultToStringTransformerTests {

	private ResultToStringTransformer transformer;

	private String doc = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";


	@Before
	public void setUp() {
		transformer = new ResultToStringTransformer();
	}

	@Test
	public void testWithDomResult() throws Exception {
		DOMResult result = XmlTestUtil.getDomResultForString(doc);
		Object transformed = transformer.transformResult(result);
		assertTrue("Wrong transformed type expected String", transformed instanceof String);
		String transformedString = (String) transformed;
		assertXMLEqual("Wrong content", doc, transformedString);
	}

	@Test
	public void testWithOutputProperties() throws Exception {
		String formattedDoc = "<order><orderItem>test</orderItem></order>";
		DOMResult domResult = XmlTestUtil.getDomResultForString(doc);
		Properties outputProperties = new Properties();
		outputProperties.setProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		transformer.setOutputProperties(outputProperties);
		Object transformed = transformer.transformResult(domResult);
		assertTrue("Wrong transformed type expected String", transformed instanceof String);
		String transformedString = (String) transformed;
		assertEquals("Wrong content", formattedDoc, transformedString);
	}

	@Test
	public void testWithStringResult() throws Exception {
		StringResult result = XmlTestUtil.getStringResultForString(doc);
		Object transformed = transformer.transformResult(result);
		assertTrue("Wrong transformed type expected String", transformed instanceof String);
		String transformedString = (String) transformed;
		assertXMLEqual("Wrong content", doc, transformedString);
	}

	@Test(expected = MessagingException.class)
	public void testWithUnsupportedSaxResult() throws Exception {
		SAXResult result = new SAXResult();
		transformer.transformResult(result);
	}

}
