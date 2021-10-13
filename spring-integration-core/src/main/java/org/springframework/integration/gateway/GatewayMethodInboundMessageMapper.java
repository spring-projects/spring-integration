/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.gateway;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A Message Mapper implementation that supports mapping <i>to</i> a
 * Message from an argument array when invoking gateway methods.
 * <p>
 * Some examples of legal method signatures:<br>
 * <tt>public void dealWith(Object payload);</tt><br>
 * <tt>public void dealWith(Message message);</tt><br>
 * <tt>public void dealWith(@Header String myHeader, Object payload);</tt><br>
 * <br>
 * <tt>public void dealWith(@Headers Map headers, Object payload);</tt><br>
 * <tt>public void dealWith(@Headers Properties headers, Map payload);</tt><br>
 * <tt>public void dealWith(Properties headers, Object payload);</tt><br>
 * <p>
 * Some examples of illegal method signatures: <br>
 * <tt>public void dealWith(Object payload, String payload);</tt><br>
 * <tt>public void dealWith(Message message, Object payload);</tt><br>
 * <tt>public void dealWith(Properties headers, Map payload);</tt><br>
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
class GatewayMethodInboundMessageMapper implements InboundMessageMapper<Object[]>, BeanFactoryAware {

	private static final Log LOGGER = LogFactory.getLog(GatewayMethodInboundMessageMapper.class);

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final Map<String, Expression> parameterPayloadExpressions = new HashMap<>();

	private final Method method;

	private final Map<String, Expression> headerExpressions;

	private final Map<String, Expression> globalHeaderExpressions;

	private final Map<String, Object> headers;

	private final List<MethodParameter> parameterList;

	private final MethodArgsMessageMapper argsMapper;

	private final MessageBuilderFactory messageBuilderFactory;

	private Expression payloadExpression;

	private EvaluationContext payloadExpressionEvaluationContext;

	private BeanFactory beanFactory;

	@Nullable
	private Expression sendTimeoutExpression;

	@Nullable
	private Expression replyTimeoutExpression;

	GatewayMethodInboundMessageMapper(Method method) {
		this(method, null);
	}

	GatewayMethodInboundMessageMapper(Method method, @Nullable Map<String, Expression> headerExpressions) {
		this(method, headerExpressions, null, null, null);
	}

	GatewayMethodInboundMessageMapper(Method method,
			@Nullable Map<String, Expression> headerExpressions,
			@Nullable Map<String, Expression> globalHeaderExpressions,
			@Nullable MethodArgsMessageMapper mapper,
			@Nullable MessageBuilderFactory messageBuilderFactory) {

		this(method, headerExpressions, globalHeaderExpressions, null, mapper, messageBuilderFactory);
	}

	GatewayMethodInboundMessageMapper(Method method,
			@Nullable Map<String, Expression> headerExpressions,
			@Nullable Map<String, Expression> globalHeaderExpressions,
			@Nullable Map<String, Object> headers,
			@Nullable MethodArgsMessageMapper mapper,
			@Nullable MessageBuilderFactory messageBuilderFactory) {

		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.headerExpressions = headerExpressions;
		this.headers = headers;
		this.globalHeaderExpressions = globalHeaderExpressions;
		this.parameterList = getMethodParameterList(method);
		this.payloadExpression = parsePayloadExpression(method);
		if (messageBuilderFactory == null) {
			this.messageBuilderFactory = new DefaultMessageBuilderFactory();
		}
		else {
			this.messageBuilderFactory = messageBuilderFactory;
		}
		if (mapper == null) {
			this.argsMapper = new DefaultMethodArgsMessageMapper();
		}
		else {
			this.argsMapper = mapper;
		}
	}


