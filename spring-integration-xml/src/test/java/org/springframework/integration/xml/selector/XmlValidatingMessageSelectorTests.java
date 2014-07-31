/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.xml.selector;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.xml.selector.XmlValidatingMessageSelector.SchemaType;

/**
 * @author Oleg Zhurakousky
 *
 */
public class XmlValidatingMessageSelectorTests {

	@Test
	public void validateCreationWithSchemaAndDefaultSchemaType() throws Exception{
		Resource resource = new ByteArrayResource("<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'/>".getBytes());
		new XmlValidatingMessageSelector(resource, (SchemaType)null);
	}

	@Test
	public void validateCreationWithSchemaAndProvidedSchemaType() throws Exception{
		Resource resource = new ByteArrayResource("<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'/>".getBytes());
		new XmlValidatingMessageSelector(resource, XmlValidatingMessageSelector.SchemaType.SCHEMA_W3C_XML);
	}

	@Test(expected=BeanCreationException.class)
	public void validateFailureInvalidSchemaLanguage() throws Exception{
		ApplicationContext context = new FileSystemXmlApplicationContext("src/test/java/org/springframework/integration/xml/selector/XmlValidatingMessageSelectorTests-context.xml");
		context.getBean("xmlValidatingMessageSelector");
	}

	@Test(expected=IllegalArgumentException.class)
	public void validateFailureWhenNoSchemaResourceProvided() throws Exception{
		new XmlValidatingMessageSelector(null, (SchemaType)null);
	}
}
