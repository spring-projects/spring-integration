/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.http.outbound;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Source;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.http.converter.SerializingHttpMessageConverter;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * A {@link MessageHandler} implementation that executes HTTP requests by delegating
 * to a {@link RestTemplate} instance. If the 'expectReply' flag is set to true (the default)
 * then a reply Message will be generated from the HTTP response. If that response contains
 * a body, it will be used as the reply Message's payload. Otherwise the reply Message's
 * payload will contain the response status as an instance of the {@link HttpStatus} enum.
 * When there is a response body, the {@link HttpStatus} enum instance will instead be
 * copied to the MessageHeaders of the reply. In both cases, the response headers will
 * be mapped to the reply Message's headers by this handler's {@link HeaderMapper} instance.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class HttpRequestExecutingMessageHandler extends AbstractReplyProducingMessageHandler {

	private final String uri;

	private volatile HttpMethod httpMethod = HttpMethod.POST;

	private volatile boolean expectReply = true;

	private volatile Class<?> expectedResponseType;

	private volatile boolean extractPayload = true;
	
	private volatile boolean extractPayloadExplicitlySet = false;

	private volatile String charset = "UTF-8";

	private volatile boolean transferCookies = false;

	private volatile HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.outboundMapper();

	private final Map<String, Expression> uriVariableExpressions = new HashMap<String, Expression>();

	private final RestTemplate restTemplate;

	private final StandardEvaluationContext evaluationContext;

	private final Pattern cookieNameExtractorPattern = Pattern.compile("([^=]+)");

	/**
	 * Create a handler that will send requests to the provided URI.
	 */
	public HttpRequestExecutingMessageHandler(URI uri) {
		this(uri.toString());
	}

	/**
	 * Create a handler that will send requests to the provided URI.
	 */
	public HttpRequestExecutingMessageHandler(String uri) {
		this(uri, null);
	}

	/**
	 * Create a handler that will send requests to the provided URI using a provided RestTemplate
	 * @param uri
	 * @param restTemplate
	 */
	public HttpRequestExecutingMessageHandler(String uri, RestTemplate restTemplate) {
		Assert.hasText(uri, "URI is required");
		this.restTemplate = (restTemplate == null ? new RestTemplate() : restTemplate);
		this.restTemplate.getMessageConverters().add(0, new SerializingHttpMessageConverter());
		this.uri = uri;
		StandardEvaluationContext sec = new StandardEvaluationContext();
		sec.addPropertyAccessor(new MapAccessor());
		this.evaluationContext = sec;
	}


	/**
	 * Specify the {@link HttpMethod} for requests. The default method will be POST.
	 */
	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	/**
	 * Specify whether the outbound message's payload should be extracted
	 * when preparing the request body. Otherwise the Message instance itself
	 * will be serialized. The default value is <code>true</code>.
	 */
	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
		this.extractPayloadExplicitlySet = true;
	}

	/**
	 * Specify the charset name to use for converting String-typed payloads to
	 * bytes. The default is 'UTF-8'.
	 */
	public void setCharset(String charset) {
		Assert.isTrue(Charset.isSupported(charset), "unsupported charset '" + charset + "'");
		this.charset = charset;
	}

	/**
	 * Specify whether a reply Message is expected. If not, this handler will simply return null for a
	 * successful response or throw an Exception for a non-successful response. The default is true.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	/**
	 * Specify the expected response type for the REST request.
	 * If this is null (the default), only the status code will be returned
	 * as the reply Message payload. To take advantage of the HttpMessageConverters
	 * registered on this adapter, provide a different type).
	 */
	public void setExpectedResponseType(Class<?> expectedResponseType) {
		this.expectedResponseType = expectedResponseType;
	}

	/**
	 * Set the {@link ResponseErrorHandler} for the underlying {@link RestTemplate}.
	 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
	 */
	public void setErrorHandler(ResponseErrorHandler errorHandler) {
		this.restTemplate.setErrorHandler(errorHandler);
	}

	/**
	 * Set a list of {@link HttpMessageConverter}s to be used by the underlying {@link RestTemplate}.
	 * Converters configured via this method will override the default converters.
	 * @see RestTemplate#setMessageConverters(java.util.List)
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.restTemplate.setMessageConverters(messageConverters);
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 */
	public void setHeaderMapper(HeaderMapper<HttpHeaders> headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} for the underlying {@link RestTemplate}.
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		this.restTemplate.setRequestFactory(requestFactory);
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

	/**
	 * Set to true if you wish 'Set-Cookie' headers in responses to be
	 * transferred as 'Cookie' headers in subsequent interactions for
	 * a message.
	 * @param transferCookies the transferCookies to set.
	 */
	public void setTransferCookies(boolean transferCookies) {
		this.transferCookies = transferCookies;
	}

	@Override
	public void onInit() {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		ConversionService conversionService = this.getConversionService();
		if (conversionService != null) {
			this.evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		if (!this.shouldIncludeRequestBody() && this.extractPayloadExplicitlySet){
			if (logger.isWarnEnabled()){
				logger.warn("The 'extractPayload' attribute has no meaning in the context of this handler since the provided HTTP Method is '" + 
			           this.httpMethod + "', and no request body will be sent for that method.");
			}
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		try {
			Map<String, Object> uriVariables = new HashMap<String, Object>();
			for (Map.Entry<String, Expression> entry : this.uriVariableExpressions.entrySet()) {
				Object value = entry.getValue().getValue(this.evaluationContext, requestMessage, String.class);
				uriVariables.put(entry.getKey(), value);
			}
			HttpEntity<?> httpRequest = this.generateHttpRequest(requestMessage);
			ResponseEntity<?> httpResponse = this.restTemplate.exchange(this.uri, this.httpMethod, httpRequest, this.expectedResponseType, uriVariables);
			if (this.expectReply) {
				HttpHeaders httpHeaders = httpResponse.getHeaders();
				if (this.transferCookies) {
					httpHeaders = this.doConvertSetCookie(httpHeaders);
				}
				Map<String, ?> headers = this.headerMapper.toHeaders(httpHeaders);
				if (httpResponse.hasBody()) {
					Object responseBody = httpResponse.getBody();
					MessageBuilder<?> replyBuilder = (responseBody instanceof Message<?>) ?
							MessageBuilder.fromMessage((Message<?>) responseBody) : MessageBuilder.withPayload(responseBody);
					replyBuilder.setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE, httpResponse.getStatusCode());
					return replyBuilder.copyHeaders(headers).build();
				}
				else {
					return MessageBuilder.withPayload(httpResponse.getStatusCode()).
							copyHeaders(headers).setHeader(org.springframework.integration.http.HttpHeaders.STATUS_CODE, httpResponse.getStatusCode()).
							build();
				}
			}
			return null;
		}
		catch (MessagingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessageHandlingException(requestMessage, "HTTP request execution failed for URI [" + this.uri + "]", e);
		}
	}

	private HttpHeaders doConvertSetCookie(HttpHeaders headers) {
		HttpHeaders modifiableHeaders = new HttpHeaders();
		for (Entry<String, List<String>> entry : headers.entrySet()) {
			String key = entry.getKey();
			if (key.equalsIgnoreCase("Set-Cookie")) {
				List<String> cookies = headers.get("Set-Cookie");
				modifiableHeaders.put(DefaultHttpHeaderMapper.TRANSFERRED_COOKIE, cookies);
				if (logger.isDebugEnabled()) {
					logger.debug("Converted Set-Cookie to "
							+ DefaultHttpHeaderMapper.TRANSFERRED_COOKIE + ": "
							+ cookies);
				}
			} else {
				modifiableHeaders.put(key, headers.get(key));
			}
		}
		return modifiableHeaders;
	}

	private HttpEntity<?> generateHttpRequest(Message<?> message) throws Exception {
		Assert.notNull(message, "message must not be null");
		return (this.extractPayload) ? this.createHttpEntityFromPayload(message)
				: this.createHttpEntityFromMessage(message);
	}

	private HttpEntity<?> createHttpEntityFromPayload(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof HttpEntity<?>) {
			// payload is already an HttpEntity, just return it as-is
			return (HttpEntity<?>) payload;
		}
		HttpHeaders httpHeaders = this.mapHeaders(message);
		if (!shouldIncludeRequestBody()) {
			return new HttpEntity<Object>(httpHeaders);
		}
		// otherwise, we are creating a request with a body and need to deal with the content-type header as well
		if (httpHeaders.getContentType() == null) {
			MediaType contentType = (payload instanceof String) ? this.resolveContentType((String) payload, this.charset)
					: this.resolveContentType(payload);
			httpHeaders.setContentType(contentType);
		}
		if (MediaType.APPLICATION_FORM_URLENCODED.equals(httpHeaders.getContentType()) ||
				MediaType.MULTIPART_FORM_DATA.equals(httpHeaders.getContentType())) {
			if (!(payload instanceof MultiValueMap)) {
				payload = this.convertToMultiValueMap((Map<?,?>) payload);
			}
		}
		return new HttpEntity<Object>(payload, httpHeaders);
	}

	private HttpEntity<?> createHttpEntityFromMessage(Message<?> message) {
		HttpHeaders httpHeaders = mapHeaders(message);
		if (shouldIncludeRequestBody()) {
			httpHeaders.setContentType(new MediaType("application", "x-java-serialized-object"));
			return new HttpEntity<Object>(message, httpHeaders);
		}
		return new HttpEntity<Object>(httpHeaders);
	}

	protected HttpHeaders mapHeaders(Message<?> message) {
		HttpHeaders httpHeaders = new HttpHeaders();
		this.headerMapper.fromHeaders(message.getHeaders(), httpHeaders);
		if (this.transferCookies) {
			doTransferCookies(httpHeaders);
		}
		return httpHeaders;
	}

	private void doTransferCookies(HttpHeaders httpHeaders) {
		List<String> transferredCookies = httpHeaders.remove(DefaultHttpHeaderMapper.TRANSFERRED_COOKIE);
		if (transferredCookies != null) {
			List<String> currentCookies = httpHeaders.get("Cookie");
			if (currentCookies == null) {
				currentCookies = new ArrayList<String>();
			}
			Map<String, String> currentCookiesAsMap = this.cookiesToMap(currentCookies);
			Map<String, String> transferredCookiesAsMap = this.cookiesToMap(transferredCookies);
			for (Entry<String, String> transferredCookie : transferredCookiesAsMap.entrySet()) {
				String name = transferredCookie.getKey();
				currentCookiesAsMap.put(name, transferredCookie.getValue());
			}
			List<String> mergedCookies = new ArrayList<String>(currentCookiesAsMap.values());
			httpHeaders.put("Cookie", mergedCookies);
			if (logger.isDebugEnabled()) {
				logger.debug("Transferred Cookies: " + transferredCookies);
			}
		}
	}

	private Map<String, String> cookiesToMap(List<String> cookies) {
		Map<String, String> map = new HashMap<String, String>();
		for (String cookie : cookies) {
			Matcher matcher = this.cookieNameExtractorPattern.matcher(cookie);
			String key = cookie;
			if (matcher.find()) {
				key = matcher.group(1).toLowerCase();
			}
			map.put(key, cookie);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private MediaType resolveContentType(Object content) {
		MediaType contentType = null;
		if (content instanceof byte[]) {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		else if (content instanceof Source) {
			contentType = MediaType.TEXT_XML;
		}
		else if (content instanceof Map) {
			// We need to check separately for MULTIPART as well as URLENCODED simply because
			// MultiValueMap<Object, Object> is actually valid content for serialization
			if (this.isFormData((Map<Object, ?>) content)) {
				if (this.isMultipart((Map<String, ?>)content)) {
					contentType = MediaType.MULTIPART_FORM_DATA;
				} 
				else {
					contentType = MediaType.APPLICATION_FORM_URLENCODED;
				}
			}
		}
		if (contentType == null) {
			contentType = new MediaType("application", "x-java-serialized-object");
		}
		return contentType;
	}

	private boolean shouldIncludeRequestBody() {
		return !HttpMethod.GET.equals(this.httpMethod);
	}

	private MediaType resolveContentType(String content, String charset) {
		return new MediaType("text", "plain", Charset.forName(charset));
	}

	private MultiValueMap<Object, Object> convertToMultiValueMap(Map<?, ?> simpleMap) {
		LinkedMultiValueMap<Object, Object> multipartValueMap = new LinkedMultiValueMap<Object, Object>();
		for (Object key : simpleMap.keySet()) {
			Object value = simpleMap.get(key);
			if (value instanceof Object[]) {
				Object[] valueArray = (Object[]) value;
				value = Arrays.asList(valueArray);	
			}
			if (value instanceof Collection) {
				multipartValueMap.put(key, new ArrayList<Object>((Collection<?>) value));
			}
			else {
				multipartValueMap.add(key, value);
			}
		}
		return multipartValueMap;
	}

	/**
	 * If all keys are Strings, and some values are not Strings we'll consider 
	 * the Map to be multipart/form-data
	 */
	private boolean isMultipart(Map<String, ?> map) {
		for (String key : map.keySet()) {
			Object value = map.get(key);
			if (value != null) {
				if (value.getClass().isArray()) {
					value = CollectionUtils.arrayToList(value);	
				}
				if (value instanceof Collection) {
					Collection<?> cValues = (Collection<?>) value;
					for (Object cValue : cValues) {
						if (cValue != null && !(cValue instanceof String)) {
							return true;
						}
					}
				} 
				else if (!(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * If all keys and values are Strings, we'll consider the Map to be form data.
	 */
	private boolean isFormData(Map<Object, ?> map) {
		for (Object	 key : map.keySet()) {
			if (!(key instanceof String)) {
				return false;
			} 	
		}
		return true;
	}

}
