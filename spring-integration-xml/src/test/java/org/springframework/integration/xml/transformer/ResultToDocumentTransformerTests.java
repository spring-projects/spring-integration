/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.MessagingException;
import org.springframework.xml.transform.StringResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Jonas Partner
 * @author Artme Bilan
 */
public class ResultToDocumentTransformerTests {

	private static final String startDoc = """
			<?xml version="1.0" encoding="ISO-8859-1"?>
			<order>
			</order>
			""";

	private final ResultToDocumentTransformer resToDocTransformer = new ResultToDocumentTransformer();

	@Test
	public void testWithDomResult() throws Exception {
		DOMResult result = XmlTestUtil.getDomResultForString(startDoc);
		Object transformed = resToDocTransformer.transformResult(result);
		assertThat(transformed instanceof Document).as("Wrong transformed type expected Document").isTrue();
		Document doc = (Document) transformed;
		assertThat(doc.getDocumentElement().getNodeName()).as("Wrong root element name").isEqualTo("order");
	}

	@Test
	public void testWithStringResult() throws Exception {
		StringResult result = XmlTestUtil.getStringResultForString(startDoc);
		Object transformed = resToDocTransformer.transformResult(result);
		assertThat(transformed instanceof Document).as("Wrong transformed type expected Document").isTrue();
		Document doc = (Document) transformed;
		assertThat(doc.getDocumentElement().getNodeName()).as("Wrong root element name").isEqualTo("order");
	}

	@Test
	public void testWithUnsupportedSaxResult() {
		SAXResult result = new SAXResult();
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> resToDocTransformer.transformResult(result));
	}

}
