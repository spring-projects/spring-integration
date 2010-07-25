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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.gateway.AbstractMessagingGateway;
import org.springframework.integration.message.HeaderMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Inbound Messaging Gateway that handles HTTP Requests. May be configured as a bean in the
 * Application Context and delegated to from a simple HttpRequestHandlerServlet in
 * <code>web.xml</code> where the servlet and bean both have the same name. If the
 * {@link #expectReply} property is set to true, a response can generated from a
 * reply Message. Otherwise, the gateway will play the role of a unidirectional
 * Channel Adapter with a simple status-based response (e.g. 200 OK).
 * <p/>
 * The default supported request methods are GET and POST, but the list of values can
 * be configured with the {@link #supportedMethods} property. The payload generated from
 * a GET request (or HEAD or OPTIONS if supported) will be a {@link MultiValueMap} 
 * containing the parameter values. For a request containing a body (e.g. a POST),
 * the type of the payload is determined by the {@link #conversionTargetType} property.
 * <p/>
 * If the HTTP request is a multipart, a {@link MultiValueMap} payload will be generated. If
 * this gateway's {@link #uploadMultipartFiles} property is set to true, any files included
 * in that multipart request will be copied to the temporary directory. The corresponding
 * values for those files within the payload map will be {@link java.io.File} instances.
 * <p/>
 * By default a number of {@link HttpMessageConverter}s are already configured. The list
 * can be overridden by calling the {@link #setMessageConverters(List)} method.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpRequestHandlingMessagingGateway extends AbstractMessagingGateway implements HttpRequestHandler {

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder", HttpRequestHandlingMessagingGateway.class.getClassLoader());

	private static final boolean jacksonPresent =
			ClassUtils.isPresent("org.codehaus.jackson.map.ObjectMapper", HttpRequestHandlingMessagingGateway.class.getClassLoader()) &&
					ClassUtils.isPresent("org.codehaus.jackson.JsonGenerator", HttpRequestHandlingMessagingGateway.class.getClassLoader());

	private static boolean romePresent =
			ClassUtils.isPresent("com.sun.syndication.feed.WireFeed", HttpRequestHandlingMessagingGateway.class.getClassLoader());


	private volatile Class<?> conversionTargetType = byte[].class;

	private volatile List<HttpMethod> supportedMethods = Arrays.asList(HttpMethod.GET, HttpMethod.POST);

	private volatile List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

	private volatile HeaderMapper<HttpHeaders> headerMapper = new DefaultHeaderMapper();

	private final boolean expectReply;

	private volatile MultipartResolver multipartResolver;


	public HttpRequestHandlingMessagingGateway() {
		this(true);
	}

	@SuppressWarnings("unchecked")
	public HttpRequestHandlingMessagingGateway(boolean expectReply) {
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
	public final void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
		ServletServerHttpRequest request = this.prepareRequest(servletRequest);
		ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
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
		if (this.expectReply) {
			Message<?> reply = this.sendAndReceiveMessage(message);
			this.headerMapper.fromHeaders(reply.getHeaders(), response.getHeaders());
			if (reply.getPayload() != null) {
				this.writePayload(reply.getPayload(), response, request.getHeaders().getAccept());
			}
		}
		else {
			this.send(message);
			// will be a status response for now... add an optional ResponseGenerator strategy?
		}
		this.postProcessRequest(servletRequest);
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

	@SuppressWarnings("unchecked")
	private void writePayload(Object payload, ServletServerHttpResponse response, List<MediaType> acceptTypes) throws IOException {
		for (HttpMessageConverter converter : this.messageConverters) {
			for (MediaType acceptType : acceptTypes) {
				if (converter.canWrite(payload.getClass(), acceptType)) {
					converter.write(payload, acceptType, response);
					return;
				}
			}
		}
		throw new MessagingException("Could not convert reply: no suitable HttpMessageConverter found for result type [" +
				payload.getClass().getName() + "] and content types [" + acceptTypes + "]");
	}


	private static class DefaultHeaderMapper implements HeaderMapper<HttpHeaders> {

		public void fromHeaders(MessageHeaders headers, HttpHeaders target) {
			for (String name : headers.keySet()) {
				Object value = headers.get(name);
				if (value instanceof String) {
					target.add(name, (String) value);
				}
			}
		}

		public Map<String, ?> toHeaders(HttpHeaders source) {
			return source;
		}
	}

}
