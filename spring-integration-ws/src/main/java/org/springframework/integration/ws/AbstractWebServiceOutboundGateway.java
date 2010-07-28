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

import java.io.IOException;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * Base class for outbound Web Service-invoking Messaging Gateways.
 * 
 * @author Mark Fisher
 * @author Jonas Partner
 */
public abstract class AbstractWebServiceOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final WebServiceTemplate webServiceTemplate;

	private volatile WebServiceMessageCallback requestCallback;

	private volatile boolean ignoreEmptyResponses = true;


	public AbstractWebServiceOutboundGateway(DestinationProvider destinationProvider, WebServiceMessageFactory messageFactory) {
		Assert.notNull(destinationProvider, "DestinationProvider must not be null");
        this.webServiceTemplate = (messageFactory != null) ?
				new WebServiceTemplate(messageFactory) : new WebServiceTemplate();
		this.webServiceTemplate.setDestinationProvider(destinationProvider);
	}


	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * Specify whether empty String response payloads should be ignored.
	 * The default is <code>true</code>. Set this to <code>false</code> if
	 * you want to send empty String responses in reply Messages.
	 */
	public void setIgnoreEmptyResponses(boolean ignoreEmptyResponses) {
		this.ignoreEmptyResponses = ignoreEmptyResponses;
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

	public void setInterceptors(ClientInterceptor[] interceptors) {
		this.webServiceTemplate.setInterceptors(interceptors);
	}

	protected WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	public final Object handleRequestMessage(Message<?> message) {
		Object responsePayload = this.doHandle(message.getPayload(), this.getRequestCallback(message));
		if (responsePayload != null) {
			boolean shouldIgnore = (this.ignoreEmptyResponses
					&& responsePayload instanceof String && !StringUtils.hasText((String) responsePayload));
			if (!shouldIgnore) {
				return responsePayload;
			}
		}
		return null;
	}

	protected abstract Object doHandle(Object requestPayload, WebServiceMessageCallback requestCallback);


	private WebServiceMessageCallback getRequestCallback(Message<?> requestMessage) {
		String soapAction = requestMessage.getHeaders().get(WebServiceHeaders.SOAP_ACTION, String.class);
		return (soapAction != null) ?
				new TypeCheckingSoapActionCallback(soapAction, this.requestCallback) : this.requestCallback;
	}


	private static class TypeCheckingSoapActionCallback extends SoapActionCallback {

		private final WebServiceMessageCallback callbackDelegate;

		TypeCheckingSoapActionCallback(String soapAction, WebServiceMessageCallback callbackDelegate) {
			super(soapAction);
			this.callbackDelegate = callbackDelegate;
		}

		@Override
		public void doWithMessage(WebServiceMessage message) throws IOException {
			if (message instanceof SoapMessage) {
				super.doWithMessage(message);
			}
			if (this.callbackDelegate != null) {
				try {
					this.callbackDelegate.doWithMessage(message);
				}
				catch (Exception e) {
					throw new MessagingException("error occurred in WebServiceMessageCallback", e);
				}
			}
		}
	}

}
