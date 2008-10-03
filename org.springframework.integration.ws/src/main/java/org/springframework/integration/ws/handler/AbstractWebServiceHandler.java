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

package org.springframework.integration.ws.handler;

import java.io.IOException;
import java.net.URI;

import org.springframework.integration.endpoint.AbstractMessageHandlingEndpoint;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * Base class for outbound Web Service-invoking adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractWebServiceHandler extends AbstractMessageHandlingEndpoint {

	public static final String SOAP_ACTION_PROPERTY_KEY = "_ws.soapAction";


	private final WebServiceTemplate webServiceTemplate;

	private volatile WebServiceMessageCallback requestCallback;


	public AbstractWebServiceHandler(URI uri, WebServiceMessageFactory messageFactory) {
		Assert.notNull(uri, "URI must not be null");
		this.webServiceTemplate = (messageFactory != null) ?
				new WebServiceTemplate(messageFactory) : new WebServiceTemplate();
		this.webServiceTemplate.setDefaultUri(uri.toString());
	}


	public void setMessageFactory(WebServiceMessageFactory messageFactory) {
		this.webServiceTemplate.setMessageFactory(messageFactory);
	}

	public void setRequestCallback(WebServiceMessageCallback requestCallback) {
		this.requestCallback = requestCallback;
	}

	public void setFaultMessageResolver(FaultMessageResolver faultMessageResolver) {
		this.webServiceTemplate.setFaultMessageResolver(faultMessageResolver);
	}
	
	public void setMessageSender(WebServiceMessageSender messageSender) {
		this.webServiceTemplate.setMessageSender(messageSender);
	}

	public void setMessageSenders(WebServiceMessageSender[] messageSenders) {
		this.webServiceTemplate.setMessageSenders(messageSenders);
	}
	
	protected WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	public final Message<?> handle(Message<?> message) {
		Object responsePayload = this.doHandle(message.getPayload(), this.getRequestCallback(message));
		return responsePayload != null ? new GenericMessage<Object>(responsePayload, message.getHeaders()) : null;
	}

	protected abstract Object doHandle(Object requestPayload, WebServiceMessageCallback requestCallback);

	private WebServiceMessageCallback getRequestCallback(Message<?> requestMessage) {
		if (this.requestCallback != null) {
			return this.requestCallback;
		}
		String soapAction = requestMessage.getHeaders().get(SOAP_ACTION_PROPERTY_KEY, String.class);
		return (soapAction != null) ? new TypeCheckingSoapActionCallback(soapAction) : null;
	}


	private static class TypeCheckingSoapActionCallback extends SoapActionCallback {

		TypeCheckingSoapActionCallback(String soapAction) {
			super(soapAction);
		}

		@Override
		public void doWithMessage(WebServiceMessage message) throws IOException {
			if (message instanceof SoapMessage) {
				super.doWithMessage(message);
			}
		}
	}

}