	public void setPayloadExpression(Expression expressionString) {
		this.payloadExpression = expressionString;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.payloadExpressionEvaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);
	}

	public void setSendTimeoutExpression(Expression sendTimeoutExpression) {
		this.sendTimeoutExpression = sendTimeoutExpression;
	}

	public void setReplyTimeoutExpression(Expression replyTimeoutExpression) {
		this.replyTimeoutExpression = replyTimeoutExpression;
	}

	@Override
	public Message<?> toMessage(Object[] arguments, @Nullable Map<String, Object> headers) {
		Assert.notNull(arguments, "cannot map null arguments to Message");
		if (arguments.length != this.parameterList.size()) {
			String prefix = (arguments.length < this.parameterList.size()) ? "Not enough" : "Too many";
			throw new IllegalArgumentException(prefix + " parameters provided for method [" + this.method +
					"], expected " + this.parameterList.size() + " but received " + arguments.length + ".");
		}
		return mapArgumentsToMessage(arguments, headers);
	}

	@Nullable
	private Message<?> mapArgumentsToMessage(Object[] arguments, @Nullable Map<String, Object> headers) {
		try {
			return this.argsMapper.toMessage(new MethodArgsHolder(this.method, arguments), headers);
		}
		catch (MessagingException e) { // NOSONAR to avoid if..else
			throw e;
		}
		catch (Exception e) {
			throw new MessageMappingException("Failed to map arguments: " + Arrays.toString(arguments), e);
		}
	}

	private Map<String, Object> evaluateHeaders(EvaluationContext methodInvocationEvaluationContext,
			MethodArgsHolder methodArgsHolder, Map<String, Expression> headerExpressions) {

		Map<String, Object> evaluatedHeaders = new HashMap<>();
		for (Map.Entry<String, Expression> entry : headerExpressions.entrySet()) {
			Object value = entry.getValue().getValue(methodInvocationEvaluationContext, methodArgsHolder);
			evaluatedHeaders.put(entry.getKey(), value);
		}
		return evaluatedHeaders;
	}

	// TODO Remove in the future release. The MethodArgsHolder as a root object covers this use-case.
	private StandardEvaluationContext createMethodInvocationEvaluationContext(Object[] arguments) {
		StandardEvaluationContext context = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		context.setVariable("args", arguments);
		context.setVariable("gatewayMethod", this.method);
		return context;
	}

	@Nullable
	private Object evaluatePayloadExpression(String expressionString, Object argumentValue) {
		Expression expression =
				this.parameterPayloadExpressions.computeIfAbsent(expressionString, PARSER::parseExpression);
		return expression.getValue(this.payloadExpressionEvaluationContext, argumentValue);
	}


	private void copyHeaders(Map<?, ?> argumentValue, Map<String, Object> headers) {
		for (Entry<?, ?> entry : argumentValue.entrySet()) {
			Object key = entry.getKey();
			if (!(key instanceof String)) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Invalid header name [" + key +
							"], name type must be String. Skipping mapping of this header to MessageHeaders.");
				}
			}
			else {
				headers.put((String) key, entry.getValue());
			}
		}
	}

	private void throwExceptionForMultipleMessageOrPayloadParameters(MethodParameter methodParameter) {
		throw new MessagingException(
				"At most one parameter (or expression via method-level @Payload) may be mapped to the " +
						"payload or Message. Found more than one on method [" + methodParameter.getMethod() + "]");
	}

	static String determineHeaderName(Annotation headerAnnotation, MethodParameter methodParameter) {
		String valueAttribute = (String) AnnotationUtils.getValue(headerAnnotation);
		String headerName = StringUtils.hasText(valueAttribute) ? valueAttribute : methodParameter.getParameterName();
		Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is " +
				"disabled or header name is not explicitly provided via @Header annotation.");
		return headerName;
	}

	static List<MethodParameter> getMethodParameterList(Method method) {
		List<MethodParameter> parameterList = new LinkedList<>();
		ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		for (int i = 0; i < method.getParameterCount(); i++) {
			MethodParameter methodParameter = new SynthesizingMethodParameter(method, i);
			methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
			parameterList.add(methodParameter);
		}
		return parameterList;
	}

	@Nullable
	private static Expression parsePayloadExpression(Method method) {
		Expression expression = null;
		Annotation payload = method.getAnnotation(Payload.class);
		if (payload != null) {
			String expressionString = (String) AnnotationUtils.getValue(payload);
			Assert.hasText(expressionString,
					"@Payload at method-level on a Gateway must provide a non-empty Expression.");
			expression = PARSER.parseExpression(expressionString); // NOSONAR protected with hasText()
		}
		return expression;
	}

	public class DefaultMethodArgsMessageMapper implements MethodArgsMessageMapper {

		private final MessageBuilderFactory msgBuilderFactory =
				GatewayMethodInboundMessageMapper.this.messageBuilderFactory;

		@Override
		public Message<?> toMessage(MethodArgsHolder holder, @Nullable Map<String, Object> headersToMap) {
			Object messageOrPayload = null;
			boolean foundPayloadAnnotation = false;
			Object[] arguments = holder.getArgs();
			EvaluationContext methodInvocationEvaluationContext = createMethodInvocationEvaluationContext(arguments);
			Map<String, Object> headersToPopulate =
					headersToMap != null
							? new HashMap<>(headersToMap)
							: new HashMap<>();
			if (GatewayMethodInboundMessageMapper.this.payloadExpression != null) {
				messageOrPayload =
						GatewayMethodInboundMessageMapper.this.payloadExpression.getValue(
								methodInvocationEvaluationContext, holder);
			}
			for (int i = 0; i < GatewayMethodInboundMessageMapper.this.parameterList.size(); i++) {
				Object argumentValue = arguments[i];
				MethodParameter methodParameter = GatewayMethodInboundMessageMapper.this.parameterList.get(i);
				Annotation annotation =
						MessagingAnnotationUtils.findMessagePartAnnotation(methodParameter.getParameterAnnotations(),
								false);
				if (annotation != null) {
					if (annotation.annotationType().equals(Payload.class)) {
						messageOrPayload =
								processPayloadAnnotation(messageOrPayload, argumentValue, methodParameter, annotation);
						foundPayloadAnnotation = true;
					}
					else {
						headerOrHeaders(headersToPopulate, argumentValue, methodParameter, annotation);
					}
				}
				else if (messageOrPayload == null) {
					messageOrPayload = argumentValue;
				}
				else if (Map.class.isAssignableFrom(methodParameter.getParameterType())) {
					processMapArgument(messageOrPayload, foundPayloadAnnotation, headersToPopulate,
							(Map<?, ?>) argumentValue);
				}
				else if (GatewayMethodInboundMessageMapper.this.payloadExpression == null) {
					throwExceptionForMultipleMessageOrPayloadParameters(methodParameter);
				}
			}

			Assert.isTrue(messageOrPayload != null,
					() -> "The 'payload' (or 'Message') for gateway [" + GatewayMethodInboundMessageMapper.this.method +
							"] method call cannot be determined (must not be 'null') from the provided arguments: " +
							Arrays.toString(arguments));
			populateSendAndReplyTimeoutHeaders(methodInvocationEvaluationContext, holder, headersToPopulate);
			return buildMessage(holder, headersToPopulate, messageOrPayload, methodInvocationEvaluationContext);
		}

		private void headerOrHeaders(Map<String, Object> headersToPopulate, Object argumentValue,
				MethodParameter methodParameter, Annotation annotation) {

			if (annotation.annotationType().equals(Header.class)) {
				processHeaderAnnotation(headersToPopulate, argumentValue, methodParameter, annotation);
			}
			else if (annotation.annotationType().equals(Headers.class)) {
				processHeadersAnnotation(headersToPopulate, argumentValue);
			}
		}

		@Nullable
		private Object processPayloadAnnotation(@Nullable Object messageOrPayload,
				Object argumentValue, MethodParameter methodParameter, Annotation annotation) {

			if (messageOrPayload != null) {
				throwExceptionForMultipleMessageOrPayloadParameters(methodParameter);
			}
			String expression = (String) AnnotationUtils.getValue(annotation);
			if (!StringUtils.hasText(expression)) {
				return argumentValue;
			}
			else {
				return evaluatePayloadExpression(expression, argumentValue);
			}
		}

		private void processHeaderAnnotation(Map<String, Object> headersToPopulate, @Nullable Object argumentValue,
				MethodParameter methodParameter, Annotation annotation) {

			String headerName = determineHeaderName(annotation, methodParameter);
			if ((Boolean) AnnotationUtils.getValue(annotation, "required") // NOSONAR never null
					&& argumentValue == null) {
				throw new IllegalArgumentException("Received null argument value for required header: '"
						+ headerName + "'");
			}
			headersToPopulate.put(headerName, argumentValue);
		}

		private void processHeadersAnnotation(Map<String, Object> headersToPopulate, @Nullable Object argumentValue) {
			if (argumentValue != null) {
				if (!(argumentValue instanceof Map)) {
					throw new IllegalArgumentException(
							"@Headers annotation is only valid for Map-typed parameters");
				}
				for (Object key : ((Map<?, ?>) argumentValue).keySet()) {
					Assert.isInstanceOf(String.class, key, "Invalid header name [" + key +
							"], name type must be String.");
					Object value = ((Map<?, ?>) argumentValue).get(key);
					headersToPopulate.put((String) key, value);
				}
			}
		}

		private void processMapArgument(Object messageOrPayload, boolean foundPayloadAnnotation,
				Map<String, Object> headersToPopulate, Map<?, ?> argumentValue) {

			if (messageOrPayload instanceof Map && !foundPayloadAnnotation
					&& GatewayMethodInboundMessageMapper.this.payloadExpression == null) {
				throw new MessagingException("Ambiguous method parameters; found more than one " +
						"Map-typed parameter and neither one contains a @Payload annotation");
			}
			copyHeaders(argumentValue, headersToPopulate);
		}

		private void populateSendAndReplyTimeoutHeaders(EvaluationContext methodInvocationEvaluationContext,
				MethodArgsHolder methodArgsHolder, Map<String, Object> headersToPopulate) {

			if (GatewayMethodInboundMessageMapper.this.sendTimeoutExpression != null) {
				headersToPopulate.computeIfAbsent(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER,
						v -> GatewayMethodInboundMessageMapper.this.sendTimeoutExpression
								.getValue(methodInvocationEvaluationContext, methodArgsHolder, Long.class));
			}
			if (GatewayMethodInboundMessageMapper.this.replyTimeoutExpression != null) {
				headersToPopulate.computeIfAbsent(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER,
						v -> GatewayMethodInboundMessageMapper.this.replyTimeoutExpression
								.getValue(methodInvocationEvaluationContext, methodArgsHolder, Long.class));
			}
		}

		private Message<?> buildMessage(MethodArgsHolder methodArgsHolder, Map<String, Object> headers,
				Object messageOrPayload, EvaluationContext methodInvocationEvaluationContext) {

			AbstractIntegrationMessageBuilder<?> builder =
					(messageOrPayload instanceof Message)
							? this.msgBuilderFactory.fromMessage((Message<?>) messageOrPayload)
							: this.msgBuilderFactory.withPayload(messageOrPayload);
			builder.copyHeadersIfAbsent(headers);
			// Explicit headers in XML override any @Header annotations...
			if (!CollectionUtils.isEmpty(GatewayMethodInboundMessageMapper.this.headerExpressions)) {
				Map<String, Object> evaluatedHeaders = evaluateHeaders(methodInvocationEvaluationContext,
						methodArgsHolder, GatewayMethodInboundMessageMapper.this.headerExpressions);
				builder.copyHeaders(evaluatedHeaders);
			}
			// ...whereas global (default) headers do not...
			if (!CollectionUtils.isEmpty(GatewayMethodInboundMessageMapper.this.globalHeaderExpressions)) {
				Map<String, Object> evaluatedHeaders = evaluateHeaders(methodInvocationEvaluationContext,
						methodArgsHolder, GatewayMethodInboundMessageMapper.this.globalHeaderExpressions);
				builder.copyHeadersIfAbsent(evaluatedHeaders);
			}
			if (GatewayMethodInboundMessageMapper.this.headers != null) {
				builder.copyHeadersIfAbsent(GatewayMethodInboundMessageMapper.this.headers);
			}
			return builder.build();
		}

	}

}
