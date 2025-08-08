/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.source;

import java.io.BufferedReader;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class StringSourceTests {

	private static final StringSourceFactory sourceFactory = new StringSourceFactory();

	private static final String testDoc = """
			<?xml version="1.0" encoding="UTF-8"?>
			<item>one</item>
			""";

	@Test
	public void testWithDocument() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString(testDoc);
		StringSource source = (StringSource) sourceFactory.createSource(doc);
		BufferedReader reader = new BufferedReader(source.getReader());
		String docAsString = reader.readLine();

		assertThat(docAsString).and(testDoc).areIdentical();
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
		StringBuffer buffer = new StringBuffer(testDoc);
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> sourceFactory.createSource(buffer));
	}

}
