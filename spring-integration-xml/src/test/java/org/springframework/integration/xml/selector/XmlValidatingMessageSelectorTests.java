/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.selector;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.selector.XmlValidatingMessageSelector.SchemaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 */
public class XmlValidatingMessageSelectorTests {

	@Test
	public void validateCreationWithSchemaAndDefaultSchemaType() throws Exception {
		Resource resource = new ByteArrayResource("<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'/>".getBytes());
		XmlValidatingMessageSelector messageSelector = new XmlValidatingMessageSelector(resource, (SchemaType) null);
		assertThat(TestUtils.getPropertyValue(messageSelector, "xmlValidator.schema").getClass().getSimpleName())
				.isEqualTo("SimpleXMLSchema");
	}

	@Test
	public void validateCreationWithSchemaAndProvidedSchemaType() throws Exception {
		Resource resource = new ByteArrayResource("<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'/>".getBytes());
		XmlValidatingMessageSelector messageSelector = new XmlValidatingMessageSelector(resource, SchemaType.XML_SCHEMA);
		assertThat(TestUtils.getPropertyValue(messageSelector, "xmlValidator.schema").getClass().getSimpleName())
				.isEqualTo("SimpleXMLSchema");
	}

	@Test
	public void validateFailureInvalidSchemaLanguage() {
		assertThatThrownBy(() -> new ClassPathXmlApplicationContext("XmlValidatingMessageSelectorTests-context.xml",
				getClass()))
				.hasStackTraceContaining("java.lang.IllegalArgumentException: No enum constant");
	}

	@Test
	public void validateFailureWhenNoSchemaResourceProvided() {
		assertThatIllegalArgumentException().isThrownBy(() -> new XmlValidatingMessageSelector(null, (SchemaType) null));
	}

}
