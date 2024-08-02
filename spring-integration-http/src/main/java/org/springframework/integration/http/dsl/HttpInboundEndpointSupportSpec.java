/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.integration.http.dsl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.http.inbound.BaseHttpInboundEndpoint;
import org.springframework.integration.http.inbound.CrossOrigin;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @param <S> the target spec extension type.
 * @param <E> the target endpoint type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class HttpInboundEndpointSupportSpec<S extends HttpInboundEndpointSupportSpec<S, E>,
		E extends BaseHttpInboundEndpoint>
		extends MessagingGatewaySpec<S, E>
		implements ComponentsRegistration {

	protected final RequestMapping requestMapping = new RequestMapping(); // NOSONAR final

	protected final Map<String, Expression> headerExpressions = new HashMap<>(); // NOSONAR final

	protected final HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.inboundMapper(); // NOSONAR final

	@Nullable
	private HeaderMapper<HttpHeaders> explicitHeaderMapper;

	protected HttpInboundEndpointSupportSpec(E gateway, String... path) {
		super(gateway);
		this.requestMapping.setPathPatterns(path);
		this.target.setRequestMapping(this.requestMapping);
		this.target.setHeaderExpressions(this.headerExpressions);
		this.target.setHeaderMapper(this.headerMapper);
	}

	/**
	 * Provide a {@link Consumer} for configuring {@link RequestMapping} via {@link RequestMappingSpec}.
	 * @param mapping the {@link Consumer} to configure {@link RequestMappingSpec}.
	 * @return the spec
	 * @see RequestMapping
	 */
	public S requestMapping(Consumer<RequestMappingSpec> mapping) {
		mapping.accept(new RequestMappingSpec(this.requestMapping));
		return _this();
	}

	/**
	 * Provide a {@link Consumer} for configuring {@link CrossOrigin} via {@link CrossOriginSpec}.
	 * @param crossOrigin the {@link Consumer} to configure {@link CrossOriginSpec}.
	 * @return the spec
	 * @see CrossOrigin
	 */
	public S crossOrigin(Consumer<CrossOriginSpec> crossOrigin) {
		CrossOriginSpec originSpec = new CrossOriginSpec();
		crossOrigin.accept(originSpec);
		this.target.setCrossOrigin(originSpec.crossOrigin);
		return _this();
	}

	/**
	 * Specify a SpEL expression to evaluate in order to generate the Message payload.
	 * @param payloadExpression The payload expression.
	 * @return the spec
	 * @see BaseHttpInboundEndpoint#setPayloadExpression(Expression)
	 */
	public S payloadExpression(String payloadExpression) {
		return payloadExpression(PARSER.parseExpression(payloadExpression));
	}

	/**
	 * Specify a SpEL expression to evaluate in order to generate the Message payload.
	 * @param payloadExpression The payload expression.
	 * @return the spec
	 * @see BaseHttpInboundEndpoint#setPayloadExpression(Expression)
	 */
	public S payloadExpression(Expression payloadExpression) {
		this.target.setPayloadExpression(payloadExpression);
		return _this();
	}

	/**
	 * Specify a {@link Function} to evaluate in order to generate the Message payload.
	 * @param payloadFunction The payload {@link Function}.
	 * @param <P> the expected HTTP request body type.
	 * @return the spec
	 * @see BaseHttpInboundEndpoint#setPayloadExpression(Expression)
	 */
	public <P> S payloadFunction(Function<HttpEntity<P>, ?> payloadFunction) {
		return payloadExpression(new FunctionExpression<>(payloadFunction));
	}

	/**
	 * Specify a Map of SpEL expressions to evaluate in order to generate the Message headers.
	 * @param expressions The {@link Map} of SpEL expressions for headers.
	 * @return the spec
	 * @see BaseHttpInboundEndpoint#setHeaderExpressions(Map)
	 */
	public S headerExpressions(Map<String, Expression> expressions) {
		Assert.notNull(expressions, "'headerExpressions' must not be null");
		this.headerExpressions.clear();
		this.headerExpressions.putAll(expressions);
		return _this();
	}

	/**
	 * Specify SpEL expression for provided header to populate.
	 * @param header the header name to populate.
	 * @param expression the SpEL expression for the header.
	 * @return the spec
	 * @see BaseHttpInboundEndpoint#setHeaderExpressions(Map)
	 */
	public S headerExpression(String header, String expression) {
		return headerExpression(header, PARSER.parseExpression(expression));
	}

	/**
	 * Specify SpEL expression for provided header to populate.
	 * @param header the header name to populate.
	 * @param expression the SpEL expression for the header.
	 * @return the spec
	 * @see BaseHttpInboundEndpoint#setHeaderExpressions(Map)
	 */
	public S headerExpression(String header, Expression expression) {
		this.headerExpressions.put(header, expression);
		return _this();
	}

	/**
	 * Specify a {@link Function} for provided header to populate.
	 * @param header the header name to add.
	 * @param headerFunction the function to evaluate the header value against {@link HttpEntity}.
	 * @param <P> the expected HTTP body type.
	 * @return the current Spec.
	 * @see BaseHttpInboundEndpoint#setHeaderExpressions(Map)
	 */
	public <P> S headerFunction(String header, Function<HttpEntity<P>, ?> headerFunction) {
		return headerExpression(header, new FunctionExpression<>(headerFunction));
	}

	/**
	 * Set the {@link HeaderMapper} to use when mapping between HTTP headers and MessageHeaders.
	 * @param mapper The header mapper.
	 * @return the current Spec.
	 */
	public S headerMapper(HeaderMapper<HttpHeaders> mapper) {
		this.target.setHeaderMapper(mapper);
		this.explicitHeaderMapper = mapper;
		return _this();
	}

	/**
	 * Provide the pattern array for request headers to map.
	 * @param patterns the patterns for request headers to map.
	 * @return the current Spec.
	 * @see DefaultHttpHeaderMapper#setOutboundHeaderNames(String[])
	 */
	public S mappedRequestHeaders(String... patterns) {
		Assert.isNull(this.explicitHeaderMapper,
				() -> "The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': "
						+ this.explicitHeaderMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setInboundHeaderNames(patterns);
		return _this();
	}

	/**
	 * Provide the pattern array for response headers to map.
	 * @param patterns the patterns for response headers to map.
	 * @return the current Spec.
	 * @see DefaultHttpHeaderMapper#setInboundHeaderNames(String[])
	 */
	public S mappedResponseHeaders(String... patterns) {
		Assert.isNull(this.explicitHeaderMapper,
				() -> "The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': "
						+ this.explicitHeaderMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setOutboundHeaderNames(patterns);
		return _this();
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request content is read by the
	 * {@link org.springframework.http.converter.HttpMessageConverter}s.
	 * By default, this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to {@code byte[].class}.
	 * @param requestPayloadType The payload type.
	 * @return the current Spec.
	 */
	public S requestPayloadType(Class<?> requestPayloadType) {
		this.target.setRequestPayloadTypeClass(requestPayloadType);
		return _this();
	}

	/**
	 * Specify the type of payload to be generated when the inbound HTTP request content is read by the
	 * {@link org.springframework.http.converter.HttpMessageConverter}s.
	 * By default, this value is null which means at runtime any "text" Content-Type will
	 * result in String while all others default to {@code byte[].class}.
	 * @param requestPayloadType The payload type.
	 * @return the current Spec.
	 */
	public S requestPayloadType(ResolvableType requestPayloadType) {
		this.target.setRequestPayloadType(requestPayloadType);
		return _this();
	}

	/**
	 * Specify whether only the reply Message's payload should be passed in the response.
	 * If this is set to {@code false}, the entire Message will be used to generate the response.
	 * The default is {@code true}.
	 * @param extractReplyPayload true to extract the reply payload.
	 * @return the current Spec.
	 */
	public S extractReplyPayload(boolean extractReplyPayload) {
		this.target.setExtractReplyPayload(extractReplyPayload);
		return _this();
	}

	/**
	 * Specify the {@link Expression} to resolve a status code for Response to override
	 * the default '200 OK' or '500 Internal Server Error' for a timeout.
	 * @param statusCodeExpression The status code Expression.
	 * @return the current Spec.
	 * @see BaseHttpInboundEndpoint#setStatusCodeExpression(Expression)
	 */
	public S statusCodeExpression(String statusCodeExpression) {
		this.target.setStatusCodeExpressionString(statusCodeExpression);
		return _this();
	}

	/**
	 * Specify the {@link Expression} to resolve a status code for Response to override
	 * the default '200 OK' or '500 Internal Server Error' for a timeout.
	 * @param statusCodeExpression The status code Expression.
	 * @return the current Spec.
	 * @see BaseHttpInboundEndpoint#setStatusCodeExpression(Expression)
	 */
	public S statusCodeExpression(Expression statusCodeExpression) {
		this.target.setStatusCodeExpression(statusCodeExpression);
		return _this();
	}

	/**
	 * Specify the {@link Function} to resolve a status code for Response to override
	 * the default '200 OK' or '500 Internal Server Error' for a timeout.
	 * @param statusCodeFunction The status code {@link Function}.
	 * @return the current Spec.
	 * @see BaseHttpInboundEndpoint#setStatusCodeExpression(Expression)
	 */
	public S statusCodeFunction(Function<RequestEntity<?>, ?> statusCodeFunction) {
		return statusCodeExpression(new FunctionExpression<>(statusCodeFunction));
	}

	/**
	 * Specify a {@link Validator} to validate a converted payload from request.
	 * @param validator the {@link Validator} to use.
	 * @return the spec
	 * @since 5.2
	 */
	public S validator(Validator validator) {
		this.target.setValidator(validator);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		HeaderMapper<HttpHeaders> headerMapperToRegister =
				(this.explicitHeaderMapper != null ? this.explicitHeaderMapper : this.headerMapper);
		return Collections.singletonMap(headerMapperToRegister, null);
	}

	/**
	 * A fluent API for the {@link RequestMapping}.
	 */
	public static final class RequestMappingSpec {

		private final RequestMapping requestMapping;

		RequestMappingSpec(RequestMapping requestMapping) {
			this.requestMapping = requestMapping;
		}

		/**
		 * The HTTP request methods to map to, narrowing the primary mapping:
		 * GET, POST, HEAD, OPTIONS, PUT, PATCH, DELETE, TRACE.
		 * @param supportedMethods the {@link HttpMethod}s to use.
		 * @return the spec
		 */
		public RequestMappingSpec methods(HttpMethod... supportedMethods) {
			this.requestMapping.setMethods(supportedMethods);
			return this;
		}

		/**
		 * The parameters of the mapped request, narrowing the primary mapping.
		 * @param params the request params to map to.
		 * @return the spec
		 */
		public RequestMappingSpec params(String... params) {
			this.requestMapping.setParams(params);
			return this;
		}

		/**
		 * The headers of the mapped request, narrowing the primary mapping.
		 * @param headers the request headers to map to.
		 * @return the spec
		 */
		public RequestMappingSpec headers(String... headers) {
			this.requestMapping.setHeaders(headers);
			return this;
		}

		/**
		 * The consumable media types of the mapped request, narrowing the primary mapping.
		 * @param consumes the media types for {@code Content-Type} header.
		 * @return the spec
		 */
		public RequestMappingSpec consumes(String... consumes) {
			this.requestMapping.setConsumes(consumes);
			return this;
		}

		/**
		 * The producible media types of the mapped request, narrowing the primary mapping.
		 * @param produces the media types for {@code Accept} header.
		 * @return the spec
		 */
		public RequestMappingSpec produces(String... produces) {
			this.requestMapping.setProduces(produces);
			return this;
		}

	}

	/**
	 * A fluent API for the {@link CrossOrigin}.
	 */
	public static final class CrossOriginSpec {

		private final CrossOrigin crossOrigin = new CrossOrigin();

		CrossOriginSpec() {
		}

		/**
		 * List of allowed origins, e.g. {@code "https://domain1.com"}.
		 * <p>These values are placed in the {@code Access-Control-Allow-Origin}
		 * header of both the pre-flight response and the actual response.
		 * {@code "*"} means that all origins are allowed.
		 * <p>If undefined, all origins are allowed.
		 * @param origin the list of allowed origins.
		 * @return the spec
		 */
		public CrossOriginSpec origin(String... origin) {
			this.crossOrigin.setOrigin(origin);
			return this;
		}

		/**
		 * List of request headers that can be used during the actual request.
		 * <p>This property controls the value of the pre-flight response's
		 * {@code Access-Control-Allow-Headers} header.
		 * {@code "*"}  means that all headers requested by the client are allowed.
		 * @param allowedHeaders the list of request headers.
		 * @return the spec
		 */
		public CrossOriginSpec allowedHeaders(String... allowedHeaders) {
			this.crossOrigin.setAllowedHeaders(allowedHeaders);
			return this;
		}

		/**
		 * List of response headers that the user-agent will allow the client to access.
		 * <p>This property controls the value of actual response's
		 * {@code Access-Control-Expose-Headers} header.
		 * @param exposedHeaders the list of response headers.
		 * @return the spec
		 */
		public CrossOriginSpec exposedHeaders(String... exposedHeaders) {
			this.crossOrigin.setExposedHeaders(exposedHeaders);
			return this;
		}

		/**
		 * List of supported HTTP request methods, e.g.
		 * {@code "{RequestMethod.GET, RequestMethod.POST}"}.
		 * <p>Methods specified here override those specified via {@code RequestMapping}.
		 * @param method the list of supported HTTP request methods
		 * @return the spec
		 */
		public CrossOriginSpec method(RequestMethod... method) {
			this.crossOrigin.setMethod(method);
			return this;
		}

		/**
		 * Whether the browser should include any cookies associated with the
		 * domain of the request being annotated.
		 * <p>Set to {@code "false"} if such cookies should not included.
		 * @param allowCredentials the {@code boolean} flag to include
		 * {@code Access-Control-Allow-Credentials=true} in pre-flight response or not
		 * @return the spec
		 */
		public CrossOriginSpec allowCredentials(Boolean allowCredentials) {
			this.crossOrigin.setAllowCredentials(allowCredentials);
			return this;
		}

		/**
		 * The maximum age (in seconds) of the cache duration for pre-flight responses.
		 * <p>This property controls the value of the {@code Access-Control-Max-Age}
		 * header in the pre-flight response.
		 * @param maxAge the maximum age (in seconds) of the cache duration for pre-flight responses.
		 * @return the spec
		 */
		public CrossOriginSpec maxAge(long maxAge) {
			this.crossOrigin.setMaxAge(maxAge);
			return this;
		}

	}

}
