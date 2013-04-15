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

package org.springframework.integration.http.inbound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.expression.BeanFactoryResolver;
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
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.integration.http.multipart.MultipartHttpInputMessage;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.UrlPathHelper;

/**
 * Base class for HTTP request handling endpoints.
 * <p>
 * By default GET and POST requests are accepted, but the 'supportedMethods' property may be set to include others or
 * limit the options (e.g. POST only). A GET request will generate a payload containing its 'parameterMap' while a POST
 * request will be converted to a Message payload according to the registered {@link HttpMessageConverter}s. Several are
 * registered by default, but the list can be explicitly set via {@link #setMessageConverters(List)}.
 * <p>
 * To customize the mapping of request headers to the MessageHeaders, provide a
 * reference to a {@code HeaderMapper<HttpHeaders>} implementation
 * to the {@link #setHeaderMapper(HeaderMapper)} method.
 * <p>
 * The behavior is "request/reply" by default. Pass <code>false</code> to the constructor to force send-only as opposed
 * to sendAndReceive. Send-only means that as soon as the Message is created and passed to the
 * {@link #setRequestChannel(org.springframework.integration.MessageChannel) request channel}, a response will be
 * generated. Subclasses determine how that response is generated (e.g. simple status response or rendering a View).
 * <p>
 * In a request-reply scenario, the reply Message's payload will be extracted prior to generating a response by default.
 * To have the entire serialized Message available for the response, switch the {@link #extractReplyPayload} value to
 * <code>false</code>.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public abstract class HttpRequestHandlingEndpointSupport extends MessagingGatewaySupport
	implements OrderlyShutdownCapable {

	private static final boolean jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder",
			HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static final boolean jacksonPresent = ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper",
			HttpRequestHandlingEndpointSupport.class.getClassLoader())
			&& ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", HttpRequestHandlingEndpointSupport.class
					.getClassLoader());

	private static boolean romePresent = ClassUtils.isPresent("com.sun.syndication.feed.WireFeed",
			HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private volatile List<HttpMethod> supportedMethods = Arrays.asList(HttpMethod.GET, HttpMethod.POST);

	private volatile Class<?> requestPayloadType = null;

	private volatile List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.inboundMapper();

	private final boolean expectReply;

	private volatile String path;

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();

	private final PathMatcher pathMatcher = new AntPathMatcher();

	private volatile boolean extractReplyPayload = true;

	private volatile MultipartResolver multipartResolver;

	private volatile Expression payloadExpression;

	private volatile Map<String, Expression> headerExpressions;

	private volatile boolean shuttingDown;

	private final AtomicInteger activeCount = new AtomicInteger();

	public HttpRequestHandlingEndpointSupport() {
		this(true);
	}

	@SuppressWarnings("rawtypes")
	public HttpRequestHandlingEndpointSupport(boolean expectReply) {
		this.expectReply = expectReply;
		this.messageConverters.add(new MultipartAwareFormHttpMessageConverter());
		this.messageConverters.add(new ByteArrayHttpMessageConverter());
		this.messageConverters.add(new StringHttpMessageConverter());
		this.messageConverters.add(new ResourceHttpMessageConverter());
		this.messageConverters.add(new SourceHttpMessageConverter());
		if (jaxb2Present) {
			this.messageConverters.add(new Jaxb2RootElementHttpMessageConverter());
		}
		if (jacksonPresent) {
			this.messageConverters.add(new MappingJacksonHttpMessageConverter());
		}
		if (romePresent) {
			this.messageConverters.add(new AtomFeedHttpMessageConverter());
			this.messageConverters.add(new RssChannelHttpMessageConverter());
		}
	}

	/**
	 * @return whether to expect reply
	 */
	protected boolean isExpectReply() {
		return expectReply;
	}

	/**
	 * Set the path template for which this endpoint expects requests.
	 * May include path variable {keys} to match against.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	String getPath() {
		return path;
	}

	/**
	 * Specifies a SpEL expression to evaluate in order to generate the Message payload.
	 * The EvaluationContext will be populated with an HttpEntity instance as the root object,
	 * and it may contain one or both of the <code>#pathVariables</code> and
	 * <code>#queryParameters</code> variables if present. Those variables' values are Maps.
	 */
	public void setPayloadExpression(Expression payloadExpression) {
		this.payloadExpression = payloadExpression;
	}

	/**
	 * Specifies a Map of SpEL expressions to evaluate in order to generate the Message headers.
	 * The keys in the map will be used as the header names. When evaluating the expression,
	 * the EvaluationContext will be populated with an HttpEntity instance as the root object,
	 * and it may contain one or both of the <code>#pathVariables</code> and
	 * <code>#queryParameters</code> variables if present. Those variables' values are Maps.
	 */
	public void setHeaderExpressions(Map<String, Expression> headerExpressions) {
		this.headerExpressions = headerExpressions;
	}

	/**
	 * Set the message body converters to use. These converters are used to convert from and to HTTP requests and
	 * responses.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
		this.messageConverters = messageConverters;
	}

	protected List<HttpMessageConverter<?>> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify the supported request method names for this gateway. By default, only GET and POST are supported.
	 */
	public void setSupportedMethodNames(String... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported method is required");
		HttpMethod[] methodArray = new HttpMethod[supportedMethods.length];
		for (int i = 0; i < methodArray.length; i++) {
			methodArray[i] = HttpMethod.valueOf(supportedMethods[i].toUpperCase());
		}
		this.supportedMethods = Arrays.asList(methodArray);
	}

	/**
	 * Specify the supported request methods for this gateway. By default, only GET and POST are supported.
	 */
	public void setSupportedMethods(HttpMethod... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported method is required");
		this.supportedMethods = Arrays.asList(supportedMethods);
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request content is read by the
	 * {@link HttpMessageConverter}s. By default this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to <code>byte[].class</code>.
	 */
	public void setRequestPayloadType(Class<?> requestPayloadType) {
		this.requestPayloadType = requestPayloadType;
	}

	/**
	 * Specify whether only the reply Message's payload should be passed in the response. If this is set to 'false', the
	 * entire Message will be used to generate the response. The default is 'true'.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	/**
	 * Specify the {@link MultipartResolver} to use when checking requests. If no resolver is provided, the
	 * "multipartResolver" bean in the context will be used as a fallback. If that is not available either, this
	 * endpoint will not support multipart requests.
	 */
	public void setMultipartResolver(MultipartResolver multipartResolver) {
		this.multipartResolver = multipartResolver;
	}

	protected boolean isShuttingDown() {
		return this.shuttingDown;
	}

	@Override
	public String getComponentType() {
		return (this.expectReply) ? "http:inbound-gateway" : "http:inbound-channel-adapter";
	}

	/**
	 * Locates the {@link MultipartResolver} bean based on the default name defined by the
	 * {@link DispatcherServlet#MULTIPART_RESOLVER_BEAN_NAME} constant if available.
	 */
	@Override
	protected void onInit() throws Exception {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (this.multipartResolver == null && beanFactory != null) {
			try {
				MultipartResolver multipartResolver = this.getBeanFactory().getBean(
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

		this.validateSupportedMethods();
	}


	@Override
	protected void doStart() {
		this.shuttingDown = false;
		super.doStart();
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel. If this gateway's
	 * 'expectReply' property is true, it will also generate a response from the reply Message once received.
	 * @return a the response Message
	 */
	protected final Message<?> doHandleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
		if (this.isShuttingDown()) {
			return createServiceUnavailableResponse();
		}
		else {
			return actualDoHandleRequest(servletRequest, servletResponse);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Message<?> actualDoHandleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {
		this.activeCount.incrementAndGet();
		try {
			ServletServerHttpRequest request = this.prepareRequest(servletRequest);
			if (!this.supportedMethods.contains(request.getMethod())) {
				servletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				return null;
			}

			Object requestBody = null;
			if (this.isReadable(request)) {
				requestBody = this.extractRequestBody(request);
			}
			HttpEntity httpEntity = new HttpEntity(requestBody, request.getHeaders());

			StandardEvaluationContext evaluationContext = this.createEvaluationContext();
			evaluationContext.setRootObject(httpEntity);

			LinkedMultiValueMap<String, String> requestParams = this.convertParameterMap(servletRequest.getParameterMap());
			evaluationContext.setVariable("requestParams", requestParams);

			if (StringUtils.hasText(this.path)) {
				String lookupPath = this.urlPathHelper.getLookupPathForRequest(servletRequest);
				Map pathVariables = this.pathMatcher.extractUriTemplateVariables(this.path, lookupPath);
				if (!pathVariables.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("Mapped path variables: " + pathVariables);
					}
					evaluationContext.setVariable("pathVariables", pathVariables);
				}
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

			MessageBuilder<?> messageBuilder = null;

			if (payload instanceof Message<?>){
				messageBuilder = MessageBuilder.fromMessage((Message<?>) payload).copyHeadersIfAbsent(headers);
			}
			else {
				messageBuilder = MessageBuilder.withPayload(payload).copyHeaders(headers);
			}

			Message<?> message = messageBuilder
					.setHeader(org.springframework.integration.http.HttpHeaders.REQUEST_URL, request.getURI().toString())
					.setHeader(org.springframework.integration.http.HttpHeaders.REQUEST_METHOD, request.getMethod().toString())
					.setHeader(org.springframework.integration.http.HttpHeaders.USER_PRINCIPAL, servletRequest.getUserPrincipal())
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
		return MessageBuilder.withPayload("Endpoint is shutting down")
				.setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE, HttpStatus.SERVICE_UNAVAILABLE)
				.build();
	}

	/**
	 * Converts the reply message to the appropriate HTTP reply object and
	 * sets up the {@link ServletServerHttpResponse}.
	 *
	 * @param response The ServletServerHttpResponse.
	 * @param replyMessage The reply message.
	 * @return The message payload (if {@link #extractReplyPayload}) otherwise the
	 * message.
	 */
	protected final Object setupResponseAndConvertReply(ServletServerHttpResponse response, Message<?> replyMessage) {

		this.headerMapper.fromHeaders(replyMessage.getHeaders(), response.getHeaders());
		HttpStatus httpStatus = this.resolveHttpStatusFromHeaders(((Message<?>) replyMessage).getHeaders());
		if (httpStatus != null) {
			response.setStatusCode(httpStatus);
		}

		Object reply = replyMessage;
		if (this.extractReplyPayload) {
			reply = replyMessage.getPayload();
		}
		return reply;

	}

	/**
	 * @deprecated As of release 2.2, please use {@link #setupResponseAndConvertReply(ServletServerHttpResponse, Message)} instead.
	 */
	@Deprecated
	protected final Object setupResponseAndConvertReply(HttpServletResponse servletResponse, Message<?> replyMessage) {
		ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
		Object reply = setupResponseAndConvertReply(response, replyMessage);
		response.close();
		return reply;
	}

	/**
	 * Prepares an instance of {@link ServletServerHttpRequest} from the raw {@link HttpServletRequest}. Also converts
	 * the request into a multipart request to make multiparts available if necessary. If no multipart resolver is set,
	 * simply returns the existing request.
	 * @param request current HTTP request
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
		HttpMethod method = request.getMethod();
		if (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method) || HttpMethod.OPTIONS.equals(method)) {
			return false;
		}
		return request.getHeaders().getContentType() != null;
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
	@SuppressWarnings("rawtypes")
	private LinkedMultiValueMap<String, String> convertParameterMap(Map parameterMap) {
		LinkedMultiValueMap<String, String> convertedMap = new LinkedMultiValueMap<String, String>();
		for (Object key : parameterMap.keySet()) {
			String[] values = (String[]) parameterMap.get(key);
			for (String value : values) {
				convertedMap.add((String) key, value);
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
		HttpStatus httpStatus = null;
		if (httpStatusFromHeader instanceof HttpStatus) {
			httpStatus = (HttpStatus) httpStatusFromHeader;
		}
		else if (httpStatusFromHeader instanceof Integer) {
			httpStatus = HttpStatus.valueOf((Integer) httpStatusFromHeader);
		}
		else if (httpStatusFromHeader instanceof String) {
			httpStatus = HttpStatus.valueOf(Integer.parseInt((String) httpStatusFromHeader));
		}
		return httpStatus;
	}

	protected StandardEvaluationContext createEvaluationContext(){
		if (this.getBeanFactory() != null) {
			return ExpressionUtils.createStandardEvaluationContext(new BeanFactoryResolver(this.getBeanFactory()),
					this.getConversionService());
		}
		else {
			return ExpressionUtils.createStandardEvaluationContext(this.getConversionService());
		}
	}

	private void validateSupportedMethods() {
		if (this.requestPayloadType != null){
			for (HttpMethod httpMethod : this.supportedMethods) {
				if (HttpMethod.GET.equals(httpMethod) || HttpMethod.HEAD.equals(httpMethod) || HttpMethod.OPTIONS.equals(httpMethod)){
					if (logger.isWarnEnabled()){
						logger.warn("The 'requestPayloadType' attribute will have no relevance for one of the specified HTTP methods '" +
			               httpMethod + "'");
					}
				}
			}
		}
	}

	public int beforeShutdown() {
		this.shuttingDown = true;
		return this.activeCount.get();
	}

	public int afterShutdown() {
		return this.activeCount.get();
	}
}
