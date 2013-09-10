/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.integration.xml.source;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.BufferedReader;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.MessagingException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;

public class StringSourceTests {
	
	StringSourceFactory sourceFactory;
	
	@Before
	public void setUp() throws Exception{
		sourceFactory = new StringSourceFactory();
	}
	
	@Test
	public void testWithDocument() throws Exception{
		String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>";
		Document doc = XmlTestUtil.getDocumentForString(docString);
		StringSource source = (StringSource)sourceFactory.createSource(doc);
		BufferedReader reader = new BufferedReader(source.getReader());
		String docAsString =reader.readLine();
		assertXMLEqual("Wrong content in StringSource","<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>", docAsString);
	}
	
	
	@Test
	public void testWithString() throws Exception{
		String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>";
		StringSource source = (StringSource)sourceFactory.createSource(docString);
		BufferedReader reader = new BufferedReader(source.getReader());
		String docAsString =reader.readLine();
		assertXMLEqual("Wrong content in StringSource","<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>", docAsString);
	}
	
	
	@Test(expected=MessagingException.class)
	public void testWithUnsupportedPayload() throws Exception{
		String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>";
		StringBuffer buffer = new StringBuffer(docString);
		StringSource source = (StringSource)sourceFactory.createSource(buffer);
		BufferedReader reader = new BufferedReader(source.getReader());
		String docAsString =reader.readLine();
		assertXMLEqual("Wrong content in StringSource","<?xml version=\"1.0\" encoding=\"UTF-8\"?><item>one</item>", docAsString);
	}

}
