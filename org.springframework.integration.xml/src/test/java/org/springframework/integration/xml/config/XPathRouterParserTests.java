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

package org.springframework.integration.xml.config;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class XPathRouterParserTests extends AbstractJUnit4SpringContextTests{

	@Autowired @Qualifier("inputOne")
	MessageChannel inputOne;

	@Autowired @Qualifier("inputTwo")
	MessageChannel inputTwo;

	@Autowired @Qualifier("outputOne")
	PollableChannel outputOne;

	@Autowired @Qualifier("outputTwo")
	PollableChannel outputTwo;

	@Autowired @Qualifier("errors")
	PollableChannel errors;


	@SuppressWarnings("unchecked")
	@Test
	public void testOutputOne() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		inputOne.send(docMessage);
		GenericMessage<Document> received = (GenericMessage<Document>) outputOne.receive(1000);
		assertNotNull("Did not recevie message from outputOne", received);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOutputTwo() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<name>outputTwo</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		inputOne.send(docMessage);
		GenericMessage<Document> received = (GenericMessage<Document>) outputTwo.receive(1000);
		assertNotNull("Did not recevie message from two", received);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOutputThree() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<name>outputThree</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		inputOne.send(docMessage);
		GenericMessage<Document> received = (GenericMessage<Document>) errors.receive(1000);
		assertNotNull("Did not recevie message on errors", received);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOutputOneMulti() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		inputTwo.send(docMessage);
		GenericMessage<Document> received = (GenericMessage<Document>) outputOne.receive(1000);
		assertNotNull("Did not recevie message from outputOne", received);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testOutputOneAndTwoMulti() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<doc><name>outputOne</name><name>outputTwo</name></doc>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		inputTwo.send(docMessage);
		GenericMessage<Document> received = (GenericMessage<Document>) outputTwo.receive(1000);
		assertNotNull("Did not recevie message from two", received);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testOutputThreeMulti() throws Exception{
		Document doc = XmlTestUtil.getDocumentForString("<name>outputThree</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		inputTwo.send(docMessage);
		GenericMessage<Document> received = (GenericMessage<Document>) errors.receive(1000);
		assertNotNull("Did not recevie message on errors", received);
	}

}
