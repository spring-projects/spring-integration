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

package org.springframework.integration.xml.selector;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class BooleanTestXpathMessageSelectorTests {

	@Test
	public void testWithSimpleString() {
		BooleanTestXPathMessageSelector selector = new BooleanTestXPathMessageSelector("boolean(/one/two)");
		assertThat(selector.accept(new GenericMessage<>("<one><two/></one>"))).isTrue();
		assertThat(selector.accept(new GenericMessage<>("<one><three/></one>"))).isFalse();
	}

	@Test
	public void testWithDocument() throws Exception {
		BooleanTestXPathMessageSelector selector = new BooleanTestXPathMessageSelector("boolean(/one/two)");
		assertThat(selector.accept(new GenericMessage<>(XmlTestUtil.getDocumentForString("<one><two/></one>"))))
				.isTrue();
		assertThat(selector.accept(new GenericMessage<>(XmlTestUtil
				.getDocumentForString("<one><three/></one>")))).isFalse();
	}

	@Test
	public void testWithNamespace() {
		var selector = new BooleanTestXPathMessageSelector("boolean(/ns1:one/ns1:two)", "ns1", "www.example.org");
		assertThat(selector
				.accept(new GenericMessage<>("<ns1:one xmlns:ns1='www.example.org'><ns1:two/></ns1:one>")))
				.isTrue();
		assertThat(
				selector.accept(
						new GenericMessage<>("""
								<ns2:one xmlns:ns2='www.example2.org'>
									<ns1:two xmlns:ns1='www.example.org' />
								</ns2:one>
								""")))
				.isFalse();
	}

	@Test
	public void testStringWithXPathExpressionProvided() {
		XPathExpression xpathExpression = XPathExpressionFactory.createXPathExpression("boolean(/one/two)");
		BooleanTestXPathMessageSelector selector = new BooleanTestXPathMessageSelector(xpathExpression);
		assertThat(selector.accept(new GenericMessage<>("<one><two/></one>"))).isTrue();
		assertThat(selector.accept(new GenericMessage<String>("<one><three/></one>"))).isFalse();
	}

	@Test
	public void testNodeWithXPathExpressionAsString() throws Exception {
		XPathExpression xpathExpression = XPathExpressionFactory.createXPathExpression("boolean(./three)");
		BooleanTestXPathMessageSelector selector = new BooleanTestXPathMessageSelector(xpathExpression);
		Document testDocument = XmlTestUtil.getDocumentForString("<one><two><three/></two></one>");
		assertThat(selector.accept(new GenericMessage<>(testDocument.getElementsByTagName("two").item(0))))
				.isTrue();
		assertThat(selector.accept(new GenericMessage<>(testDocument.getElementsByTagName("three").item(0))))
				.isFalse();
	}

}
