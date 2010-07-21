/*
 * Copyright 2002-2010 the original author or authors.
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

import static junit.framework.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.core.MessagingException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
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
