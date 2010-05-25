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

package org.springframework.integration.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.transform.TransformerException;

import org.junit.Test;

import org.springframework.integration.message.MessageBuilder;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.soap.SoapMessage;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class SimpleWebServiceOutboundGatewayTests {

	@Test // INT-1051
	public void soapActionAndCustomCallback() {
		String uri = "http://www.example.org";
		SimpleWebServiceOutboundGateway gateway = new SimpleWebServiceOutboundGateway(new TestDestinationProvider(uri));
		final AtomicReference<String> soapActionFromCallback = new AtomicReference<String>();
		gateway.setRequestCallback(new WebServiceMessageCallback() {
			public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
				SoapMessage soapMessage = (SoapMessage) message;
				soapActionFromCallback.set(soapMessage.getSoapAction());
			}
		});
		gateway.afterPropertiesSet();
		String soapActionHeaderValue = "testAction";
		String request = "<test>foo</test>";
		try {
			gateway.handleMessage(MessageBuilder.withPayload(request)
					.setHeader(WebServiceHeaders.SOAP_ACTION, soapActionHeaderValue)
					.build());
		}
		catch (Exception e) {
			// expected
		}
		assertNotNull(soapActionFromCallback.get());
		assertEquals("\"" + soapActionHeaderValue + "\"", soapActionFromCallback.get());
	}


	private static class TestDestinationProvider implements DestinationProvider {

		private final URI uri;

		TestDestinationProvider(String uri) {
			this.uri = URI.create(uri);
		}

		public URI getDestination() {
			return this.uri;
		}
	}

}
