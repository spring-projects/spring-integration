/*
 * Copyright 2017-2021 the original author or authors.
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

package org.springframework.integration.http.inbound;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.core.ResolvableType;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.http.support.IntegrationWebExchangeBindException;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

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

	protected static final boolean JAXB_PRESENT = ClassUtils.isPresent("javax.xml.bind.Binder", null);

	protected static final boolean ROME_TOOLS_PRESENT = ClassUtils.isPresent("com.rometools.rome.feed.atom.Feed", null);

	protected static final List<HttpMethod> NON_READABLE_BODY_HTTP_METHODS =
			Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

	protected final AtomicInteger activeCount = new AtomicInteger(); // NOSONAR

	private final boolean expectReply;

	private Validator validator;

	private ResolvableType requestPayloadType = null;

	private HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.inboundMapper();

	private boolean extractReplyPayload = true;

	private Expression statusCodeExpression;

	private EvaluationContext evaluationContext;

	private RequestMapping requestMapping = new RequestMapping();

	private Expression payloadExpression;

	private Map<String, Expression> headerExpressions;

	private CrossOrigin crossOrigin;

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
	 * result in String while all others default to {@code byte[].class}.
	 * @param requestPayloadType The payload type.
	 */
	public void setRequestPayloadTypeClass(Class<?> requestPayloadType) {
		setRequestPayloadType(ResolvableType.forClass(requestPayloadType));
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request
	 * content is read by the converters/encoders.
	 * By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to {@code byte[].class}.
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
	 * {@link org.springframework.messaging.Message} header.
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

	/**
	 * Specify a {@link Validator} to validate a converted payload from request.
	 * @param validator the {@link Validator} to use.
	 * @since 5.2
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	protected Validator getValidator() {
		return this.validator;
	}

	@Override
	protected void onInit() {
		super.onInit();

		validateSupportedMethods();

		if (this.statusCodeExpression != null) {
			this.evaluationContext = createEvaluationContext();
		}

		getRequestMapping().setName(getComponentName());
	}

	private void validateSupportedMethods() {
		if (this.requestPayloadType != null && logger.isWarnEnabled() &&
				CollectionUtils.containsAny(NON_READABLE_BODY_HTTP_METHODS,
						Arrays.asList(getRequestMapping().getMethods()))) {

			logger.warn("The 'requestPayloadType' attribute will have no relevance for one " +
					"of the specified HTTP methods '" + NON_READABLE_BODY_HTTP_METHODS + "'");
		}
	}

	protected HttpStatus evaluateHttpStatus(HttpEntity<?> httpEntity) {
		if (this.statusCodeExpression != null) {
			Object value = this.statusCodeExpression.getValue(this.evaluationContext, httpEntity);
			return buildHttpStatus(value);
		}
		else {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
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
		return this.expectReply ? "http:inbound-gateway" : "http:inbound-channel-adapter";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return this.expectReply ? super.getIntegrationPatternType() : IntegrationPatternType.inbound_channel_adapter;
	}

	protected void validate(Object value) {
		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(value, "requestPayload");
		ValidationUtils.invokeValidator(this.validator, value, errors);
		if (errors.hasErrors()) {
			throw new IntegrationWebExchangeBindException(getComponentName(), value, errors);
		}
	}

	/**
	 * Checks if the request has a readable body (not a GET, HEAD, or OPTIONS request).
	 * @param httpMethod the HTTP method to check
	 * @return true or false if HTTP request can contain the body
	 */
	protected static boolean isReadable(@Nullable HttpMethod httpMethod) {
		return httpMethod != null && !(CollectionUtils.containsInstance(NON_READABLE_BODY_HTTP_METHODS, httpMethod));
	}

}
