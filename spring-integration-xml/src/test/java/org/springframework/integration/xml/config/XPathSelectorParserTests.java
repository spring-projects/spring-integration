/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.integration.xml.util.XmlTestUtil;

/**
 * @author Jonas Partner
 */
public class XPathSelectorParserTests {

	@Test
	public void testSimpleStringExpressionBoolean() throws Exception {
		String contextXml = "<si-xml:xpath-selector id='selector' evaluation-result-type='boolean' ><si-xml:xpath-expression expression='/name'/></si-xml:xpath-selector>";
		MessageSelector selector =getSelector( contextXml);
		
		assertTrue(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<name>outputOne</name>"))));
		assertFalse(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<other>outputOne</other>"))));
	}
	
	@Test
	public void testStringExpressionWithNamespaceBoolean() throws Exception {
		String contextXml = "<si-xml:xpath-selector id='selector'  evaluation-result-type='boolean'><si-xml:xpath-expression expression='/ns:name' ns-prefix='ns' ns-uri='www.example.org'/> </si-xml:xpath-selector>";
		MessageSelector selector = getSelector(contextXml);
		assertTrue(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>"))));
		assertFalse(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<name>outputOne</name>"))));
	}

	@Test
	public void testStringExpressionWithNamespaceString() throws Exception {
		String contextXml = "<si-xml:xpath-selector id='selector'  evaluation-result-type='string' string-test-value='outputOne'><si-xml:xpath-expression expression='/ns:name' ns-prefix='ns' ns-uri='www.example.org'/> </si-xml:xpath-selector>";
		MessageSelector selector = getSelector(contextXml);

		assertTrue(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>"))));
		assertFalse(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<name>outputOne</name>"))));
	}

	@Test
	public void testStringExpressionWithNestedMap() throws Exception {
		StringBuffer contextXml =  new StringBuffer("<si-xml:xpath-selector id='selector' evaluation-result-type='boolean'>");
		contextXml.append("<si-xml:xpath-expression expression='/ns:name' >")
		.append("<map><entry key='ns' value='www.example.org' /></map>")
		.append("</si-xml:xpath-expression></si-xml:xpath-selector>");
		MessageSelector selector = getSelector(contextXml.toString());
		
		assertTrue(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>"))));
		assertFalse(selector.accept(new GenericMessage<Document>(XmlTestUtil.getDocumentForString("<name>outputOne</name>"))));

	}


	public MessageSelector getSelector( String testcontextXml) throws Exception{
		TestXmlApplicationContext ctx = 
			TestXmlApplicationContextHelper.getTestAppContext(testcontextXml);
		return (MessageSelector) ctx.getBean("selector");

	}

}
