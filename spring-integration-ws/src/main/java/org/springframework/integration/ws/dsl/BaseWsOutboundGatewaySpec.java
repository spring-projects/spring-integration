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

import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.ws.AbstractWebServiceOutboundGateway;
import org.springframework.integration.ws.SoapHeaderMapper;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 * The base {@link MessageHandlerSpec} for {@link AbstractWebServiceOutboundGateway}s.
 *
 * @param <S> the target {@link BaseWsOutboundGatewaySpec} implementation type.
 * @param <E> the target {@link AbstractWebServiceOutboundGateway} implementation type.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public class BaseWsOutboundGatewaySpec<
		S extends BaseWsOutboundGatewaySpec<S, E>, E extends AbstractWebServiceOutboundGateway>
	extends MessageHandlerSpec<S, E> {

	/**
	 * Configure the header mapper.
	 * @param headerMapper the mapper.
	 * @return the spec.
	 */
	public S headerMapper(SoapHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * Set the Map of URI variable expressions to evaluate against the outbound message
	 * when replacing the variable placeholders in a URI template.
	 * @param uriVariableExpressions The URI variable expressions.
	 * @return the spec.
	 */
	public S uriVariableExpressions(Map<String, Expression> uriVariableExpressions) {
		this.target.setUriVariableExpressions(uriVariableExpressions);
		return _this();
	}

	/**
	 * Specify whether the URI should be encoded after any <code>uriVariables</code>
	 * are expanded and before sending the request. The default value is <code>true</code>.
	 * @param encodeUri true if the URI should be encoded.
	 * @return the spec.
	 * @see org.springframework.web.util.UriComponentsBuilder
	 */
	public S encodeUri(boolean encodeUri) {
		this.target.setEncodeUri(encodeUri);
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
		this.target.setIgnoreEmptyResponses(ignoreEmptyResponses);
		return _this();
	}

	/**
	 * Specify the {@link WebServiceTemplate} to use.
	 * @param webServiceTemplate the template.
	 * @return the spec.
	 */
	public S webServiceTemplate(WebServiceTemplate webServiceTemplate) {
		this.target.setWebServiceTemplate(webServiceTemplate);
		return _this();
	}

	/**
	 * Specify the {@link WebServiceMessageFactory} to use.
	 * @param messageFactory the message factory.
	 * @return the spec.
	 */
	public S messageFactory(WebServiceMessageFactory messageFactory) {
		this.target.setMessageFactory(messageFactory);
		return _this();
	}

	/**
	 * Specify the {@link WebServiceMessageCallback} to use.
	 * @param requestCallback the call back.
	 * @return the spec.
	 */
	public S requestCallback(WebServiceMessageCallback requestCallback) {
		this.target.setRequestCallback(requestCallback);
		return _this();
	}

	/**
	 * Specify the {@link FaultMessageResolver} to use.
	 * @param faultMessageResolver the resolver.
	 * @return the spec.
	 */
	public S faultMessageResolver(FaultMessageResolver faultMessageResolver) {
		this.target.setFaultMessageResolver(faultMessageResolver);
		return _this();
	}

	/**
	 * Specify the {@link WebServiceMessageSender}s to use.
	 * @param messageSenders the senders.
	 * @return the spec.
	 */
	public S messageSenders(WebServiceMessageSender... messageSenders) {
		this.target.setMessageSenders(messageSenders);
		return _this();
	}

	/**
	 * Specify the {@link ClientInterceptor}s to use.
	 * @param interceptors the interceptors.
	 * @return the spec.
	 */
	public S interceptors(ClientInterceptor... interceptors) {
		this.target.setInterceptors(interceptors);
		return _this();
	}

}
