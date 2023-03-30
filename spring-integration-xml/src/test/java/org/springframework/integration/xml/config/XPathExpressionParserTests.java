/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 *
 */
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
		String xmlDoc = """
				<si-xml:xpath-expression id='xpathExpression'
								expression='/ns1:name'
								ns-prefix='ns1'
								ns-uri='www.example.org' />
				""";
		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");
	}

	@Test
	public void testStringExpressionWithNamespaceMapReference() throws Exception {
		XPathExpression xPathExpression = getXPathExpression("""
				<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' namespace-map='myNamespaces' />

				<util:map id='myNamespaces'>
					<entry key='ns1' value='www.example.org' />
				</util:map>
				""");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");
	}

	@Test
	public void testStringExpressionWithNamespaceInnerBean() throws Exception {
		String xmlDoc = """
				<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name'>
					<map>
						<entry key='ns1' value='www.example.org' />
					</map>
				</si-xml:xpath-expression>
				""";

		XPathExpression xPathExpression = getXPathExpression(xmlDoc);
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil
				.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>")))
				.isEqualTo("outputOne");
		assertThat(xPathExpression.evaluateAsString(XmlTestUtil.getDocumentForString("<name>outputOne</name>")))
				.isEqualTo("");
	}

	@Test
	public void testStringExpressionWithMultipleNamespaceInnerBean() {

		String xmlDoc = """
				<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name'>
					<map>
						<entry key='ns1' value='www.example.org' />
					</map>
					<map>
						<entry key='ns2' value='www.example2.org' />
					</map>
				</si-xml:xpath-expression>
				""";

		try {
			getXPathExpression(xmlDoc);
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getCause() instanceof SAXParseException).isTrue();
			return;
		}

		fail("Expected an Exceptions");

	}

	@Test
	public void testNamespacePrefixButNoUri() throws Exception {
		String xmlDoc = "<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' ns-prefix='ns1' />";
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> getXPathExpression(xmlDoc))
				.withStackTraceContaining("Both 'ns-prefix' and 'ns-uri' must be specified if one is specified.");
	}

	@Test
	public void testNamespacedStringExpressionWithNamespaceMapReference() {
		String xmlDoc = """
				<si-xml:xpath-expression id='xpathExpression'
						expression='/ns1:name'
						ns-prefix='ns1'
						ns-uri='www.example.org'
						namespace-map='myNamespaces'/>

				<util:map id='myNamespaces'>
					<entry key='ns1' value='www.example.org' />
				</util:map>
				""";

		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> getXPathExpression(xmlDoc))
				.withStackTraceContaining("It is not valid to specify both, the namespace attributes " +
						"('ns-prefix' and 'ns-uri') and the 'namespace-map' attribute.");
	}

	@Test
	public void testNamespacedStringExpressionWithNamespaceInnerBean() {
		String xmlDoc = """
				<si-xml:xpath-expression id='xpathExpression'
									expression='/ns1:name'
									ns-prefix='ns1'
									ns-uri='www.example.org'>
					<map>
						<entry key='ns1' value='www.example.org' />
					</map>
				</si-xml:xpath-expression>
				""";
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> getXPathExpression(xmlDoc))
				.withStackTraceContaining("It is not valid to specify both, the namespace " +
						"attributes ('ns-prefix' and 'ns-uri') and the 'map' sub-element.");
	}

	@Test
	public void testStringExpressionWithNamespaceInnerBeanAndWithNamespaceMapReference() {
		String xmlDoc = """
				<si-xml:xpath-expression id='xpathExpression' expression='/ns1:name' namespace-map='myNamespaces'>
					<map>
						<entry key='ns1' value='www.example.org' />
					</map>
				</si-xml:xpath-expression>

				<util:map id='myNamespaces'>
					<entry key='ns1' value='www.example.org' />
				</util:map>
				""";
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> getXPathExpression(xmlDoc))
				.withStackTraceContaining(
						"It is not valid to specify both, the 'namespace-map' attribute and the 'map' sub-element.");
	}

	public XPathExpression getXPathExpression(String contextXml) {
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper.getTestAppContext(contextXml);
		return ctx.getBean("xpathExpression", XPathExpression.class);
	}

}
