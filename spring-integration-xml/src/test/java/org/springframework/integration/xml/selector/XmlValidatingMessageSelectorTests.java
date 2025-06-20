/*
 * Copyright 2002-present the original author or authors.
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
