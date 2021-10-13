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

package org.springframework.integration.xml.source;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.MessagingException;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class DomSourceFactoryTests {

	private static final String docContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>testValue</root>";

	private static final DomSourceFactory sourceFactory = new DomSourceFactory();

	private static Document doc;

	@BeforeAll
	public static void setUp() throws Exception {
		StringReader reader = new StringReader(docContent);
		doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
	}

	@Test
	public void testWithDocumentPayload() throws Exception {
		Source source = sourceFactory.createSource(doc);
		assertThat(source).isNotNull();
		assertThat(source).isInstanceOf(DOMSource.class);
		assertThat(XmlTestUtil.sourceToString(source)).and(docContent).areIdentical();
	}

	@Test
	public void testWithStringPayload() throws Exception {
		Source source = sourceFactory.createSource(docContent);
		assertThat(source).isNotNull();
		assertThat(source).isInstanceOf(DOMSource.class);
		assertThat(XmlTestUtil.sourceToString(source)).and(docContent).areIdentical();
	}

	@Test
	public void testWithUnsupportedPayload() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> sourceFactory.createSource(12));
	}

}
