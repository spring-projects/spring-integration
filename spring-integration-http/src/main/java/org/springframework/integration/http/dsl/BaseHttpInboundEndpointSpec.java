/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.http.dsl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.http.inbound.CrossOrigin;
import org.springframework.integration.http.inbound.HttpRequestHandlingEndpointSupport;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartResolver;

/**
 *
 * @param <S> the target {@link BaseHttpInboundEndpointSpec} implementation type.
 * @param <E> the target {@link HttpRequestHandlingEndpointSupport} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class BaseHttpInboundEndpointSpec<S extends BaseHttpInboundEndpointSpec<S, E>,
		E extends HttpRequestHandlingEndpointSupport>
		extends MessagingGatewaySpec<S, E> implements ComponentsRegistration {

	private final RequestMapping requestMapping = new RequestMapping();

	private final Map<String, Expression> headerExpressions = new HashMap<>();

	private final HeaderMapper<HttpHeaders> headerMapper = DefaultHttpHeaderMapper.inboundMapper();

	private HeaderMapper<HttpHeaders> explicitHeaderMapper;

	BaseHttpInboundEndpointSpec(E endpoint, String... path) {
		super(endpoint);
		this.requestMapping.setPathPatterns(path);
		this.target.setRequestMapping(this.requestMapping);
		this.target.setHeaderExpressions(this.headerExpressions);
		this.target.setHeaderMapper(this.headerMapper);
	}

	public S requestMapping(Consumer<RequestMappingSpec> requestMapping) {
		requestMapping.accept(new RequestMappingSpec(this.requestMapping));
		return _this();
	}

	public S crossOrigin(Consumer<CrossOriginSpec> crossOrigin) {
		CrossOriginSpec originSpec = new CrossOriginSpec();
		crossOrigin.accept(originSpec);
		this.target.setCrossOrigin(originSpec.crossOrigin);
		return _this();
	}

	public S payloadExpression(String payloadExpression) {
		return payloadExpression(PARSER.parseExpression(payloadExpression));
	}

	public S payloadExpression(Expression payloadExpression) {
		this.target.setPayloadExpression(payloadExpression);
		return _this();
	}

	public <P> S payloadFunction(Function<HttpEntity<P>, ?> payloadFunction) {
		return payloadExpression(new FunctionExpression<>(payloadFunction));
	}

	public S headerExpressions(Map<String, Expression> headerExpressions) {
		Assert.notNull(headerExpressions, "'headerExpressions' must not be null");
		this.headerExpressions.clear();
		this.headerExpressions.putAll(headerExpressions);
		return _this();
	}

	public S headerExpression(String header, Expression expression) {
		this.headerExpressions.put(header, expression);
		return _this();
	}

	public S headerExpression(String header, String expression) {
		return headerExpression(header, PARSER.parseExpression(expression));
	}

	/**
	 * @param header the header name to add.
	 * @param headerFunction the function to evaluate the header value against {@link HttpEntity}.
	 * @return the current Spec.
	 * @see HttpRequestHandlingEndpointSupport#setHeaderExpressions(Map)
	 */
	public S headerFunction(String header, Function<HttpEntity<?>, ?> headerFunction) {
		return headerExpression(header, new FunctionExpression<HttpEntity<?>>(headerFunction));
	}

	public S messageConverters(HttpMessageConverter<?>... messageConverters) {
		this.target.setMessageConverters(Arrays.asList(messageConverters));
		return _this();
	}

	public S mergeWithDefaultConverters(boolean mergeWithDefaultConverters) {
		this.target.setMergeWithDefaultConverters(mergeWithDefaultConverters);
		return _this();
	}

	public S headerMapper(HeaderMapper<HttpHeaders> headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		this.explicitHeaderMapper = headerMapper;
		return _this();
	}

	public S mappedRequestHeaders(String... patterns) {
		Assert.isNull(this.explicitHeaderMapper,
				"The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': "
						+ this.explicitHeaderMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setOutboundHeaderNames(patterns);
		return _this();
	}

	public S mappedResponseHeaders(String... patterns) {
		Assert.isNull(this.explicitHeaderMapper,
				"The 'mappedRequestHeaders' must be specified on the provided 'headerMapper': "
						+ this.explicitHeaderMapper);
		((DefaultHttpHeaderMapper) this.headerMapper).setInboundHeaderNames(patterns);
		return _this();
	}

	public S requestPayloadType(Class<?> requestPayloadType) {
		this.target.setRequestPayloadType(requestPayloadType);
		return _this();
	}

	public S extractReplyPayload(boolean extractReplyPayload) {
		this.target.setExtractReplyPayload(extractReplyPayload);
		return _this();
	}

	public S multipartResolver(MultipartResolver multipartResolver) {
		this.target.setMultipartResolver(multipartResolver);
		return _this();
	}

	public S statusCodeExpression(Expression statusCodeExpression) {
		this.target.setStatusCodeExpression(statusCodeExpression);
		return _this();
	}

	public S statusCodeFunction(Function<Void, ?> statusCodeFunction) {
		return statusCodeExpression(new FunctionExpression<>(statusCodeFunction));
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		HeaderMapper<HttpHeaders> headerMapperToRegister =
				(this.explicitHeaderMapper != null ? this.explicitHeaderMapper : this.headerMapper);
		return Collections.singletonList(headerMapperToRegister);
	}

	/**
	 * A fluent API for the {@link RequestMapping}.
	 */
	public static final class RequestMappingSpec {

		private final RequestMapping requestMapping;

		RequestMappingSpec(RequestMapping requestMapping) {
			this.requestMapping = requestMapping;
		}

		public RequestMappingSpec methods(HttpMethod... supportedMethods) {
			this.requestMapping.setMethods(supportedMethods);
			return this;
		}

		public RequestMappingSpec params(String... params) {
			this.requestMapping.setParams(params);
			return this;
		}

		public RequestMappingSpec headers(String... headers) {
			this.requestMapping.setHeaders(headers);
			return this;
		}

		public RequestMappingSpec consumes(String... consumes) {
			this.requestMapping.setConsumes(consumes);
			return this;
		}

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
			super();
		}

		public CrossOriginSpec origin(String... origin) {
			this.crossOrigin.setOrigin(origin);
			return this;
		}

		public CrossOriginSpec allowedHeaders(String... allowedHeaders) {
			this.crossOrigin.setAllowedHeaders(allowedHeaders);
			return this;
		}

		public CrossOriginSpec exposedHeaders(String... exposedHeaders) {
			this.crossOrigin.setExposedHeaders(exposedHeaders);
			return this;
		}

		public CrossOriginSpec method(RequestMethod... method) {
			this.crossOrigin.setMethod(method);
			return this;
		}

		public CrossOriginSpec allowCredentials(Boolean allowCredentials) {
			this.crossOrigin.setAllowCredentials(allowCredentials);
			return this;
		}

		public CrossOriginSpec maxAge(long maxAge) {
			this.crossOrigin.setMaxAge(maxAge);
			return this;
		}

	}

}
