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

package org.springframework.integration.ws.outbound;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.transform.TransformerException;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.ws.DefaultSoapHeaderMapper;
import org.springframework.integration.ws.SoapHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
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
 * @author Christian Tzolov
 * @author Ngoc Nhan
 * @author Jooyoung Pyoung
 */
public abstract class AbstractWebServiceOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final Lock lock = new ReentrantLock();

	protected final DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(); // NOSONAR - final

	private final @Nullable String uri;

	private final @Nullable DestinationProvider destinationProvider;

	private final Map<String, Expression> uriVariableExpressions = new HashMap<>();

	@SuppressWarnings("NullAway.Init")
	private StandardEvaluationContext evaluationContext;

	private @Nullable WebServiceMessageCallback requestCallback;

	private WebServiceTemplate webServiceTemplate;

	private boolean ignoreEmptyResponses = true;

	private SoapHeaderMapper headerMapper = new DefaultSoapHeaderMapper();

	private boolean webServiceTemplateExplicitlySet;

	public AbstractWebServiceOutboundGateway(@Nullable final String uri, @Nullable WebServiceMessageFactory messageFactory) {
		Assert.hasText(uri, "URI must not be empty");
		this.webServiceTemplate = messageFactory != null ?
				new WebServiceTemplate(messageFactory) : new WebServiceTemplate();
		this.destinationProvider = null;
		this.uri = uri;
	}

	public AbstractWebServiceOutboundGateway(DestinationProvider destinationProvider,
			@Nullable WebServiceMessageFactory messageFactory) {

		Assert.notNull(destinationProvider, "DestinationProvider must not be null");
		this.webServiceTemplate = messageFactory != null ?
				new WebServiceTemplate(messageFactory) : new WebServiceTemplate();
		this.destinationProvider = destinationProvider;
		// we always call WebServiceTemplate methods with an explicit URI argument,
		// but in case the WebServiceTemplate is accessed directly we'll set this:
		this.webServiceTemplate.setDestinationProvider(destinationProvider);
		this.uri = null;
	}

	public void setHeaderMapper(SoapHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 * @param uriVariableExpressions The URI variable expressions.
	 */
	public void setUriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		this.lock.lock();
		try {
			this.uriVariableExpressions.clear();
			this.uriVariableExpressions.putAll(uriVariableExpressions);
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Set the encoding mode to use.
	 * By default, this is set to {@link DefaultUriBuilderFactory.EncodingMode#TEMPLATE_AND_VALUES}.
	 * @param encodingMode the mode to use for uri encoding
	 * @since 5.3
	 */
	public void setEncodingMode(DefaultUriBuilderFactory.EncodingMode encodingMode) {
		Assert.notNull(encodingMode, "'encodingMode' must not be null");
		this.uriFactory.setEncodingMode(encodingMode);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		setOutputChannel(replyChannel);
	}

	/**
	 * Specify whether empty String response payloads should be ignored.
	 * The default is <code>true</code>. Set this to <code>false</code> if
	 * you want to send empty String responses in reply Messages.
	 * @param ignoreEmptyResponses true if empty responses should be ignored.
	 */
	public void setIgnoreEmptyResponses(boolean ignoreEmptyResponses) {
		this.ignoreEmptyResponses = ignoreEmptyResponses;
	}

	public void setWebServiceTemplate(WebServiceTemplate webServiceTemplate) {
		doSetWebServiceTemplate(webServiceTemplate);
	}

	protected final void doSetWebServiceTemplate(WebServiceTemplate template) {
		Assert.notNull(template, "'webServiceTemplate' must not be null");
		this.webServiceTemplate = template;
		this.webServiceTemplateExplicitlySet = true;
	}

	public void setMessageFactory(WebServiceMessageFactory messageFactory) {
		Assert.state(!this.webServiceTemplateExplicitlySet,
				() -> "'messageFactory' must be specified on the provided: " + this.webServiceTemplate);
		this.webServiceTemplate.setMessageFactory(messageFactory);
	}

	public void setRequestCallback(@Nullable WebServiceMessageCallback requestCallback) {
		this.requestCallback = requestCallback;
	}

	public void setFaultMessageResolver(FaultMessageResolver faultMessageResolver) {
		Assert.state(!this.webServiceTemplateExplicitlySet,
				() -> "'faultMessageResolver' must be specified on the provided: " + this.webServiceTemplate);
		this.webServiceTemplate.setFaultMessageResolver(faultMessageResolver);
	}

	public void setMessageSenders(WebServiceMessageSender... messageSenders) {
		Assert.state(!this.webServiceTemplateExplicitlySet,
				() -> "'messageSenders' must be specified on the provided: " + this.webServiceTemplate);
		this.webServiceTemplate.setMessageSenders(messageSenders);
	}

	public void setInterceptors(ClientInterceptor... interceptors) {
		Assert.state(!this.webServiceTemplateExplicitlySet,
				() -> "'interceptors' must be specified on the provided: " + this.webServiceTemplate);
		this.webServiceTemplate.setInterceptors(interceptors);
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		Assert.state(this.destinationProvider == null || CollectionUtils.isEmpty(this.uriVariableExpressions),
				"uri variables are not supported when a DestinationProvider is supplied.");
	}

	protected WebServiceTemplate getWebServiceTemplate() {
		return this.webServiceTemplate;
	}

	@Override
	public final @Nullable Object handleRequestMessage(Message<?> requestMessage) {
		URI uriWithVariables = prepareUri(requestMessage);
		if (uriWithVariables == null) {
			throw new MessageDeliveryException(requestMessage, "Failed to determine URI for " +
					"Web Service request in outbound gateway: " + this.getComponentName());
		}
		Object responsePayload = doHandle(uriWithVariables.toString(), requestMessage, this.requestCallback);
		if (responsePayload != null && !(this.ignoreEmptyResponses
				&& responsePayload instanceof String string
				&& !StringUtils.hasText(string))) {

			return responsePayload;
		}
		return null;
	}

	private @Nullable URI prepareUri(Message<?> requestMessage) {
		if (this.destinationProvider != null) {
			return this.destinationProvider.getDestination();
		}

		Map<String, @Nullable Object> uriVariables =
				ExpressionEvalMap.from(this.uriVariableExpressions)
						.usingEvaluationContext(this.evaluationContext)
						.withRoot(requestMessage)
						.build();

		Assert.notNull(this.uri, "'uri' must not be null");
		return this.uriFactory.expand(this.uri, uriVariables);
	}

	protected abstract @Nullable Object doHandle(String theUri, Message<?> requestMessage,
			@Nullable WebServiceMessageCallback reqCallback);

	protected abstract class RequestMessageCallback extends TransformerObjectSupport
			implements WebServiceMessageCallback {

		private final @Nullable WebServiceMessageCallback reqCallback;

		private final Message<?> requestMessage;

		public RequestMessageCallback(@Nullable WebServiceMessageCallback requestCallback, Message<?> requestMessage) {
			this.reqCallback = requestCallback;
			this.requestMessage = requestMessage;
		}

		@Override
		public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
			Object payload = this.requestMessage.getPayload();
			doWithMessageInternal(message, payload);
			if (message instanceof SoapMessage soapMessage) {
				AbstractWebServiceOutboundGateway.this.headerMapper
						.fromHeadersToRequest(this.requestMessage.getHeaders(), soapMessage);
			}
			if (this.reqCallback != null) {
				this.reqCallback.doWithMessage(message);
			}
		}

		public abstract void doWithMessageInternal(WebServiceMessage message, Object payload)
				throws IOException, TransformerException;

	}

	protected abstract class ResponseMessageExtractor extends TransformerObjectSupport
			implements WebServiceMessageExtractor<Object> {

		@Override
		public @Nullable Object extractData(WebServiceMessage message)
				throws IOException, TransformerException {

			Object resultObject = this.doExtractData(message);

			if (resultObject != null && message instanceof SoapMessage soapMessage) {
				Map<String, @Nullable Object> mappedMessageHeaders =
						AbstractWebServiceOutboundGateway.this.headerMapper.toHeadersFromReply(soapMessage);
				return getMessageBuilderFactory()
						.withPayload(resultObject)
						.copyHeaders(mappedMessageHeaders)
						.build();
			}
			else {
				return resultObject;
			}
		}

		public abstract @Nullable Object doExtractData(WebServiceMessage message) throws IOException, TransformerException;

	}

}
