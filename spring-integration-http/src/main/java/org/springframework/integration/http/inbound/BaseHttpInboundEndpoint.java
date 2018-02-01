/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.ResolvableType;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * The {@link MessagingGatewaySupport} extension for HTTP Inbound endpoints
 * with basic properties.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class BaseHttpInboundEndpoint extends MessagingGatewaySupport implements OrderlyShutdownCapable {

	protected static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder",
			BaseHttpInboundEndpoint.class.getClassLoader());

	protected static final boolean romeToolsPresent = ClassUtils.isPresent("com.rometools.rome.feed.atom.Feed",
			BaseHttpInboundEndpoint.class.getClassLoader());

	protected static final List<HttpMethod> nonReadableBodyHttpMethods =
			Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

	protected final boolean expectReply;

	protected final AtomicInteger activeCount = new AtomicInteger();

	private volatile ResolvableType requestPayloadType = null;

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.inboundMapper();

	private volatile boolean extractReplyPayload = true;

	private volatile Expression statusCodeExpression;

	private volatile EvaluationContext evaluationContext;

	private volatile RequestMapping requestMapping = new RequestMapping();

	private volatile Expression payloadExpression;

	private volatile Map<String, Expression> headerExpressions;

	private volatile CrossOrigin crossOrigin;

	public BaseHttpInboundEndpoint(boolean expectReply) {
		super(expectReply);
		this.expectReply = expectReply;
	}

	/**
	 * Specifies a SpEL expression to evaluate in order to generate the Message payload.
	 * The EvaluationContext will be populated with an HttpEntity instance as the root object,
	 * and it may contain variables:
	 * <ul>
	 * <li><code>#pathVariables</code></li>
	 * <li><code>#requestParams</code></li>
	 * <li><code>#requestAttributes</code></li>
	 * <li><code>#requestHeaders</code></li>
	 * <li><code>#matrixVariables</code></li>
	 * <li><code>#cookies</code>
	 * </ul>
	 * @param payloadExpression The payload expression.
	 */
	public void setPayloadExpression(Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	/**
	 * Specifies a Map of SpEL expressions to evaluate in order to generate the Message headers.
	 * The keys in the map will be used as the header names. When evaluating the expression,
	 * the EvaluationContext will be populated with an HttpEntity instance as the root object,
	 * and it may contain variables:
	 * <ul>
	 * <li><code>#pathVariables</code></li>
	 * <li><code>#requestParams</code></li>
	 * <li><code>#requestAttributes</code></li>
	 * <li><code>#requestHeaders</code></li>
	 * <li><code>#matrixVariables</code></li>
	 * <li><code>#cookies</code>
	 * </ul>
	 * @param headerExpressions The header expressions.
	 */
	public void setHeaderExpressions(Map<String, Expression> headerExpressions) {
		this.headerExpressions = headerExpressions;
	}

	/**
	 * Set the {@link RequestMapping} which allows you to specify a flexible RESTFul-mapping for this endpoint.
	 * @param requestMapping The request mapping.
	 */
	public void setRequestMapping(RequestMapping requestMapping) {
		Assert.notNull(requestMapping, "requestMapping must not be null");
		this.requestMapping = requestMapping;
	}

	public final RequestMapping getRequestMapping() {
		return this.requestMapping;
	}

	/**
	 * Set the {@link CrossOrigin} to permit cross origin requests for this endpoint.
	 * @param crossOrigin the CrossOrigin config.
	 * @since 4.2
	 */
	public void setCrossOrigin(CrossOrigin crossOrigin) {
		this.crossOrigin = crossOrigin;
	}

	public CrossOrigin getCrossOrigin() {
		return this.crossOrigin;
	}

	protected Expression getPayloadExpression() {
		return this.payloadExpression;
	}

	protected Map<String, Expression> getHeaderExpressions() {
		return this.headerExpressions;
	}

	/**
	 * @return Whether to expect a reply.
	 */
	protected boolean isExpectReply() {
		return this.expectReply;
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 * @param headerMapper The header mapper.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	protected HeaderMapper<HttpHeaders> getHeaderMapper() {
		return this.headerMapper;
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request
	 * content is read by the converters/encoders.
	 * By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to <code>byte[].class</code>.
	 * @param requestPayloadType The payload type.
	 */
	public void setRequestPayloadTypeClass(Class<?> requestPayloadType) {
		setRequestPayloadType(ResolvableType.forClass(requestPayloadType));
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request
	 * content is read by the converters/encoders.
	 * By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to <code>byte[].class</code>.
	 * @param requestPayloadType The payload type.
	 */
	public void setRequestPayloadType(ResolvableType requestPayloadType) {
		this.requestPayloadType = requestPayloadType;
	}

	protected ResolvableType getRequestPayloadType() {
		return this.requestPayloadType;
	}

	/**
	 * Specify whether only the reply Message's payload should be passed in the response.
	 * If this is set to 'false', the entire Message will be used to generate the response.
	 * The default is 'true'.
	 * @param extractReplyPayload true to extract the reply payload.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	protected boolean getExtractReplyPayload() {
		return this.extractReplyPayload;
	}

	/**
	 * Specify the {@link Expression} to resolve a status code for Response to override
	 * the default '200 OK' or '500 Internal Server Error' for a timeout.
	 * @param statusCodeExpression The status code Expression.
	 * @since 5.0
	 * @see #setStatusCodeExpression(Expression)
	 */
	public void setStatusCodeExpressionString(String statusCodeExpression) {
		setStatusCodeExpression(EXPRESSION_PARSER.parseExpression(statusCodeExpression));
	}

	/**
	 * Specify the {@link Expression} to resolve a status code for Response to override
	 * the default '200 OK' or '500 Internal Server Error' for a timeout.
	 * <p>The {@link #statusCodeExpression} is applied only for the one-way
	 * {@code <http:inbound-channel-adapter/>} or when no reply (timeout) is received for
	 * a gateway. The {@code <http:inbound-gateway/>} (or whenever
	 * {@link #BaseHttpInboundEndpoint(boolean) expectReply} is true) resolves
	 * an {@link HttpStatus} from the
	 * {@link org.springframework.integration.http.HttpHeaders#STATUS_CODE} reply
	 * {@link Message} header.
	 * @param statusCodeExpression The status code Expression.
	 * @since 4.1
	 * @see #setReplyTimeout(long)
	 * @see HttpRequestHandlingEndpointSupport#HttpRequestHandlingEndpointSupport(boolean)
	 */
	public void setStatusCodeExpression(Expression statusCodeExpression) {
		this.statusCodeExpression = statusCodeExpression;
	}

	protected Expression getStatusCodeExpression() {
		return this.statusCodeExpression;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();

		validateSupportedMethods();

		if (this.statusCodeExpression != null) {
			this.evaluationContext = createEvaluationContext();
		}

		getRequestMapping().setName(getComponentName());
	}

	private void validateSupportedMethods() {
		if (this.requestPayloadType != null
				&& CollectionUtils.containsAny(nonReadableBodyHttpMethods,
				Arrays.asList(getRequestMapping().getMethods()))) {
			if (logger.isWarnEnabled()) {
				logger.warn("The 'requestPayloadType' attribute will have no relevance for one " +
						"of the specified HTTP methods '" + nonReadableBodyHttpMethods + "'");
			}
		}
	}

	protected HttpStatus evaluateHttpStatus() {
		Object value = this.statusCodeExpression.getValue(this.evaluationContext);
		return buildHttpStatus(value);
	}

	protected HttpStatus resolveHttpStatusFromHeaders(MessageHeaders headers) {
		Object httpStatusFromHeader = headers.get(org.springframework.integration.http.HttpHeaders.STATUS_CODE);
		return buildHttpStatus(httpStatusFromHeader);
	}

	private HttpStatus buildHttpStatus(Object httpStatusValue) {
		HttpStatus httpStatus = null;
		if (httpStatusValue instanceof HttpStatus) {
			httpStatus = (HttpStatus) httpStatusValue;
		}
		else if (httpStatusValue instanceof Integer) {
			httpStatus = HttpStatus.valueOf((Integer) httpStatusValue);
		}
		else if (httpStatusValue instanceof String) {
			httpStatus = HttpStatus.valueOf(Integer.parseInt((String) httpStatusValue));
		}
		return httpStatus;
	}

	protected StandardEvaluationContext createEvaluationContext() {
		return ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	public int beforeShutdown() {
		stop();
		return this.activeCount.get();
	}

	@Override
	public int afterShutdown() {
		return this.activeCount.get();
	}

	@Override
	public String getComponentType() {
		return (this.expectReply) ? "http:inbound-gateway" : "http:inbound-channel-adapter";
	}

	/**
	 * Checks if the request has a readable body (not a GET, HEAD, or OPTIONS request).
	 * @param request the HTTP request to check the method
	 * @return true or false if HTTP request can contain the body
	 */
	protected boolean isReadable(HttpRequest request) {
		return !(CollectionUtils.containsInstance(nonReadableBodyHttpMethods, request.getMethod()));
	}
}
