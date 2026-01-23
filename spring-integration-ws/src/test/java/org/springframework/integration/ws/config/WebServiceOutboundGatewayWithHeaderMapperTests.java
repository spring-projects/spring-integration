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

package org.springframework.integration.ws.config;

import java.io.ByteArrayInputStream;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.integration.ws.WebServiceHeaders;
import org.springframework.integration.ws.outbound.AbstractWebServiceOutboundGateway;
import org.springframework.integration.ws.outbound.SimpleWebServiceOutboundGateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.xml.DomUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.pox.dom.DomPoxMessageFactory;
import org.springframework.ws.soap.SoapHeader;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.SoapMessageFactory;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.xml.namespace.QNameUtils;
import org.springframework.xml.transform.StringResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class WebServiceOutboundGatewayWithHeaderMapperTests {

	private static final String responseSoapMessage = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
				<SOAP-ENV:Header/>
				<SOAP-ENV:Body>
						<root><name>jane</name></root>
				</SOAP-ENV:Body>
			</SOAP-ENV:Envelope>
			""";

	private static final String responseNonSoapMessage = """
			<?xml version="1.0" encoding="UTF-8" ?>
			<person><name>oleg</name></person>
			""";

	@Autowired
	private ApplicationContext context;

	@Test
	public void headerMapperParserTest() {
		SimpleWebServiceOutboundGateway gateway =
				TestUtils.<SimpleWebServiceOutboundGateway>getPropertyValue(this.context.getBean("withHeaderMapper"), "handler");
		DefaultSoapHeaderMapper headerMapper = TestUtils.getPropertyValue(gateway, "headerMapper");
		assertThat(headerMapper).isNotNull();

		AbstractHeaderMapper.HeaderMatcher requestHeaderMatcher =
				TestUtils.getPropertyValue(headerMapper, "requestHeaderMatcher");
		assertThat(requestHeaderMatcher.matchHeader("foo")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("foo123")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("baz")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("123baz123")).isTrue();
		assertThat(requestHeaderMatcher.matchHeader("bar")).isFalse();
		assertThat(requestHeaderMatcher.matchHeader("bar123")).isFalse();

		AbstractHeaderMapper.HeaderMatcher replyHeaderMatcher =
				TestUtils.getPropertyValue(headerMapper, "replyHeaderMatcher");
		assertThat(replyHeaderMatcher.matchHeader("foo")).isFalse();
		assertThat(replyHeaderMatcher.matchHeader("foo123")).isFalse();
		assertThat(replyHeaderMatcher.matchHeader("baz")).isFalse();
		assertThat(replyHeaderMatcher.matchHeader("123baz123")).isFalse();
		assertThat(replyHeaderMatcher.matchHeader("bar")).isTrue();
		assertThat(replyHeaderMatcher.matchHeader("bar123")).isTrue();
	}

	@Test
	public void withHeaderMapperString() throws Exception {
		String payload = "<root><name>bill</name></root>";
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", true);
		assertThat(replyMessage.getPayload()).isInstanceOf(String.class);
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("baz")).isNull();
	}

	@Test
	public void withHeaderMapperAndExtractPayloadFalse() throws Exception {
		SimpleWebServiceOutboundGateway gateway =
				this.context.getBean("withHeaderMapper.handler", SimpleWebServiceOutboundGateway.class);
		gateway.setExtractPayload(false);

		String payload = "<root><name>bill</name></root>";
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", true);
		assertThat(replyMessage.getPayload()).isInstanceOf(WebServiceMessage.class);
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("baz")).isNull();
	}

	@Test
	public void withHeaderMapperStringPOX() throws Exception {
		String payload = "<root><name>bill</name></root>";
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", false);
		assertThat(replyMessage.getPayload()).isInstanceOf(String.class);
		assertThat(replyMessage.getPayload()).asString().contains("<person><name>oleg</name></person>");
	}

	@Test
	public void withHeaderMapperSource() throws Exception {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document document = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		DOMSource payload = new DOMSource(document);
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", true);
		assertThat(replyMessage.getPayload()).isInstanceOf(DOMSource.class);
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("baz")).isNull();
	}

	@Test
	public void withHeaderMapperSourcePOX() throws Exception {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document document = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		DOMSource payload = new DOMSource(document);
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", false);
		assertThat(replyMessage.getPayload()).isInstanceOf(DOMSource.class);
		assertThat(this.extractStringResult(replyMessage).contains("<person><name>oleg</name></person>")).isTrue();
	}

	@Test
	public void withHeaderMapperDocument() throws Exception {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document payload = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", true);
		assertThat(replyMessage.getPayload()).isInstanceOf(Document.class);
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("baz")).isNull();
	}

	@Test
	public void withHeaderMapperDocumentPOX() throws Exception {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
		Document payload = docBuilder.parse(new ByteArrayInputStream("<root><name>bill</name></root>".getBytes()));
		Message<?> replyMessage = process(payload, "withHeaderMapper", "inputChannel", false);
		assertThat(replyMessage.getPayload()).isInstanceOf(Document.class);
		assertThat(this.extractStringResult(replyMessage).contains("<person><name>oleg</name></person>")).isTrue();
	}

	@Test
	public void withHeaderMapperAndMarshaller() throws Exception {
		Person person = new Person();
		person.setName("Bill Clinton");
		Message<?> replyMessage = process(person, "marshallingWithHeaderMapper", "inputMarshallingChannel", true);
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("baz")).isNull();
	}

	private Message<?> process(Object payload, String gatewayName, String channelName, final boolean soap)
			throws Exception {

		AbstractWebServiceOutboundGateway gateway =
				TestUtils.<AbstractWebServiceOutboundGateway>getPropertyValue(this.context.getBean(gatewayName), "handler");

		if (!soap) {
			WebServiceTemplate template =
					TestUtils.<WebServiceTemplate>getPropertyValue(gateway, "webServiceTemplate");
			template.setMessageFactory(new DomPoxMessageFactory());
		}

		WebServiceMessageSender messageSender = mock();
		WebServiceConnection wsConnection = mock();
		Mockito.when(messageSender.createConnection(any(URI.class))).thenReturn(wsConnection);
		Mockito.when(messageSender.supports(any(URI.class))).thenReturn(true);

		Mockito.doAnswer(invocation -> {

					WebServiceMessage wsMessage = invocation.getArgument(0);
//				try { // uncomment if you want to see a pretty-print of SOAP message
//					Transformer transformer = TransformerFactory.newInstance().newTransformer();
//					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//					transformer.transform(new DOMSource(soapMessage.getDocument()), new StreamResult(System.out));
//				}
//				catch (Exception e) {
//					// ignore
//				}
					if (soap) {
						SoapHeader soapHeader = ((SoapMessage) wsMessage).getSoapHeader();
						assertThat(soapHeader.getAttributeValue(QNameUtils.parseQNameString("foo"))).isNotNull();
						assertThat(soapHeader.getAttributeValue(QNameUtils.parseQNameString("foobar"))).isNotNull();
						assertThat(soapHeader.getAttributeValue(QNameUtils.parseQNameString("abaz"))).isNotNull();
						assertThat(soapHeader.getAttributeValue(QNameUtils.parseQNameString("bar"))).isNull();
					}
					return null;

				}).when(wsConnection)
				.send(any(WebServiceMessage.class));

		Mockito.doAnswer(invocation -> {
			WebServiceMessageFactory factory = invocation.getArgument(0);
			String responseMessage =
					factory instanceof SoapMessageFactory
							? responseSoapMessage
							: responseNonSoapMessage;
			WebServiceMessage wsMessage =
					factory.createWebServiceMessage(new ByteArrayInputStream(responseMessage.getBytes()));
			if (soap) {

				((SoapMessage) wsMessage).getSoapHeader().addAttribute(QNameUtils.parseQNameString("bar"), "bar");
				((SoapMessage) wsMessage).getSoapHeader().addAttribute(QNameUtils.parseQNameString("baz"), "baz");
			}

//				try { // uncomment if you want to see a pretty-print of SOAP message
//					Transformer transformer = TransformerFactory.newInstance().newTransformer();
//					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//					transformer.transform(new DOMSource(soapMessage.getDocument()), new StreamResult(System.out));
//				}
//				catch (Exception e) {
//					// ignore
//				}
			return wsMessage;
		}).when(wsConnection).receive(any(WebServiceMessageFactory.class));

		gateway.setMessageSenders(messageSender);

		MessageChannel inputChannel = context.getBean(channelName, MessageChannel.class);
		Message<?> message =
				MessageBuilder.withPayload(payload)
						.setHeader("foo", "foo")
						.setHeader("foobar", "foobar")
						.setHeader("abaz", "abaz")
						.setHeader("bar", "bar").
						setHeader(WebServiceHeaders.SOAP_ACTION, "someAction")
						.build();
		inputChannel.send(message);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		return outputChannel.receive(0);
	}

	private String extractStringResult(Message<?> replyMessage) throws Exception {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StringResult result = new StringResult();
		Object payload = replyMessage.getPayload();
		if (payload instanceof DOMSource domSource) {
			transformer.transform(domSource, result);
		}
		else if (payload instanceof Document document) {
			transformer.transform(new DOMSource(document), result);
		}
		else {
			throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass().getName());
		}
		return result.toString();
	}

	public static class Person {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class SampleUnmarshaller implements Unmarshaller {

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public Object unmarshal(Source source) throws XmlMappingException {
			Element documentElement = (Element) ((DOMSource) source).getNode();

			String name = DomUtils.getChildElementValueByTagName(documentElement, "name");
			Person person = new Person();
			person.setName(name);
			return person;
		}

	}

}
