/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.IOException;

import org.springframework.messaging.Message;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.support.MarshallingUtils;

/**
 * An outbound Messaging Gateway for invoking Web Services that also supports
 * marshalling and unmarshalling of the request and response messages.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @see Marshaller
 * @see Unmarshaller
 */
public class MarshallingWebServiceOutboundGateway extends AbstractWebServiceOutboundGateway {

	private volatile Marshaller marshaller;
	private volatile Unmarshaller unmarshaller;

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller, Unmarshaller unmarshaller, WebServiceMessageFactory messageFactory) {
		super(destinationProvider, messageFactory);
		this.configureMarshallers(marshaller, unmarshaller);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(destinationProvider, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller, WebServiceMessageFactory messageFactory) {
		this(destinationProvider, marshaller, null, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(DestinationProvider destinationProvider, Marshaller marshaller) {
		this(destinationProvider, marshaller, (WebServiceMessageFactory) null);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, Unmarshaller unmarshaller, WebServiceMessageFactory messageFactory) {
		super(uri, messageFactory);
		this.configureMarshallers(marshaller, unmarshaller);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, Unmarshaller unmarshaller) {
		this(uri, marshaller, unmarshaller, null);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller, WebServiceMessageFactory messageFactory) {
		this(uri, marshaller, null, messageFactory);
	}

	public MarshallingWebServiceOutboundGateway(String uri, Marshaller marshaller) {
		this(uri, marshaller, (WebServiceMessageFactory) null);
	}

	@Override
	protected Object doHandle(String uri, Message<?> requestMessage, WebServiceMessageCallback requestCallback) {
		Object reply = this.getWebServiceTemplate().sendAndReceive(uri,
				new MarshallingRequestMessageCallback(requestCallback, requestMessage), new MarshallingResponseMessageExtractor());
		return reply;
	}

	@Override
	public String getComponentType() {
		return "ws:outbound-gateway(marshaling)";
	}

	/**
	 * Sets the provided Marshaller and Unmarshaller on this gateway's WebServiceTemplate.
	 * Neither may be null.
	 *
	 * @param marshaller The marshaller.
	 * @param unmarshaller The unmarshaller.
	 */
	private void configureMarshallers(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "marshaller must not be null");
		if (unmarshaller == null){
			Assert.isInstanceOf(Unmarshaller.class, marshaller,
					"Marshaller [" + marshaller + "] does not implement the Unmarshaller interface. " +
					"Please set an Unmarshaller explicitly by using one of the constructors that accepts " +
					"both Marshaller and Unmarshaller arguments.");
			unmarshaller = (Unmarshaller) marshaller;
		}

		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}

	private class MarshallingRequestMessageCallback extends RequestMessageCallback {

		public MarshallingRequestMessageCallback(WebServiceMessageCallback requestCallback, Message<?> requestMessage){
			super(requestCallback, requestMessage);
		}

		@Override
		public void doWithMessageInternal(WebServiceMessage message, Object payload) throws IOException{
			MarshallingUtils.marshal(marshaller, payload, message);
		}
	}

	private class MarshallingResponseMessageExtractor extends ResponseMessageExtractor {

		@Override
		public Object doExtractData(WebServiceMessage message) throws IOException{
			return MarshallingUtils.unmarshal(unmarshaller, message);
		}
	}
}
