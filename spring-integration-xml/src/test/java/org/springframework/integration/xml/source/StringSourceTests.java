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

import java.io.BufferedReader;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class StringSourceTests {

	private static final StringSourceFactory sourceFactory = new StringSourceFactory();

	@Test
	public void testWithDocument() throws Exception {
		String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>";
		Document doc = XmlTestUtil.getDocumentForString(docString);
		StringSource source = (StringSource) sourceFactory.createSource(doc);
		BufferedReader reader = new BufferedReader(source.getReader());
		String docAsString = reader.readLine();

		assertThat(docAsString).and(docString).areIdentical();
	}


	@Test
	public void testWithString() throws Exception {
		String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>";
		StringSource source = (StringSource) sourceFactory.createSource(docString);
		BufferedReader reader = new BufferedReader(source.getReader());
		String docAsString = reader.readLine();

		assertThat(docAsString).and(docString).areIdentical();
	}


	@Test
	public void testWithUnsupportedPayload() {
		String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>";
		StringBuffer buffer = new StringBuffer(docString);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> sourceFactory.createSource(buffer));
	}

}
