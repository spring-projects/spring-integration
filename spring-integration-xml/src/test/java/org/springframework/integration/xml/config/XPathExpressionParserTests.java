/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.Test;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class XPathExpressionParserTests {

	@Test
	public void testSimpleStringExpression() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' expression='/name' />";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("outputOne");
	}

	@Test
	public void testNamespacedStringExpression() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' ns-prefix='ns1' ns-uri='www.example.org' />";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");
	}

	@Test
	public void testStringExpressionWithNamespaceMapReference() throws Exception {
		StringBuffer xmlDoc = new StringBuffer("<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' namespace-map='myNamespaces' />");
		xmlDoc.append("<util:map id='myNamespaces'><entry key='ns1' value='www.example.org' /></util:map>");
		XPathExpression xPathExpression = getXPathExpression(xmlDoc.toString());
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");
	}

	@Test
	public void testStringExpressionWithNamespaceInnerBean() throws Exception {

		StringBuilder xmlDoc = new StringBuilder("<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name'>")
				.append("    <map><entry key='ns1' value='www.example.org' /></map>")
				.append("</si-xml:xpath-expression>");

		XPathExpression xPathExpression = getXPathExpression(xmlDoc.toString());
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");
	}

	@Test
	public void testStringExpressionWithMultipleNamespaceInnerBean() throws Exception {

		StringBuilder xmlDoc = new StringBuilder(
				"<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' >")
				.append("    <map><entry key='ns1' value='www.example.org'  /></map>")
				.append("    <map><entry key='ns2' value='www.example2.org' /></map>")
				.append("</si-xml:xpath-expression>");

		try {
			getXPathExpression(xmlDoc.toString());
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getCause() instanceof SAXParseException).isTrue();
			return;
		}

		fail("Expected an Exceptions");

	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testNamespacePrefixButNoUri() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' ns-prefix='ns1' />";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");

	}

	@Test
	public void testNamespacedStringExpressionWithNamespaceMapReference() throws Exception {
		StringBuilder xmlDoc = new StringBuilder("<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' ns-prefix='ns1' ns-uri='www.example.org' namespace-map='myNamespaces'/>");
		xmlDoc.append("<util:map id='myNamespaces'><entry key='ns1' value='www.example.org' /></util:map>");

		try {
			getXPathExpression(xmlDoc.toString());
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage())
					.isEqualTo("It is not valid to specify both, the namespace attributes ('ns-prefix' and 'ns-uri') " +
							"and the 'namespace-map' attribute.");
			return;
		}

		fail("Expected an Exceptions");

	}

	@Test
	public void testNamespacedStringExpressionWithNamespaceInnerBean() throws Exception {
		StringBuilder xmlDoc = new StringBuilder(
				"<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' ns-prefix='ns1' ns-uri='www.example.org'>")
				.append("    <map><entry key='ns1' value='www.example.org' /></map>")
				.append("</si-xml:xpath-expression>");
		try {
			getXPathExpression(xmlDoc.toString());
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage())
					.isEqualTo("It is not valid to specify both, the namespace attributes ('ns-prefix' and 'ns-uri') and the 'map' sub-element.");
			return;
		}

		fail("Expected an Exceptions");

	}

	@Test
	public void testStringExpressionWithNamespaceInnerBeanAndWithNamespaceMapReference() throws Exception {
		StringBuilder xmlDoc = new StringBuilder(
				"<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' namespace-map='myNamespaces'>")
				.append("    <map><entry key='ns1' value='www.example.org' /></map>")
				.append("</si-xml:xpath-expression>")
				.append("<util:map id='myNamespaces'><entry key='ns1' value='www.example.org' /></util:map>");
		try {
			getXPathExpression(xmlDoc.toString());
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getCause().getMessage())
					.isEqualTo("It is not valid to specify both, the 'namespace-map' attribute and the 'map' sub-element.");
			return;
		}

		fail("Expected an Exceptions");

	}

	public XPathExpression getXPathExpression(String contextXml) {
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper.getTestAppContext(contextXml);
		return (XPathExpression) ctx.getBean("xpathExpression");
	}

}
