/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.ws.dsl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.util.JavaUtils;
import org.springframework.integration.ws.AbstractWebServiceOutboundGateway;
import org.springframework.integration.ws.SoapHeaderMapper;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * The base {@link MessageHandlerSpec} for {@link AbstractWebServiceOutboundGateway}s.
 *
 * @param <S> the target {@link BaseWsOutboundGatewaySpec} implementation type.
 * @param <E> the target {@link AbstractWebServiceOutboundGateway} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.3
 *
 */
public abstract class BaseWsOutboundGatewaySpec<
		S extends BaseWsOutboundGatewaySpec<S, E>, E extends AbstractWebServiceOutboundGateway>
	extends MessageHandlerSpec<S, E> {

	private final Map<String, Expression> uriVariableExpressions = new HashMap<>();

	protected WebServiceTemplate template; // NOSONAR

	protected DestinationProvider destinationProvider; // NOSONAR

	protected String uri; // NOSONAR

	protected WebServiceMessageFactory webServiceMessageFactory; // NOSONAR

	private SoapHeaderMapper headerMapper;

	private DefaultUriBuilderFactory.EncodingMode encodingMode;

	private boolean ignoreEmptyResponses = true;

	private WebServiceMessageCallback requestCallback;

	protected FaultMessageResolver faultMessageResolver; // NOSONAR

	protected WebServiceMessageSender[] messageSenders; // NOSONAR

	protected ClientInterceptor[] gatewayInterceptors; // NOSONAR

	protected boolean extractPayload = true; // NOSONAR

	/**
	 * Configure with a destination provider;
	 * @param destinationProvider the destination provider.
	 * @return the spec.
	 */
	public S destinationProvider(DestinationProvider destinationProvider) {
		this.destinationProvider = destinationProvider;
		return _this();
	}

	/**
	 * Configure with a URI.
	 * @param uri the uri.
	 * @return the spec.
	 */
	public S uri(String uri) {
		this.uri = uri;
		return _this();
	}

	/**
	 * Configure the header mapper.
	 * @param headerMapper the mapper.
	 * @return the spec.
	 */
	public S headerMapper(SoapHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
		return _this();
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 * @param uriVariableExpressions The URI variable expressions.
	 * @return the spec.
	 */
	public S uriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		this.uriVariableExpressions.putAll(uriVariableExpressions);
		return _this();
	}

	/**
	 * Specify a {@link DefaultUriBuilderFactory.EncodingMode} for uri construction.
	 * @param encodingMode to use for uri construction.
	 * @return the spec
	 */
	public S encodingMode(DefaultUriBuilderFactory.EncodingMode encodingMode) {
		this.encodingMode = encodingMode;
		return _this();
	}

	/**
	 * Specify whether empty String response payloads should be ignored.
	 * The default is <code>true</code>. Set this to <code>false</code> if
	 * you want to send empty String responses in reply Messages.
	 * @param ignoreEmptyResponses true if empty responses should be ignored.
	 * @return the spec.
	 */
	public S ignoreEmptyResponses(boolean ignoreEmptyResponses) {
		this.ignoreEmptyResponses = ignoreEmptyResponses;
		return _this();
	}

	/**
	 * Specify the {@link WebServiceMessageCallback} to use.
	 * @param requestCallback the call back.
	 * @return the spec.
	 */
	public S requestCallback(WebServiceMessageCallback requestCallback) {
		this.requestCallback = requestCallback;
		return _this();
	}

	@Override
	protected E doGet() {
		return assemble(create());
	}

	protected abstract E create();

	protected E assemble(E gateway) {
		gateway.setUriVariableExpressions(this.uriVariableExpressions);
		JavaUtils.INSTANCE
			.acceptIfNotNull(this.headerMapper, gateway::setHeaderMapper);
		gateway.setEncodingMode(this.encodingMode);
		gateway.setIgnoreEmptyResponses(this.ignoreEmptyResponses);
		gateway.setRequestCallback(this.requestCallback);
		return gateway;
	}

}
