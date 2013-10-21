/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/** 
 * @author Jonas Partner
 */
public class XPathRouterTests {

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void simpleSingleAttribute() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\" />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathRouter router = new XPathRouter(expression);
		Object[] channelNames = router.getChannelKeys(new GenericMessage(doc)).toArray();
		assertEquals("Wrong number of channels returned", 1, channelNames.length);
		assertEquals("Wrong channel name", "one", channelNames[0]);
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void simpleSingleAttributeAsString() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\" />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathRouter router = new XPathRouter(expression);
		router.setEvaluateAsString(true);
		Object[] channelNames = router.getChannelKeys(new GenericMessage(doc)).toArray();
		assertEquals("Wrong number of channels returned", 1, channelNames.length);
		assertEquals("Wrong channel name", "one", channelNames[0]);
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void simpleRootNode() throws Exception {	
		Document doc = XmlTestUtil.getDocumentForString("<doc><foo>oleg</foo><bar>bang</bar></doc>");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("name(./node())");
		XPathRouter router = new XPathRouter(expression);
		router.setEvaluateAsString(true);
		Object[] channelNames = router.getChannelKeys(new GenericMessage(doc)).toArray();
		assertEquals("Wrong number of channels returned", 1, channelNames.length);
		assertEquals("Wrong channel name", "doc", channelNames[0]);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void multipleNodeValues() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\"><book>bOne</book><book>bTwo</book></doc>");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/book");
		XPathRouter router = new XPathRouter(expression);
		Object[] channelNames = router.getChannelKeys(new GenericMessage(doc)).toArray();
		assertEquals("Wrong number of channels returned", 2, channelNames.length);
		assertEquals("Wrong channel name", "bOne", channelNames[0]);
		assertEquals("Wrong channel name", "bTwo", channelNames[1]);
	}
	

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	/*
	 * Will return only one (the first node text in the collection), since 
	 * the evaluation return type use is String (not NODESET)
	 * This test is just for sanity and the reminder that setting 'evaluateAsNode'
	 * to 'false' would still result in no exception but result will most likely be 
	 * not what is expected.
	 */
	public void multipleNodeValuesAsString() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/book");
		XPathRouter router = new XPathRouter(expression);
		router.setEvaluateAsString(true);
		Object[] channelNames = router.getChannelKeys(new GenericMessage("<doc type=\"one\"><book>bOne</book><book>bTwo</book></doc>")).toArray();
		assertEquals("Wrong number of channels returned", 1, channelNames.length);
		assertEquals("Wrong channel name", "bOne", channelNames[0]);
	}

	@Test(expected = MessagingException.class)
	public void nonNodePayload() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathRouter router = new XPathRouter(expression);
		router.getChannelKeys(new GenericMessage<String>("test"));
	}

	@Test
	public void nodePayload() throws Exception {
		XPathRouter router = new XPathRouter("./three/text()");
		Document testDocument = XmlTestUtil.getDocumentForString("<one><two><three>bob</three><three>dave</three></two></one>");
		Object[] channelNames =  router.getChannelKeys(new GenericMessage<Node>(testDocument.getElementsByTagName("two").item(0))).toArray();
		assertEquals("bob",channelNames[0]);
		assertEquals("dave",channelNames[1]);
	}
	
	@Test
	public void testSimpleDocType() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type='one' />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathRouter router = new XPathRouter(expression);
		Object channelName = router.getChannelKeys(new GenericMessage<Document>(doc)).toArray()[0];
		assertEquals("Wrong channel name", "one", channelName);
	}

	@Test
	public void testSimpleStringDoc() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathRouter router = new XPathRouter(expression);
		Object channelName = router.getChannelKeys(new GenericMessage<String>("<doc type='one' />")).toArray()[0];
		assertEquals("Wrong channel name", "one", channelName);
	}

	@Test(expected = MessagingException.class)
	public void testNonNodePayload() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathRouter router = new XPathRouter(expression);
		router.getChannelKeys(new GenericMessage<String>("test"));
	}

	@Test
	public void testNodePayload() throws Exception {
		XPathRouter router = new XPathRouter("./three/text()");
		Document testDocument = XmlTestUtil.getDocumentForString("<one><two><three>bob</three></two></one>");
		Object[] channelNames = router.getChannelKeys(new GenericMessage<Node>(testDocument
				.getElementsByTagName("two").item(0))).toArray();
		assertEquals("bob", channelNames[0]);
	}

	@Test
	public void testEvaluationReturnsEmptyString() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type='one' />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/somethingelse/@type");
		XPathRouter router = new XPathRouter(expression);
		List<Object> channelNames = router.getChannelKeys(new GenericMessage<Document>(doc));
		assertEquals(0, channelNames.size());
	}

}
