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

package org.springframework.integration.gateway;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A Message Mapper implementation that supports mapping <i>to</i> a
 * Message from an argument array when invoking gateway methods.
 * <p/>
 * Some examples of legal method signatures:<br/>
 * <tt>public void dealWith(Object payload);</tt><br/>
 * <tt>public void dealWith(Message message);</tt><br/>
 * <tt>public void dealWith(@Header String myHeader, Object payload);</tt><br/>
 * <br/>
 * <tt>public void dealWith(@Headers Map headers, Object payload);</tt><br/>
 * <tt>public void dealWith(@Headers Properties headers, Map payload);</tt><br/>
 * <tt>public void dealWith(Properties headers, Object payload);</tt><br/>
 * <p/>
 * Some examples of illegal method signatures: <br/>
 * <tt>public void dealWith(Object payload, String payload);</tt><br/>
 * <tt>public void dealWith(Message message, Object payload);</tt><br/>
 * <tt>public void dealWith(Properties headers, Map payload);</tt><br/>
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class GatewayMethodInboundMessageMapper implements InboundMessageMapper<Object[]>, BeanFactoryAware {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();


	private final Method method;

	private final Map<String, Expression> headerExpressions;

	private final List<MethodParameter> parameterList;

	private volatile Expression payloadExpression;

	private final Map<String, Expression> parameterPayloadExpressions = new HashMap<String, Expression>();

	private final StandardEvaluationContext staticEvaluationContext = new StandardEvaluationContext();

	private volatile BeanResolver beanResolver;


	public GatewayMethodInboundMessageMapper(Method method) {
		this(method, null);
	}

	public GatewayMethodInboundMessageMapper(Method method, Map<String, Expression> headerExpressions) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.headerExpressions = headerExpressions;
		this.parameterList = getMethodParameterList(method);
		this.payloadExpression = parsePayloadExpression(method);
	}


	public void setPayloadExpression(String expressionString) {
		this.payloadExpression = PARSER.parseExpression(expressionString);
	}

	public void setBeanFactory(final BeanFactory beanFactory) {
		if (beanFactory != null) {
			this.beanResolver = new BeanFactoryResolver(beanFactory);
			this.staticEvaluationContext.setBeanResolver(beanResolver);
		}
	}

	public Message<?> toMessage(Object[] arguments) {
		Assert.notNull(arguments, "cannot map null arguments to Message");
		if (arguments.length != this.parameterList.size()) {
			String prefix = (arguments.length < this.parameterList.size()) ? "Not enough" : "Too many";
			throw new IllegalArgumentException(prefix + " parameters provided for method [" + method +
					"], expected " + this.parameterList.size() + " but received " + arguments.length + ".");
		}

		return this.mapArgumentsToMessage(arguments);
	}

	private Message<?> mapArgumentsToMessage(Object[] arguments) {
		EvaluationContext methodInvocationEvaluationContext = createMethodInvocationEvaluationContext(arguments);

		Map<String, Object> evaluatedHeaders = new HashMap<String, Object>();
		List<Object> evaluatedPayloads = new ArrayList<Object>();

		// process payload SpEL (XML)
		if (this.payloadExpression != null) {
			evaluatedPayloads.add(this.payloadExpression.getValue(methodInvocationEvaluationContext));
		}

		// only set to true if mapped via SpEL or Annotation
		boolean payloadExplicitlyMapped = this.payloadExpression != null;
		// only set to true if mapped via SpEL or Annotation
		boolean headersExplicitlyMapped = !CollectionUtils.isEmpty(this.headerExpressions);

		if (!payloadExplicitlyMapped){
			Assert.notEmpty(arguments, "Empty argument list can not be mapped to the Message");
		}

		for (int i = 0; i < this.parameterList.size(); i++) {
			Object argumentValue = arguments[i];
			MethodParameter methodParameter = this.parameterList.get(i);
			Annotation annotation = this.findMappingAnnotation(methodParameter.getParameterAnnotations());
			if (annotation != null) {
				// Get annotated payload (if exists)
				if (annotation.annotationType().equals(Payload.class)){
					if (!payloadExplicitlyMapped ){
						Object result = this.processAnnotatedPayload(annotation, methodParameter, argumentValue);
						if (result != null){
							payloadExplicitlyMapped = evaluatedPayloads.add(result);
							//payloadExplicitlyMapped = true;
						}
					}
					else {
						this.throwExceptionForMultipleMessageOrPayloadParameters(method);
					}
				}
				// Get annotated headers (if exists)
				Map<String, Object> processedHeaders = this.processAnnotatedHeaders(annotation, methodParameter, argumentValue);
				if (!CollectionUtils.isEmpty(processedHeaders)){
					evaluatedHeaders.putAll(processedHeaders);
					headersExplicitlyMapped = true;
				}
			}
			else if (Map.class.isAssignableFrom(methodParameter.getParameterType()) && !headersExplicitlyMapped) {
				// we only get here to process by convention (no Annotation, no SpEL
				Type type =  methodParameter.getGenericParameterType();
				if (type instanceof ParameterizedType){
					Class<?> keyType = (Class<?>) ((ParameterizedType)type).getActualTypeArguments()[0];
					if (keyType.isAssignableFrom(String.class) && i > 0){ // first argument can not be headers
						this.copyHeaders((Map<?, ?>) argumentValue, evaluatedHeaders);
					}
					else {
						this.mapToPayloadIfNecessary(payloadExplicitlyMapped, evaluatedPayloads, argumentValue);
					}
				}
				else {
					this.mapToPayloadIfNecessary(payloadExplicitlyMapped, evaluatedPayloads, argumentValue);
				}
			}
			else {
				this.mapToPayloadIfNecessary(payloadExplicitlyMapped, evaluatedPayloads, argumentValue);
			}
		}

		Assert.isTrue(evaluatedPayloads.size() > 0, "unable to determine a Message or payload parameter on method [" + method + "]");

		if (evaluatedPayloads.size() > 1){
			this.throwExceptionForMultipleMessageOrPayloadParameters(method);
		}

		// process headers SpEL (XML)
		// unlike payload which could only be set once, headers could be set with both XML and Annotation,
		// however if there is any collision XML-based configuration wins to maintain Spring conventions and
		// therefore should be able to override whatever headers might have been set through annotations
		// that's why we are processing it last.
		if (!CollectionUtils.isEmpty(this.headerExpressions)) {
			for (Map.Entry<String, Expression> entry : this.headerExpressions.entrySet()) {
				Object value = entry.getValue().getValue(methodInvocationEvaluationContext);
				if (value != null) {
					evaluatedHeaders.put(entry.getKey(), value);
				}
			}
		}

		return this.buildMessage(evaluatedPayloads.iterator().next(), evaluatedHeaders);
	}

	private Message<?> buildMessage(Object candidate, Map<String, Object> headers){
		MessageBuilder<?> builder = (candidate instanceof Message)
				? MessageBuilder.fromMessage((Message<?>) candidate)
				: MessageBuilder.withPayload(candidate);
		builder.copyHeadersIfAbsent(headers);
		return builder.build();
	}

	private void mapToPayloadIfNecessary(boolean payloadMapped, List<Object> evaluatedPayloads, Object argumentValue){
		if (!payloadMapped) {
			evaluatedPayloads.add(argumentValue);
		}
		// else ignoring it since payload was mapped via SpEL or Annotation and therefore
		// no conventional mapping of the payoload will be attempted.
	}

	private Map<String, Object> processAnnotatedHeaders(Annotation annotation, MethodParameter methodParameter, Object argumentValue){
		Map<String, Object> evaluatedHeaders = new HashMap<String, Object>();
		if (annotation.annotationType().equals(Header.class)) {
			Header headerAnnotation = (Header) annotation;
			String headerName = this.determineHeaderName(headerAnnotation, methodParameter);
			if (headerAnnotation.required() && argumentValue == null) {
				throw new IllegalArgumentException("Received null argument value for required header: '" + headerName + "'");
			}
			evaluatedHeaders.put(headerName, argumentValue);
		}
		else if (annotation.annotationType().equals(Headers.class)) {
			if (argumentValue != null) {
				Assert.isInstanceOf(Map.class, argumentValue, "@Headers annotation is only valid for Map-typed parameters");
				for (Object key : ((Map<?, ?>) argumentValue).keySet()) {
					Assert.isInstanceOf(String.class, key, "Invalid header name [" + key +
							"], name type must be String.");
					Object value = ((Map<?, ?>) argumentValue).get(key);
					evaluatedHeaders.put((String) key, value);
				}
			}
		}
		return evaluatedHeaders;
	}

	private Object processAnnotatedPayload(Annotation annotation, MethodParameter methodParameter, Object argumentValue){
		String expression = ((Payload) annotation).value();
		if (!StringUtils.hasText(expression)) {
			return argumentValue;
		}
		else {
			return this.evaluatePayloadExpression(expression, argumentValue);
		}
//		else if (annotation.annotationType().equals(Payload.class)) {
//			if (!payloadMapped){
//				payloadMapped = true;
//				String expression = ((Payload) annotation).value();
//				if (!StringUtils.hasText(expression)) {
//					evaluatedPayloads.add(argumentValue);
//				}
//				else {
//					evaluatedPayloads.add(this.evaluatePayloadExpression(expression, argumentValue));
//				}
//			}
//			else {
//				throw new MessagingException("foo");
//			}
//		}
	}

	private StandardEvaluationContext createMethodInvocationEvaluationContext(Object[] arguments) {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("args", arguments);
		context.setVariable("method", this.method.getName());
		if (this.beanResolver != null) {
			context.setBeanResolver(this.beanResolver);
		}
		return context;
	}

	private Object evaluatePayloadExpression(String expressionString, Object argumentValue) {
		Expression expression = this.parameterPayloadExpressions.get(expressionString);
		if (expression == null) {
			expression = PARSER.parseExpression(expressionString);
			this.parameterPayloadExpressions.put(expressionString, expression);
		}
		return expression.getValue(this.staticEvaluationContext, argumentValue);
	}

	private Annotation findMappingAnnotation(Annotation[] annotations) {
		if (annotations == null || annotations.length == 0) {
			return null;
		}
		Annotation match = null;
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> type = annotation.annotationType();
			if (type.equals(Payload.class) || type.equals(Header.class) || type.equals(Headers.class)) {
				if (match != null) {
					throw new MessagingException("At most one parameter annotation can be provided for message mapping, " +
							"but found two: [" + match.annotationType().getName() + "] and [" + annotation.annotationType().getName() + "]");
				}
				match = annotation;
			}
		}
		return match;
	}

	private void copyHeaders(Map<?, ?> argumentValue, Map<String, Object> headers) {
		for (Object key : argumentValue.keySet()) {
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Invalid header name [" + key +
					"], name type must be String.");
			}
			Object value = argumentValue.get(key);
			headers.put((String) key, value);
		}
	}

	private void throwExceptionForMultipleMessageOrPayloadParameters(Method method) {
		throw new MessagingException(
				"At most one parameter (or expression via method-level @Payload) may be mapped to the " +
				"payload or Message. Found more than one on method [" + method + "]");
	}

	private String determineHeaderName(Header headerAnnotation, MethodParameter methodParameter) {
		String valueAttribute = headerAnnotation.value();
		String headerName = StringUtils.hasText(valueAttribute) ? valueAttribute : methodParameter.getParameterName();
		Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is " +
				"disabled or header name is not explicitly provided via @Header annotation.");
		return headerName;
	}

	private static List<MethodParameter> getMethodParameterList(Method method) {
		List<MethodParameter> parameterList = new LinkedList<MethodParameter>();
		ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();
		int parameterCount = method.getParameterTypes().length;
		for (int i = 0; i < parameterCount; i++) {
			MethodParameter methodParameter = new MethodParameter(method, i);
			methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
			parameterList.add(methodParameter);
		}
		return parameterList;
	}

	private static Expression parsePayloadExpression(Method method) {
		Expression expression = null;
		Payload payload = method.getAnnotation(Payload.class);
		if (payload != null) {
			String expressionString = payload.value();
			Assert.hasText(expressionString,
					"@Payload at method-level on a Gateway must provide a non-empty Expression.");
			expression = PARSER.parseExpression(expressionString);
		}
		return expression;
	}

}
