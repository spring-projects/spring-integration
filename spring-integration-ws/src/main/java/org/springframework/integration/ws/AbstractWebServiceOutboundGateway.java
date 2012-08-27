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

package org.springframework.integration.ws;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriTemplate;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceMessageExtractor;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.xml.transform.TransformerObjectSupport;

/**
 * Base class for outbound Web Service-invoking Messaging Gateways.
 *
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractWebServiceOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final WebServiceTemplate webServiceTemplate;

	private final UriTemplate uriTemplate;

	private final DestinationProvider destinationProvider;

	private final Map<String, Expression> uriVariableExpressions = new HashMap<String, Expression>();

	private volatile  StandardEvaluationContext evaluationContext;

	private volatile WebServiceMessageCallback requestCallback;

	private volatile boolean ignoreEmptyResponses = true;

	protected volatile SoapHeaderMapper headerMapper = new DefaultSoapHeaderMapper();

	public AbstractWebServiceOutboundGateway(final String uri, WebServiceMessageFactory messageFactory) {
		Assert.hasText(uri, "URI must not be empty");
		this.webServiceTemplate = new WebServiceTemplate(messageFactory);
		this.destinationProvider = null;
		this.uriTemplate = new UriTemplate(uri);
	}

	public AbstractWebServiceOutboundGateway(DestinationProvider destinationProvider, WebServiceMessageFactory messageFactory) {
		Assert.notNull(destinationProvider, "DestinationProvider must not be null");
		this.webServiceTemplate = new WebServiceTemplate(messageFactory);
		this.destinationProvider = destinationProvider;
		// we always call WebServiceTemplate methods with an explicit URI argument,
		// but in case the WebServiceTemplate is accessed directly we'll set this:
		this.webServiceTemplate.setDestinationProvider(destinationProvider);
		this.uriTemplate = null;
	}

	public void setHeaderMapper(SoapHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 */
	public void setUriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		synchronized (this.uriVariableExpressions) {
			this.uriVariableExpressions.clear();
			this.uriVariableExpressions.putAll(uriVariableExpressions);
		}
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

	public void setMessageSenders(WebServiceMessageSender... messageSenders) {
		this.webServiceTemplate.setMessageSenders(messageSenders);
	}

	public void setInterceptors(ClientInterceptor... interceptors) {
		this.webServiceTemplate.setInterceptors(interceptors);
	}

	@Override
	public void onInit() {
		super.onInit();
		if (this.getBeanFactory() != null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
		}
		else {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
		}
		Assert.state(this.destinationProvider == null || CollectionUtils.isEmpty(this.uriVariableExpressions),
				"uri variables are not supported when a DestinationProvider is supplied.");
	}

	protected WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	public final Object handleRequestMessage(Message<?> requestMessage) {
		URI uri = this.prepareUri(requestMessage);
		if (uri == null) {
			throw new MessageDeliveryException(requestMessage, "Failed to determine URI for " +
					"Web Service request in outbound gateway: " + this.getComponentName());
		}
		Object responsePayload = this.doHandle(uri.toString(), requestMessage, this.requestCallback);
		if (responsePayload != null) {
			boolean shouldIgnore = (this.ignoreEmptyResponses
					&& responsePayload instanceof String && !StringUtils.hasText((String) responsePayload));
			if (!shouldIgnore) {
				return responsePayload;
			}
		}
		return null;
	}

	protected abstract Object doHandle(String uri, Message<?> requestMessage, WebServiceMessageCallback requestCallback);


	private URI prepareUri(Message<?> requestMessage) {
		if (this.destinationProvider != null) {
			return this.destinationProvider.getDestination();
		}
		Map<String, Object> uriVariables = new HashMap<String, Object>();
		for (Map.Entry<String, Expression> entry : this.uriVariableExpressions.entrySet()) {
			Object value = entry.getValue().getValue(this.evaluationContext, requestMessage, String.class);
			uriVariables.put(entry.getKey(), value);
		}
		return this.uriTemplate.expand(uriVariables);
	}

	protected abstract class RequestMessageCallback extends TransformerObjectSupport implements WebServiceMessageCallback {

		private final WebServiceMessageCallback requestCallback;

		private final Message<?> requestMessage;

		public RequestMessageCallback(WebServiceMessageCallback requestCallback, Message<?> requestMessage){
			this.requestCallback = requestCallback;
			this.requestMessage = requestMessage;
		}

		public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
			Object payload = this.requestMessage.getPayload();
			if (message instanceof SoapMessage){
				this.doWithMessageInternal(message, payload);
				AbstractWebServiceOutboundGateway.this.headerMapper.fromHeadersToRequest(this.requestMessage.getHeaders(),
						(SoapMessage) message);
				if (this.requestCallback != null) {
					this.requestCallback.doWithMessage(message);
				}
			}

		}

		public abstract void doWithMessageInternal(WebServiceMessage message, Object payload) throws IOException, TransformerException;

	}

	protected abstract class ResponseMessageExtractor extends TransformerObjectSupport implements WebServiceMessageExtractor<Object> {

		public Object extractData(WebServiceMessage message)
				throws IOException, TransformerException {

			Object resultObject = this.doExtractData(message);

			if (message instanceof SoapMessage){
				Map<String, Object> mappedMessageHeaders =
						AbstractWebServiceOutboundGateway.this.headerMapper.toHeadersFromReply((SoapMessage) message);
				return MessageBuilder.withPayload(resultObject).copyHeaders(mappedMessageHeaders).build();
			}
			else {
				return resultObject;
			}
		}

		public abstract Object doExtractData(WebServiceMessage message) throws IOException, TransformerException;

	}

}
