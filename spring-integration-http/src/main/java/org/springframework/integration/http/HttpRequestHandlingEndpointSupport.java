/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.gateway.AbstractMessagingGateway;
import org.springframework.integration.message.HeaderMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Base class for HTTP request handling endpoints.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
abstract class HttpRequestHandlingEndpointSupport extends AbstractMessagingGateway {

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", HttpRequestHandlingEndpointSupport.class.getClassLoader()) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", HttpRequestHandlingEndpointSupport.class.getClassLoader());

	private static boolean romePresent =
			ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", HttpRequestHandlingEndpointSupport.class.getClassLoader());


	private volatile Class<?> conversionTargetType = byte[].class;

	private volatile List<HttpMethod> supportedMethods = Arrays.asList(HttpMethod.GET, HttpMethod.POST);

	private volatile List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private volatile HeaderMapper<HttpHeaders> headerMapper = new DefaultHttpHeaderMapper();

	private final boolean expectReply;

	private volatile MultipartResolver multipartResolver;


	public HttpRequestHandlingEndpointSupport() {
		this(true);
	}

	@SuppressWarnings("unchecked")
	public HttpRequestHandlingEndpointSupport(boolean expectReply) {
		this.expectReply = expectReply;
		this.messageConverters.add(new MultipartAwareFormHttpMessageConverter());
		this.messageConverters.add(new SerializingHttpMessageConverter());
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
			// TODO add deps for:
			//this.messageConverters.add(new AtomFeedHttpMessageConverter());
			//this.messageConverters.add(new RssChannelHttpMessageConverter());
		}
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

	protected HeaderMapper<HttpHeaders> getHeaderMapper() {
		return this.headerMapper;
	}

	/**
	 * Specify the supported request methods for this gateway.
	 * By default, only GET and POST are supported.
	 */
	public void setSupportedMethods(HttpMethod... supportedMethods) {
		Assert.notEmpty(supportedMethods, "at least one supported method is required");
		this.supportedMethods = Arrays.asList(supportedMethods);
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request content
	 * is read by the {@link HttpMessageConverter}s. The default is <code>byte[].class</code>.
	 */
	public void setConversionTargetType(Class<?> conversionTargetType) {
		Assert.notNull(conversionTargetType, "conversionTargetType must not be null");
		this.conversionTargetType = conversionTargetType;
	}

	/**
	 * Specify the {@link MultipartResolver} to use when checking requests.
	 * If no resolver is provided, this mapper will not support multipart
	 * requests.
	 */
	public void setMultipartResolver(MultipartResolver multipartResolver) {
		this.multipartResolver = multipartResolver;
	}

	@Override
	public String getComponentType() {
		return (this.expectReply) ? "http:inbound-gateway" : "http:inbound-channel-adapter"; 
	}

	/**
	 * Locates the {@link MultipartResolver} bean based on the default name defined by
	 * the {@link DispatcherServlet#MULTIPART_RESOLVER_BEAN_NAME} constant if available.
	 */
	@Override
	protected void onInit() throws Exception {
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (this.multipartResolver == null && beanFactory != null) {
			try {
				MultipartResolver multipartResolver =
						this.getBeanFactory().getBean(DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
				if (logger.isDebugEnabled()) {
					logger.debug("Using MultipartResolver [" + multipartResolver + "]");
				}
				this.multipartResolver = multipartResolver;
			}
			catch (NoSuchBeanDefinitionException e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Unable to locate MultipartResolver with name '" + DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME +
							"': no multipart request handling will be supported.");
				}
			}
		}
	}

	/**
	 * Handles the HTTP request by generating a Message and sending it to the request channel.
	 * If this gateway's 'expectReply' property is true, it will also generate a response from
	 * the reply Message once received.
	 */
	protected final Object handleRequest(HttpServletRequest servletRequest) throws IOException {
		ServletServerHttpRequest request = this.prepareRequest(servletRequest);
		Assert.isTrue(this.supportedMethods.contains(request.getMethod()),
				"unsupported request method [" + request.getMethod() + "]");
		Object payload = null;
		if (this.isReadable(request)) {
			payload = this.generatePayloadFromRequestBody(request);
		}
		else {
			payload = this.convertParameterMap(servletRequest.getParameterMap());
		}
		Map<String, ?> headers = this.headerMapper.toHeaders(request.getHeaders());
		Message<?> message = MessageBuilder.withPayload(payload)
				.copyHeaders(headers)
				//.setHeader(HttpHeaders.REQUEST_URL, request.getURI().toString())
				//.setHeader(HttpHeaders.REQUEST_METHOD, request.getMethod().toString())
				//.setHeader(HttpHeaders.USER_PRINCIPAL, servletRequest.getUserPrincipal())
				.build();
		Object result = null;
		if (this.expectReply) {
			result = this.sendAndReceiveMessage(message); 
		}
		else {
			this.send(message);
		}
		this.postProcessRequest(servletRequest);
		return result;
	}

	/**
	 * Prepares an instance of {@link ServletServerHttpRequest} from the raw {@link HttpServletRequest}.
	 * Also converts the request into a multipart request to make multiparts available if necessary.
	 * If no multipart resolver is set, simply returns the existing request.
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
	 * Checks if the request has a readable body (not a GET, HEAD, or OPTIONS request)
	 * and a Content-Type header.
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
	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
	private Object generatePayloadFromRequestBody(ServletServerHttpRequest request) throws IOException {
		MediaType contentType = request.getHeaders().getContentType();
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter.canRead(this.conversionTargetType, contentType)) {
				return converter.read((Class) this.conversionTargetType, request);
			}
		}
		throw new MessagingException("Could not convert request: no suitable HttpMessageConverter found for expected type [" +
				this.conversionTargetType.getName() + "] and content type [" + contentType + "]");
	}

}
