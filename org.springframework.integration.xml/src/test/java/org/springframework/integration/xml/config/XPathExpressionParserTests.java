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
package org.springframework.integration.xml.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;

public class XPathExpressionParserTests {
	
	@Test
	public void testSimpleStringExpression() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' xpath-expression='/name' />";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertEquals("outputOne",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")));
	}
	
	@Test
	public void testNamespacedStringExpression() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' xpath-expression='/ns1:name' ns-prefix='ns1' ns-uri='www.example.org' />";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertEquals("outputOne",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")));
		assertEquals("",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")));
	}
	
	@Test
	public void testStringExpressionWithNamespaceMapReference() throws Exception {
		StringBuffer xmlDoc = new StringBuffer("<si-xml:xpath-expression id='xpathExpression' xpath-expression='/ns1:name' namespace-map='myNamespaces' />");
		xmlDoc.append("<util:map id='myNamespaces'><entry key='ns1' value='www.example.org' /></util:map>");
		XPathExpression xPathExpression = getXPathExpression(xmlDoc.toString());
		assertEquals("outputOne",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")));
		assertEquals("",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")));
	}
	
	@Test
	public void testStringExpressionWithNamespaceInnerBean() throws Exception {
		StringBuffer xmlDoc = new StringBuffer("<si-xml:xpath-expression id='xpathExpression' xpath-expression='/ns1:name' >");
		xmlDoc.append("<map><entry key='ns1' value='www.example.org' /></map>").append("</si-xml:xpath-expression>");
		XPathExpression xPathExpression = getXPathExpression(xmlDoc.toString());
		assertEquals("outputOne",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")));
		assertEquals("",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")));
	}
	
	
	@Test(expected=BeanDefinitionStoreException.class)
	public void testNamespacePrefixButNoUri() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' xpath-expression='/ns1:name' ns-prefix='ns1' />";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertEquals("outputOne",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")));
		assertEquals("",xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")));

	}
	
	public XPathExpression getXPathExpression(String contextXml){
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper.getTestAppContext(contextXml);
		return (XPathExpression) ctx.getBean("xpathExpression");
	}


}
