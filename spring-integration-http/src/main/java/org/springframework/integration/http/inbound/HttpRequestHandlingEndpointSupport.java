/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.ResolvableType;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.converter.MultipartAwareFormHttpMessageConverter;
import org.springframework.integration.http.multipart.MultipartHttpInputMessage;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.json.JacksonPresent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
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
 * reference to a {@code org.springframework.integration.mapping.HeaderMapper<HttpHeaders>} implementation
 * to the {@link #setHeaderMapper(org.springframework.integration.mapping.HeaderMapper)} method.
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
 * {@code extractReplyPayload} value to {@code false}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Biju Kunjummen
 *
 * @since 2.0
 */
public abstract class HttpRequestHandlingEndpointSupport extends BaseHttpInboundEndpoint {

	private final List<HttpMessageConverter<?>> defaultMessageConverters = new ArrayList<>();

	private List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

	private boolean convertersMerged;

	private boolean mergeWithDefaultConverters = false;

	private MultipartResolver multipartResolver;

	/**
	 * Construct a gateway that will wait for the {@link #setReplyTimeout(long)
	 * replyTimeout} for a reply; if the timeout is exceeded a '500 Internal Server Error'
	 * status code is returned. This can be modified using the
	 * {@link #setStatusCodeExpression statusCodeExpression}.
	 * @see #setReplyTimeout(long)
	 * @see #setStatusCodeExpression
	 */
	public HttpRequestHandlingEndpointSupport() {
		this(true);
	}

	/**
	 * Construct a gateway. If 'expectReply' is true it will wait for the
	 * {@link #setReplyTimeout(long) replyTimeout} for a reply; if the timeout is exceeded
	 * a '500 Internal Server Error' status code is returned. This can be modified using
	 * the {@link #setStatusCodeExpression statusCodeExpression}.
	 * If 'false', a 200 OK status will be returned; this can also be modified using
	 * {@link #setStatusCodeExpression statusCodeExpression}.
	 * @param expectReply true if a reply is expected from the downstream flow.
	 * @see #setReplyTimeout(long)
	 * @see #setStatusCodeExpression
	 */
	public HttpRequestHandlingEndpointSupport(boolean expectReply) {
		super(expectReply);
		this.defaultMessageConverters.add(new MultipartAwareFormHttpMessageConverter());
		this.defaultMessageConverters.add(new ByteArrayHttpMessageConverter());
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);
		this.defaultMessageConverters.add(stringHttpMessageConverter);
		this.defaultMessageConverters.add(new ResourceHttpMessageConverter());
		SourceHttpMessageConverter<Source> sourceConverter = new SourceHttpMessageConverter<>();
		this.defaultMessageConverters.add(sourceConverter);
		if (JAXB_PRESENT) {
			this.defaultMessageConverters.add(new Jaxb2RootElementHttpMessageConverter());
			logger.debug("'Jaxb2RootElementHttpMessageConverter' was added to the 'defaultMessageConverters'.");
		}
		if (JacksonPresent.isJackson2Present()) {
			this.defaultMessageConverters.add(new MappingJackson2HttpMessageConverter());
			logger.debug("'MappingJackson2HttpMessageConverter' was added to the 'defaultMessageConverters'.");
		}
		if (ROME_TOOLS_PRESENT) {
			this.defaultMessageConverters.add(new AtomFeedHttpMessageConverter());
			this.defaultMessageConverters.add(new RssChannelHttpMessageConverter());
			logger.debug("'AtomFeedHttpMessageConverter' was added to the 'defaultMessageConverters'.");
			logger.debug("'RssChannelHttpMessageConverter' was added to the 'defaultMessageConverters'.");
		}
	}

	/**
	 * Set the message body converters to use. These converters are used to convert from and to HTTP requests and
	 * responses.
	 * @param messageConverters The message converters.
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "'messageConverters' must not be null");
		Assert.noNullElements(messageConverters.toArray(), "'messageConverters' must not contain null entries");
		List<HttpMessageConverter<?>> localConverters = new ArrayList<>(messageConverters);
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
	 * Specify the {@link MultipartResolver} to use when checking requests. If no resolver is provided, the
	 * "multipartResolver" bean in the context will be used as a fallback. If that is not available either, this
	 * endpoint will not support multipart requests.
	 * @param multipartResolver The multipart resolver.
	 */
	public void setMultipartResolver(MultipartResolver multipartResolver) {
		this.multipartResolver = multipartResolver;
	}

	/**
	 * Locates the {@link MultipartResolver} bean based on the default name defined by the
	 * {@link DispatcherServlet#MULTIPART_RESOLVER_BEAN_NAME} constant if available.
	 * Sets up default converters if no converters set, or {@link #setMergeWithDefaultConverters(boolean)}
	 * was called with true after the converters were set.
	 */
	@Override
	protected void onInit() {
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		if (this.multipartResolver == null && beanFactory != null) {
			try {
				MultipartResolver resolver = beanFactory.getBean(
						DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
				if (logger.isDebugEnabled()) {
					logger.debug("Using MultipartResolver [" + resolver + "]");
				}
				this.multipartResolver = resolver;
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
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel. If this gateway's
	 * 'expectReply' property is true, it will also generate a response from the reply Message once received.
	 * @param servletRequest The servlet request.
	 * @param httpEntity the request entity to use.
	 * @return The response Message.
	 */
	protected final Message<?> doHandleRequest(HttpServletRequest servletRequest, RequestEntity<?> httpEntity) {
		if (isRunning()) {
			return actualDoHandleRequest(servletRequest, httpEntity);
		}
		else {
			return createServiceUnavailableResponse();
		}
	}

	private Message<?> actualDoHandleRequest(HttpServletRequest servletRequest, RequestEntity<?> httpEntity) {
		this.activeCount.incrementAndGet();
		try {
			MultiValueMap<String, String> requestParams = convertParameterMap(servletRequest.getParameterMap());

			StandardEvaluationContext evaluationContext =
					prepareEvaluationContext(servletRequest, httpEntity, requestParams);

			Map<String, Object> headers = getHeaderMapper().toHeaders(httpEntity.getHeaders());
			Object payload = null;
			Message<?> message = null;
			boolean expectReply = isExpectReply();
			try {
				if (getPayloadExpression() != null) {
					// create payload based on SpEL
					payload = getPayloadExpression().getValue(evaluationContext);
				}

				if (!CollectionUtils.isEmpty(getHeaderExpressions())) {
					headers.putAll(
							ExpressionEvalMap.from(getHeaderExpressions())
									.usingEvaluationContext(evaluationContext)
									.withRoot(httpEntity)
									.build());
				}

				if (payload == null) {
					if (httpEntity.getBody() != null) {
						payload = httpEntity.getBody();
					}
					else {
						payload = requestParams;
					}
				}

				message = prepareRequestMessage(servletRequest, httpEntity, headers, payload);
			}
			catch (Exception ex) {
				MessageConversionException conversionException =
						new MessageConversionException("Cannot create request message", ex);
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					ErrorMessage errorMessage = buildErrorMessage(null, conversionException);
					if (expectReply) {
						return this.messagingTemplate.sendAndReceive(errorChannel, errorMessage);
					}
					else {
						this.messagingTemplate.send(errorChannel, errorMessage);
						return null;
					}
				}
				else {
					throw conversionException;
				}
			}

			Message<?> reply = null;

			if (expectReply) {
				try {
					reply = sendAndReceiveMessage(message);
				}
				catch (MessageTimeoutException e) {
					reply =
							getMessageBuilderFactory()
									.withPayload(e.getMessage())
									.setHeader(HttpHeaders.STATUS_CODE, evaluateHttpStatus(httpEntity))
									.build();
				}
			}
			else {
				send(message);
			}
			return reply;
		}
		finally {
			postProcessRequest(servletRequest);
			this.activeCount.decrementAndGet();
		}
	}

	private Message<?> prepareRequestMessage(HttpServletRequest servletRequest, RequestEntity<?> httpEntity,
			Map<String, Object> headers, Object payload) {

		AbstractIntegrationMessageBuilder<?> messageBuilder;

		if (getValidator() != null) {
			validate(payload);
		}

		if (payload instanceof Message<?>) {
			messageBuilder =
					getMessageBuilderFactory()
							.fromMessage((Message<?>) payload)
							.copyHeadersIfAbsent(headers);
		}
		else {
			Assert.state(payload != null, "payload cannot be null");
			messageBuilder = getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers);
		}

		HttpMethod method = httpEntity.getMethod();
		if (method != null) {
			messageBuilder.setHeader(HttpHeaders.REQUEST_METHOD, method.toString());
		}

		return messageBuilder
				.setHeader(HttpHeaders.REQUEST_URL, httpEntity.getUrl().toString())
				.setHeader(HttpHeaders.USER_PRINCIPAL, servletRequest.getUserPrincipal())
				.build();
	}

	private StandardEvaluationContext prepareEvaluationContext(HttpServletRequest servletRequest,
			RequestEntity<?> httpEntity, MultiValueMap<String, String> requestParams) {

		StandardEvaluationContext evaluationContext = createEvaluationContext();
		evaluationContext.setRootObject(httpEntity);

		evaluationContext.setVariable("requestAttributes", RequestContextHolder.currentRequestAttributes());

		evaluationContext.setVariable("requestParams", requestParams);
		evaluationContext.setVariable("requestHeaders", new ServletServerHttpRequest(servletRequest).getHeaders());

		Cookie[] requestCookies = servletRequest.getCookies();
		if (!ObjectUtils.isEmpty(requestCookies)) {
			Map<String, Cookie> cookies =
					Arrays.stream(requestCookies)
							.collect(Collectors.toMap(Cookie::getName, Function.identity()));
			evaluationContext.setVariable("cookies", cookies);
		}

		Map<?, ?> pathVariables =
				(Map<?, ?>) servletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (!CollectionUtils.isEmpty(pathVariables)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Mapped path variables: " + pathVariables);
			}
			evaluationContext.setVariable("pathVariables", pathVariables);
		}

		Map<?, ?> matrixVariables = (Map<?, ?>) servletRequest.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE);

		if (!CollectionUtils.isEmpty(matrixVariables)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Mapped matrix variables: " + matrixVariables);
			}
			evaluationContext.setVariable("matrixVariables", matrixVariables);
		}
		return evaluationContext;
	}

	private Message<?> createServiceUnavailableResponse() {
		if (logger.isDebugEnabled()) {
			logger.debug("Endpoint is stopped; returning status " + HttpStatus.SERVICE_UNAVAILABLE);
		}
		return getMessageBuilderFactory()
				.withPayload("Endpoint is stopped")
				.setHeader(HttpHeaders.STATUS_CODE, HttpStatus.SERVICE_UNAVAILABLE)
				.build();
	}

	/**
	 * Converts the reply message to the appropriate HTTP reply object and
	 * sets up the {@link ServletServerHttpResponse}.
	 * @param response     The ServletServerHttpResponse.
	 * @param replyMessage The reply message.
	 * @return The message payload (if {@code extractReplyPayload}) otherwise the message.
	 */
	protected final Object setupResponseAndConvertReply(ServletServerHttpResponse response, Message<?> replyMessage) {
		MessageHeaders replyMessageHeaders = replyMessage.getHeaders();
		getHeaderMapper().fromHeaders(replyMessageHeaders, response.getHeaders());
		HttpStatus httpStatus = resolveHttpStatusFromHeaders(replyMessageHeaders);
		if (httpStatus != null) {
			response.setStatusCode(httpStatus);
		}

		Object reply = replyMessage;
		if (getExtractReplyPayload()) {
			reply = replyMessage.getPayload();
		}
		return reply;

	}

	protected void setStatusCodeIfNeeded(ServerHttpResponse response, HttpEntity<?> httpEntity) {
		if (getStatusCodeExpression() != null) {
			HttpStatus httpStatus = evaluateHttpStatus(httpEntity);
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
	protected ServletServerHttpRequest prepareRequest(HttpServletRequest servletRequest) {
		if (servletRequest instanceof MultipartHttpServletRequest) {
			return new MultipartHttpInputMessage((MultipartHttpServletRequest) servletRequest);
		}
		if (this.multipartResolver != null && this.multipartResolver.isMultipart(servletRequest)) {
			return new MultipartHttpInputMessage(this.multipartResolver.resolveMultipart(servletRequest));
		}
		return new ServletServerHttpRequest(servletRequest);
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
		MultiValueMap<String, String> convertedMap = new LinkedMultiValueMap<>(parameterMap.size());
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String[] values = entry.getValue();
			for (String value : values) {
				convertedMap.add(entry.getKey(), value);
			}
		}
		return convertedMap;
	}

	protected RequestEntity<Object> prepareRequestEntity(ServletServerHttpRequest request) throws IOException {
		Object requestBody = null;
		if (isReadable(request.getMethod())) {
			requestBody = extractRequestBody(request);
		}

		return new RequestEntity<>(requestBody, request.getHeaders(), request.getMethod(), request.getURI());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Object extractRequestBody(ServletServerHttpRequest request) throws IOException {
		MediaType contentType = request.getHeaders().getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		ResolvableType requestPayloadType = getRequestPayloadType();
		if (requestPayloadType == null) {
			requestPayloadType =
					ResolvableType.forClass(
							"text".equals(contentType.getType())
									? String.class
									: byte[].class);
		}

		Type targetType = requestPayloadType.getType();
		Class<?> targetClass = requestPayloadType.toClass();

		for (HttpMessageConverter<?> converter : this.messageConverters) {
			GenericHttpMessageConverter<?> genericConverter =
					converter instanceof GenericHttpMessageConverter
							? (GenericHttpMessageConverter<?>) converter
							: null;
			if (genericConverter != null
					? genericConverter.canRead(targetType, null, contentType)
					: (converter.canRead(targetClass, contentType))) {

				if (genericConverter != null) {
					return genericConverter.read(targetType, null, request);
				}
				else {
					return converter.read((Class) targetClass, request);
				}
			}
		}
		throw new MessagingException(
				"Could not convert request: no suitable HttpMessageConverter found for expected type ["
						+ requestPayloadType + "] and content type [" + contentType + "]");
	}

}
