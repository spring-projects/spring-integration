/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

import java.util.Properties;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 * @author Dave Turanski
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class ResultToStringTransformerTests {

	private ResultToStringTransformer transformer;

	private static final String doc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><order><orderItem>test</orderItem></order>";


	@BeforeEach
	public void setUp() {
		transformer = new ResultToStringTransformer();
	}

	@Test
	public void testWithDomResult() throws Exception {
		DOMResult result = XmlTestUtil.getDomResultForString(doc);
		Object transformed = transformer.transformResult(result);
		assertThat(transformed).isInstanceOf(String.class);
		String transformedString = (String) transformed;
		assertThat(transformedString).and(doc).areIdentical();
	}

	@Test
	public void testWithOutputProperties() throws Exception {
		String formattedDoc = "<order><orderItem>test</orderItem></order>";
		DOMResult domResult = XmlTestUtil.getDomResultForString(doc);
		Properties outputProperties = new Properties();
		outputProperties.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperties(outputProperties);
		Object transformed = transformer.transformResult(domResult);
		assertThat(transformed).isInstanceOf(String.class);
		String transformedString = (String) transformed;
		assertThat(transformedString).isEqualTo(formattedDoc);
	}

	@Test
	public void testWithStringResult() throws Exception {
		StringResult result = XmlTestUtil.getStringResultForString(doc);
		Object transformed = transformer.transformResult(result);
		assertThat(transformed).isInstanceOf(String.class);
		String transformedString = (String) transformed;
		assertThat(transformedString).and(doc).areIdentical();
	}

	@Test
	public void testWithUnsupportedSaxResult() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> this.transformer.transformResult(new SAXResult()));
	}

}
