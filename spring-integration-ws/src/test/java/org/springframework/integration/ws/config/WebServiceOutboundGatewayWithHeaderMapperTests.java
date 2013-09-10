/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.ws.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.AbstractWebServiceOutboundGateway;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.integration.ws.SimpleWebServiceOutboundGateway;
import org.springframework.integration.ws.WebServiceHeaders;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.xml.DomUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapMessageFactory;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.xml.namespace.QNameUtils;
import org.springframework.xml.transform.StringResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class WebServiceOutboundGatewayWithHeaderMapperTests {

	String responseSoapMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
			  "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"> " +
			  "<SOAP-ENV:Header/>" +
			  "<SOAP-ENV:Body> " +
			  "<root><name>jane</name></root>" +
			  "</SOAP-ENV:Body> " +
			  "</SOAP-ENV:Envelope>";

	String responseNonSoapMessage = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> " +
			  "<person><name>oleg</name></person>";

	@SuppressWarnings("unchecked")
	@Test
	public void headerMapperParserTest() throws Exception{
		ApplicationContext context = new ClassPathXmlApplicationContext("ws-outbound-gateway-with-headermappers.xml", this.getClass());
		SimpleWebServiceOutboundGateway gateway = TestUtils.getPropertyValue(context.getBean("withHeaderMapper"), "handler", SimpleWebServiceOutboundGateway.class);
		DefaultSoapHeaderMapper headerMapper = TestUtils.getPropertyValue(gateway, "headerMapper", DefaultSoapHeaderMapper.class);
		assertNotNull(headerMapper);

		List<String> requestHeaderNames = TestUtils.getPropertyValue(headerMapper, "requestHeaderNames", List.class);
		assertEquals(2, requestHeaderNames.size());
		assertEquals("foo*", requestHeaderNames.get(0));
		assertEquals("*baz*", requestHeaderNames.get(1));

		List<String> responseHeaderNames = TestUtils.getPropertyValue(headerMapper, "replyHeaderNames", List.class);
		assertEquals(1, responseHeaderNames.size());
		assertEquals("bar*", responseHeaderNames.get(0));
	}

	@Test
	public void withHeaderMapperString() throws Exception{
		String payload = "<root><name>bill</name></root>";
		Message<?> replyMessage = this.process(payload, "withHeaderMapper", "inputChannel", true);
		assertTrue(replyMessage.getPayload() instanceof String);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertNull(replyMessage.getHeaders().get("baz"));
	}

	@Test
	public void withHeaderMapperStringPOX() throws Exception{
		String payload = "<root><name>bill</name></root>";
		Message<?> replyMessage = this.process(payload, "withHeaderMapper", "inputChannel", false);
		assertTrue(replyMessage.getPayload() instanceof String);
		assertTrue(((String)replyMessage.getPayload()).contains("<person><name>oleg</name></person>"));
	}

	@Test
	public void withHeaderMapperSource() throws Exception{
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document document = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		DOMSource payload = new DOMSource(document);
		Message<?> replyMessage = this.process(payload, "withHeaderMapper", "inputChannel", true);
		assertTrue(replyMessage.getPayload() instanceof DOMSource);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertNull(replyMessage.getHeaders().get("baz"));
	}

	@Test
	public void withHeaderMapperSourcePOX() throws Exception{
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document document = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		DOMSource payload = new DOMSource(document);
		Message<?> replyMessage = this.process(payload, "withHeaderMapper", "inputChannel", false);
		assertTrue(replyMessage.getPayload() instanceof DOMSource);
		assertTrue(this.extractStringResult(replyMessage).contains("<person><name>oleg</name></person>"));
	}

	@Test
	public void withHeaderMapperDocument() throws Exception{
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document payload = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		Message<?> replyMessage = this.process(payload, "withHeaderMapper", "inputChannel", true);
		assertTrue(replyMessage.getPayload() instanceof Document);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertNull(replyMessage.getHeaders().get("baz"));
	}

	@Test
	public void withHeaderMapperDocumentPOX() throws Exception{
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document payload = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		Message<?> replyMessage = this.process(payload, "withHeaderMapper", "inputChannel", false);
		assertTrue(replyMessage.getPayload() instanceof Document);
		assertTrue(this.extractStringResult(replyMessage).contains("<person><name>oleg</name></person>"));
	}

	@Test
	public void withHeaderMapperAndMarshaller() throws Exception{
		Person person = new Person();
		person.setName("Bill Clinton");
		Message<?> replyMessage = this.process(person, "marshallingWithHeaderMapper", "inputMarshallingChannel", true);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertNull(replyMessage.getHeaders().get("baz"));
	}

	@SuppressWarnings("rawtypes")
	public Message<?> process(Object payload, String gatewayName, String channelName, final boolean soap) throws Exception{
		ApplicationContext context = new ClassPathXmlApplicationContext("ws-outbound-gateway-with-headermappers.xml", this.getClass());
		AbstractWebServiceOutboundGateway gateway = TestUtils.getPropertyValue(context.getBean(gatewayName), "handler", AbstractWebServiceOutboundGateway.class);

		if (!soap){
			WebServiceTemplate template = TestUtils.getPropertyValue(gateway, "webServiceTemplate", WebServiceTemplate.class);
			template.setMessageFactory(new StubMessageFactory());
		}

		WebServiceMessageSender messageSender = Mockito.mock(WebServiceMessageSender.class);
		WebServiceConnection wsConnection = Mockito.mock(WebServiceConnection.class);
		Mockito.when(messageSender.createConnection(Mockito.any(URI.class))).thenReturn(wsConnection);
		Mockito.when(messageSender.supports(Mockito.any(URI.class))).thenReturn(true);

		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          WebServiceMessage wsMessage = (WebServiceMessage) args[0];
//		          try { // uncomment if you want to see a pretty-print of SOAP message
//		        	  Transformer transformer = TransformerFactory.newInstance().newTransformer();
//			  	      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			  	      transformer.transform(new DOMSource(soapMessage.getDocument()), new StreamResult(System.out));
//		          } catch (Exception e) {
//					// ignore
//		          }
		          if (soap){
		        	  SoapHeader soapHeader = ((SoapMessage)wsMessage).getSoapHeader();
			          assertNotNull(soapHeader.getAttributeValue(QNameUtils.parseQNameString("foo")));
			          assertNotNull(soapHeader.getAttributeValue(QNameUtils.parseQNameString("foobar")));
			          assertNotNull(soapHeader.getAttributeValue(QNameUtils.parseQNameString("abaz")));
			          assertNull(soapHeader.getAttributeValue(QNameUtils.parseQNameString("bar")));
		          }

		          return null;
		      }})
		 .when(wsConnection).send(Mockito.any(WebServiceMessage.class));

		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) throws Exception{
		          Object[] args = invocation.getArguments();
		          WebServiceMessageFactory factory = (WebServiceMessageFactory) args[0];
		          String responseMessage = factory instanceof SoapMessageFactory ? responseSoapMessage : responseNonSoapMessage;
		          WebServiceMessage wsMessage = factory.createWebServiceMessage(new ByteArrayInputStream(responseMessage.getBytes()));
		          if (soap){

		        	  ((SoapMessage)wsMessage).getSoapHeader().addAttribute(QNameUtils.parseQNameString("bar"), "bar");
		        	  ((SoapMessage)wsMessage).getSoapHeader().addAttribute(QNameUtils.parseQNameString("baz"), "baz");
		          }

//		          try { // uncomment if you want to see a pretty-print of SOAP message
//		        	  Transformer transformer = TransformerFactory.newInstance().newTransformer();
//			  	      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//			  	      transformer.transform(new DOMSource(soapMessage.getDocument()), new StreamResult(System.out));
//		          } catch (Exception e) {
//					// ignore
//		          }
		          return wsMessage;
		      }})
		 .when(wsConnection).receive(Mockito.any(WebServiceMessageFactory.class));

		gateway.setMessageSender(messageSender);

		MessageChannel inputChannel = context.getBean(channelName, MessageChannel.class);
		Message<?> message =
				MessageBuilder.withPayload(payload).
				setHeader("foo", "foo").setHeader("foobar", "foobar").setHeader("abaz", "abaz").setHeader("bar", "bar").
				setHeader(WebServiceHeaders.SOAP_ACTION, "someAction").build();
		inputChannel.send(message);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		Message<?> replyMessage = outputChannel.receive(0);
		return replyMessage;
	}

	public static class Person{
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class SampleUnmarshaller implements Unmarshaller {

		public boolean supports(Class<?> clazz) {
			return true;
		}

		public Object unmarshal(Source source) throws IOException, XmlMappingException {
			Element documentElement = (Element) ((DOMSource) source).getNode();

			String name = DomUtils.getChildElementValueByTagName(documentElement, "name");
			Person person = new Person();
			person.setName(name);
			return person;
		}
	}

	private String extractStringResult(Message<?> replyMessage) throws Exception{
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringResult result = new StringResult();
        Object payload = replyMessage.getPayload();
        if (payload instanceof DOMSource){
        	transformer.transform(((DOMSource)replyMessage.getPayload()), result);
        }
        else if (payload instanceof Document){
        	transformer.transform(new DOMSource((Document)replyMessage.getPayload()), result);
        }
        else {
        	throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass().getName());
        }
        return result.toString();
	}
}
