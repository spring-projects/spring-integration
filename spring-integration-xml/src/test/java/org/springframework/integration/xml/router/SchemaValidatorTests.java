/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.xml.router;

import static org.junit.Assert.*;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringSource;

public class SchemaValidatorTests {

	
	
	
	
	@Test
	public void testValidMessageWithXsd() throws Exception{
		SchemaValidator validator = new SchemaValidator(new ClassPathResource("validationTestsSchema.xsd", SchemaValidator.class), XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source source = XmlTestUtil.getDomSourceForString("<greeting>hello</greeting>");
		assertTrue("Document expected to be valid " ,validator.isValid(source)) ;
	}
	
	@Test
	public void testInvalidMessageWithXsd() throws Exception{
		SchemaValidator validator = new SchemaValidator(new ClassPathResource("validationTestsSchema.xsd", SchemaValidator.class), XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source source = XmlTestUtil.getDomSourceForString("<notInSchema>hello</notInSchema>");
		assertFalse("Document not expected to be valid " ,validator.isValid(source)) ;
	}
	
	@Test
	public void testInvalidXml() throws Exception {
		SchemaValidator validator = new SchemaValidator(new ClassPathResource("validationTestsSchema.xsd", SchemaValidator.class), XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source source =new StringSource("something else");
		assertFalse("Document not expected to be valid " ,validator.isValid(source)) ;
	}
	
	
} 
