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

package org.springframework.integration.http.inbound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.integration.http.multipart.MultipartHttpInputMessage;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Base class for HTTP request handling endpoints.
 * <p>
 * By default GET and POST requests are accepted via a supplied default instance
 * of {@link RequestMapping}.
 * A GET request will generate a payload containing its 'parameterMap' while a POST
 * request will be converted to a Message payload according to the registered
 * {@link HttpMessageConverter}s.
 * Several are registered by default, but the list can be explicitly set via
 * {@link #setMessageConverters(List)}.
 * <p>
 * To customize the mapping of request headers to the MessageHeaders, provide a
 * reference to a {@code HeaderMapper<HttpHeaders>} implementation
 * to the {@link #setHeaderMapper(HeaderMapper)} method.
 * <p>
 * The behavior is "request/reply" by default. Pass {@code false} to the constructor
 * to force send-only as opposed to sendAndReceive. Send-only means that as soon as
 * the Message is created and passed to the
 * {@link #setRequestChannel(org.springframework.messaging.MessageChannel) request channel},
 * a response will be generated. Subclasses determine how that response is generated
 * (e.g. simple status response or rendering a View).
 * <p>
 * In a request-reply scenario, the reply Message's payload will be extracted prior
 * to generating a response by default.
 * To have the entire serialized Message available for the response, switch the
 * {@link #extractReplyPayload} value to {@code false}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @since 2.0
 */
public abstract class HttpRequestHandlingEndpointSupport extends MessagingGatewaySupport
		implements OrderlyShutdownCapable {

	private static final boolean spring41Present = ClassUtils.isPresent("org.springframework.http.RequestEntity",
			HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder",
			HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static boolean romePresent = ClassUtils.isPresent("com.sun.syndication.feed.atom.Feed",
					HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static boolean romeToolsPresent = ClassUtils.isPresent("com.rometools.rome.feed.atom.Feed",
							HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static final List<HttpMethod> nonReadableBodyHttpMethods =
			Arrays.asList(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

	private final List<HttpMessageConverter<?>> defaultMessageConverters = new ArrayList<HttpMessageConverter<?>>();

	private volatile List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private volatile RequestMapping requestMapping = new RequestMapping();

	private volatile Class<?> requestPayloadType = null;

	private volatile boolean convertersMerged;

	private volatile boolean mergeWithDefaultConverters = false;

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.inboundMapper();

	private final boolean expectReply;

	private volatile boolean extractReplyPayload = true;

	private volatile MultipartResolver multipartResolver;

	private volatile Expression payloadExpression;

	private volatile Map<String, Expression> headerExpressions;

	private volatile boolean shuttingDown;

	private volatile Expression statusCodeExpression;

	private volatile EvaluationContext evaluationContext;

	private final AtomicInteger activeCount = new AtomicInteger();

	public HttpRequestHandlingEndpointSupport() {
		this(true);
	}

	public HttpRequestHandlingEndpointSupport(boolean expectReply) {
		this.expectReply = expectReply;
		this.defaultMessageConverters.add(new MultipartAwareFormHttpMessageConverter());
		this.defaultMessageConverters.add(new ByteArrayHttpMessageConverter());
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		this.defaultMessageConverters.add(stringHttpMessageConverter);
		this.defaultMessageConverters.add(new ResourceHttpMessageConverter());
		SourceHttpMessageConverter<Source> sourceConverter = new SourceHttpMessageConverter<Source>();
		this.defaultMessageConverters.add(sourceConverter);
		if (jaxb2Present) {
			this.defaultMessageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			if (logger.isDebugEnabled()) {
				logger.debug("'Jaxb2RootElementHttpMessageConverter' was added to the 'defaultMessageConverters'.");
			}
		}
		if (JacksonJsonUtils.isJackson2Present()) {
			this.defaultMessageConverters.add(new MappingJackson2HttpMessageConverter());
			if (logger.isDebugEnabled()) {
				logger.debug("'MappingJackson2HttpMessageConverter' was added to the 'defaultMessageConverters'.");
			}
		}
		//The 'rometools' has been introduced since Spring Framework 4.1, hence we should check the version
		// of Spring Framework using the class 'org.springframework.http.RequestEntity' from that version.
		if ((spring41Present && romeToolsPresent) || romePresent) {
			this.defaultMessageConverters.add(new AtomFeedHttpMessageConverter());
			this.defaultMessageConverters.add(new RssChannelHttpMessageConverter());
			if (logger.isDebugEnabled()) {
				logger.debug("'AtomFeedHttpMessageConverter' was added to the 'defaultMessageConverters'.");
				logger.debug("'RssChannelHttpMessageConverter' was added to the 'defaultMessageConverters'.");
			}
		}
	}

	/**
	 * @return Whether to expect a reply.
	 */
	protected boolean isExpectReply() {
		return expectReply;
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
	 * Set the message body converters to use. These converters are used to convert from and to HTTP requests and
	 * responses.
	 * @param messageConverters The message converters.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		Assert.noNullElements(messageConverters.toArray(), "'messageConverters' must not contain null entries");
		List<HttpMessageConverter<?>> localConverters = new ArrayList<HttpMessageConverter<?>>(messageConverters);
		if (this.mergeWithDefaultConverters) {
			localConverters.addAll(this.defaultMessageConverters);
			this.convertersMerged = true;
		}
		this.messageConverters = localConverters;
	}

	protected List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}


	/**
	 * Flag which determines if the default converters should be available after
	 * custom converters.
	 * @param mergeWithDefaultConverters true to merge, false to replace.
	 */
	public void setMergeWithDefaultConverters(boolean mergeWithDefaultConverters) {
		this.mergeWithDefaultConverters = mergeWithDefaultConverters;
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 * @param headerMapper The header mapper.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
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
		return requestMapping;
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request content is read by the
	 * {@link HttpMessageConverter}s. By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to <code>byte[].class</code>.
	 * @param requestPayloadType The payload type.
	 */
	public void setRequestPayloadType(Class<?> requestPayloadType) {
		this.requestPayloadType = requestPayloadType;
	}

	/**
	 * Specify whether only the reply Message's payload should be passed in the response. If this is set to 'false', the
	 * entire Message will be used to generate the response. The default is 'true'.
	 * @param extractReplyPayload true to extract the reply payload.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	/**
	 * Specify the {@link MultipartResolver} to use when checking requests. If no resolver is provided, the
	 * "multipartResolver" bean in the context will be used as a fallback. If that is not available either, this
	 * endpoint will not support multipart requests.
	 * @param multipartResolver The multipart resolver.
	 */
	public void setMultipartResolver(MultipartResolver multipartResolver) {
		this.multipartResolver = multipartResolver;
	}

	/**
	 * Specify the {@link Expression} to resolve a status code for Response
	 * to override the default '200 OK'.
	 * <p> The {@link #statusCodeExpression} is applied only for the one-way {@code <http:inbound-channel-adapter/>}.
	 * The {@code <http:inbound-gateway/>} resolves an {@link HttpStatus} from the
	 * {@link org.springframework.integration.http.HttpHeaders#STATUS_CODE} reply {@link Message} header.
	 * @param statusCodeExpression The status code Expression.
	 * @since 4.1
	 */
	public void setStatusCodeExpression(Expression statusCodeExpression) {
		this.statusCodeExpression = statusCodeExpression;
	}

	@Override
	public String getComponentType() {
		return (this.expectReply) ? "http:inbound-gateway" : "http:inbound-channel-adapter";
	}

	/**
	 * Locates the {@link MultipartResolver} bean based on the default name defined by the
	 * {@link DispatcherServlet#MULTIPART_RESOLVER_BEAN_NAME} constant if available.
	 * Sets up default converters if no converters set, or {@link #setMergeWithDefaultConverters(boolean)}
	 * was called with true after the converters were set.
	 */
	@Override
	protected void onInit() throws Exception {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (this.multipartResolver == null && beanFactory != null) {
			try {
				MultipartResolver multipartResolver = beanFactory.getBean(
						DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
				if (logger.isDebugEnabled()) {
					logger.debug("Using MultipartResolver [" + multipartResolver + "]");
				}
				this.multipartResolver = multipartResolver;
			}
			catch (NoSuchBeanDefinitionException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unable to locate MultipartResolver with name '"
							+ DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME
							+ "': no multipart request handling will be supported.");
				}
			}
		}
		if (this.messageConverters.size() == 0 || (this.mergeWithDefaultConverters && !this.convertersMerged)) {
			this.messageConverters.addAll(this.defaultMessageConverters);
		}
		this.validateSupportedMethods();

		if (this.expectReply && this.statusCodeExpression != null) {
			logger.warn("The 'statusCodeExpression' is ignored when " +
					"this component is configured as request/reply gateway");
		}

		if (this.statusCodeExpression != null) {
			this.evaluationContext = createEvaluationContext();
		}
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel. If this gateway's
	 * 'expectReply' property is true, it will also generate a response from the reply Message once received.
	 * @param servletRequest The servlet request.
	 * @param servletResponse The servlet response.
	 * @return The response Message.
	 * @throws IOException Any IOException.
	 */
	protected final Message<?> doHandleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws IOException {
		if (this.isShuttingDown()) {
			return createServiceUnavailableResponse();
		}
		else {
			return actualDoHandleRequest(servletRequest, servletResponse);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private Message<?> actualDoHandleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws IOException {
		this.activeCount.incrementAndGet();
		try {
			ServletServerHttpRequest request = this.prepareRequest(servletRequest);

			Object requestBody = null;
			if (this.isReadable(request)) {
				requestBody = this.extractRequestBody(request);
			}
			HttpEntity httpEntity = new HttpEntity(requestBody, request.getHeaders());

			StandardEvaluationContext evaluationContext = this.createEvaluationContext();
			evaluationContext.setRootObject(httpEntity);

			evaluationContext.setVariable("requestAttributes", RequestContextHolder.currentRequestAttributes());

			MultiValueMap<String, String> requestParams = this.convertParameterMap(servletRequest.getParameterMap());
			evaluationContext.setVariable("requestParams", requestParams);

			evaluationContext.setVariable("requestHeaders", new ServletServerHttpRequest(servletRequest).getHeaders());

			Cookie[] requestCookies = servletRequest.getCookies();
			if (!ObjectUtils.isEmpty(requestCookies)) {
				Map<String, Cookie> cookies = new HashMap<String, Cookie>(requestCookies.length);
				for (Cookie requestCookie : requestCookies) {
					cookies.put(requestCookie.getName(), requestCookie);
				}
				evaluationContext.setVariable("cookies", cookies);
			}

			Map<String, String> pathVariables =
					(Map<String, String>) servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

			if (!CollectionUtils.isEmpty(pathVariables)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Mapped path variables: " + pathVariables);
				}
				evaluationContext.setVariable("pathVariables", pathVariables);
			}

			Map<String, MultiValueMap<String, String>> matrixVariables =
					(Map<String, MultiValueMap<String, String>>) servletRequest
							.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

			if (!CollectionUtils.isEmpty(matrixVariables)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Mapped matrix variables: " + matrixVariables);
				}
				evaluationContext.setVariable("matrixVariables", matrixVariables);
			}

			Map<String, Object> headers = this.headerMapper.toHeaders(request.getHeaders());
			Object payload = null;
			if (this.payloadExpression != null) {
				// create payload based on SpEL
				payload = this.payloadExpression.getValue(evaluationContext);
			}
			if (!CollectionUtils.isEmpty(this.headerExpressions)) {
				for (String headerName : this.headerExpressions.keySet()) {
					Expression headerExpression = this.headerExpressions.get(headerName);
					Object headerValue = headerExpression.getValue(evaluationContext);
					if (headerValue != null) {
						headers.put(headerName, headerValue);
					}
				}
			}

			if (payload == null) {
				if (requestBody != null) {
					payload = requestBody;
				}
				else {
					payload = requestParams;
				}
			}

			AbstractIntegrationMessageBuilder<?> messageBuilder = null;

			if (payload instanceof Message<?>) {
				messageBuilder = this.getMessageBuilderFactory().fromMessage((Message<?>) payload)
						.copyHeadersIfAbsent(headers);
			}
			else {
				messageBuilder = this.getMessageBuilderFactory().withPayload(payload).copyHeaders(headers);
			}

			Message<?> message = messageBuilder
					.setHeader(org.springframework.integration.http.HttpHeaders.REQUEST_URL,
							request.getURI().toString())
					.setHeader(org.springframework.integration.http.HttpHeaders.REQUEST_METHOD,
							request.getMethod().toString())
					.setHeader(org.springframework.integration.http.HttpHeaders.USER_PRINCIPAL,
							servletRequest.getUserPrincipal())
					.build();

			Message<?> reply = null;
			if (this.expectReply) {
				reply = this.sendAndReceiveMessage(message);
			}
			else {
				this.send(message);
			}
			return reply;
		}
		finally {
			this.postProcessRequest(servletRequest);
			this.activeCount.decrementAndGet();
		}
	}

	private Message<?> createServiceUnavailableResponse() {
		if (logger.isDebugEnabled()) {
			logger.debug("Endpoint is shutting down; returning status " + HttpStatus.SERVICE_UNAVAILABLE);
		}
		return this.getMessageBuilderFactory().withPayload("Endpoint is shutting down")
				.setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE, HttpStatus.SERVICE_UNAVAILABLE)
				.build();
	}

	/**
	 * Converts the reply message to the appropriate HTTP reply object and
	 * sets up the {@link ServletServerHttpResponse}.
	 * @param response     The ServletServerHttpResponse.
	 * @param replyMessage The reply message.
	 * @return The message payload (if {@link #extractReplyPayload}) otherwise the message.
	 */
	protected final Object setupResponseAndConvertReply(ServletServerHttpResponse response, Message<?> replyMessage) {

		this.headerMapper.fromHeaders(replyMessage.getHeaders(), response.getHeaders());
		HttpStatus httpStatus = this.resolveHttpStatusFromHeaders(replyMessage.getHeaders());
		if (httpStatus != null) {
			response.setStatusCode(httpStatus);
		}

		Object reply = replyMessage;
		if (this.extractReplyPayload) {
			reply = replyMessage.getPayload();
		}
		return reply;

	}

	protected void setStatusCodeIfNeeded(ServletServerHttpResponse response) {
		if (this.statusCodeExpression != null) {
			if (this.evaluationContext == null) {
				this.evaluationContext = createEvaluationContext();
			}
			Object value = this.statusCodeExpression.getValue(this.evaluationContext);
			HttpStatus httpStatus = buildHttpStatus(value);
			if (httpStatus != null) {
				response.setStatusCode(httpStatus);
			}
		}
	}

	/**
	 * Prepares an instance of {@link ServletServerHttpRequest} from the raw
	 * {@link HttpServletRequest}. Also converts the request into a multipart request to
	 * make multiparts available if necessary. If no multipart resolver is set,
	 * simply returns the existing request.
	 * @param servletRequest current HTTP request
	 * @return the processed request (multipart wrapper if necessary)
	 * @see MultipartResolver#resolveMultipart
	 */
	private ServletServerHttpRequest prepareRequest(HttpServletRequest servletRequest) {
		if (servletRequest instanceof MultipartHttpServletRequest) {
			return new MultipartHttpInputMessage((MultipartHttpServletRequest) servletRequest);
		}
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(servletRequest)) {
			return new MultipartHttpInputMessage(this.multipartResolver.resolveMultipart(servletRequest));
		}
		return new ServletServerHttpRequest(servletRequest);
	}

	/**
	 * Checks if the request has a readable body (not a GET, HEAD, or OPTIONS request) and a Content-Type header.
	 */
	private boolean isReadable(ServletServerHttpRequest request) {
		return !(CollectionUtils.containsInstance(nonReadableBodyHttpMethods, request.getMethod()))
				&& request.getHeaders().getContentType() != null;
	}

	/**
	 * Clean up any resources used by the given multipart request (if any).
	 * @param request current HTTP request
	 * @see MultipartResolver#cleanupMultipart
	 */
	private void postProcessRequest(HttpServletRequest request) {
		if (this.multipartResolver != null && request instanceof MultipartHttpServletRequest) {
			this.multipartResolver.cleanupMultipart((MultipartHttpServletRequest) request);
		}
	}

	/**
	 * Converts a servlet request's parameterMap to a {@link MultiValueMap}.
	 */
	private MultiValueMap<String, String> convertParameterMap(Map<String, String[]> parameterMap) {
		MultiValueMap<String, String> convertedMap = new LinkedMultiValueMap<String, String>(parameterMap.size());
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String[] values = entry.getValue();
			for (String value : values) {
				convertedMap.add(entry.getKey(), value);
			}
		}
		return convertedMap;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private Object extractRequestBody(ServletServerHttpRequest request) throws IOException {
		MediaType contentType = request.getHeaders().getContentType();
		Class<?> expectedType = this.requestPayloadType;
		if (expectedType == null) {
			expectedType = ("text".equals(contentType.getType())) ? String.class : byte[].class;
		}
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter.canRead(expectedType, contentType)) {
				return converter.read((Class) expectedType, request);
			}
		}
		throw new MessagingException(
				"Could not convert request: no suitable HttpMessageConverter found for expected type ["
						+ expectedType.getName() + "] and content type [" + contentType + "]");
	}

	private HttpStatus resolveHttpStatusFromHeaders(MessageHeaders headers) {
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
		return ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
	}

	private void validateSupportedMethods() {
		if (this.requestPayloadType != null
				&& CollectionUtils.containsAny(nonReadableBodyHttpMethods,
				Arrays.asList(this.requestMapping.getMethods()))) {
			if (logger.isWarnEnabled()) {
				logger.warn("The 'requestPayloadType' attribute will have no relevance for one " +
						"of the specified HTTP methods '" + nonReadableBodyHttpMethods + "'");
			}
		}
	}

	/**
	 * Lifecycle
	 */
	@Override
	protected void doStart() {
		this.shuttingDown = false;
		super.doStart();
	}

	protected boolean isShuttingDown() {
		return this.shuttingDown;
	}

	@Override
	public int beforeShutdown() {
		this.shuttingDown = true;
		return this.activeCount.get();
	}

	@Override
	public int afterShutdown() {
		return this.activeCount.get();
	}

}
