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

package org.springframework.integration.handler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.AccessException;
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
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A Message Mapper implementation that supports mapping <i>from</i> a Message
 * to an argument array when invoking handler methods, and mapping <i>to</i> a
 * Message from an argument array when invoking gateway methods.
 * <p/>
 * When mapping from a Message, the method parameters are matched
 * against the Message, its payload as well as its headers. A message or
 * payload parameter must not be annotated, and there can be at most one of
 * these. In certain special cases, more than one non-annotated parameter can
 * be used (more on this later), but there should always be at most one
 * parameter that is expecting the message or its payload.
 * <p/>
 * If a method parameter is annotated with {@link Header @Header}, the
 * annotation's value will be used as a header name. If such an annotation
 * contains no value, then the parameter name will be used as long as the
 * information is available in the class file (requires compilation with debug
 * settings for parameter names).
 * <p/>
 * In addition a Map or Properties parameter can receive multiple message
 * headers. In the case of a Map argument, all headers will be passed, but in
 * the case of a Properties argument, only the headers with String-typed values
 * will be passed. These parameters can be labeled explicitly with the
 * {@link Headers @Headers} annotation, or matched implicitly by using a non-
 * ambiguous method signature. There can be as many parameters annotated with
 * {@code @Header} as necessary, but typically there should be only one parameter
 * expecting multiple headers (with or without the {@code @Headers} annotation).
 * <p/>
 * If a Map or Properties object is expected, and the payload is not itself
 * assignable to that type or capable of being converted to that type, then
 * the MessageHeaders' values will be passed in the case of a Map-typed
 * parameter, or the MessageHeaders' String-based values will be passed in the
 * case of a Properties-typed parameter. In these cases multiple unannotated
 * parameters are legal. If, however, the actual payload type is a Map or
 * Properties instance, then this ambiguity cannot be resolved. For that
 * reason, it is highly recommended to use the explicit
 * {@code Headers @Headers} annotation whenever possible.
 * <p/>
 * Some examples of legal method signatures:<br/>
 * <tt>public void dealWith(Object payload);</tt><br/>
 * <tt>public void dealWith(Message message);</tt><br/>
 * <tt>public void dealWith(@Header String myHeader, Object payload);</tt><br/>
 * <tt>public void dealWith(@Header String myHeader, @Header String anotherHeader);</tt>
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
public class ArgumentArrayMessageMapper implements InboundMessageMapper<Object[]>, BeanFactoryAware {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();


	private final Method method;

	private final Map<String, Object> staticHeaders;

	private final List<MethodParameter> parameterList;

	private volatile Expression payloadExpression;

	private final Map<String, Expression> parameterPayloadExpressions = new HashMap<String, Expression>();

	private final StandardEvaluationContext staticEvaluationContext = new StandardEvaluationContext();

	private volatile BeanResolver beanResolver;


	public ArgumentArrayMessageMapper(Method method) {
		this(method, null);
	}
	
	public ArgumentArrayMessageMapper(Method method, Map<String, Object> staticHeaders) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.staticHeaders = staticHeaders;
		this.parameterList = getMethodParameterList(method);
		this.payloadExpression = parsePayloadExpression(method);
	}


	public void setPayloadExpression(String expressionString) {
		this.payloadExpression = PARSER.parseExpression(expressionString);
	}

	public void setBeanFactory(final BeanFactory beanFactory) {
		if (beanFactory != null) {
			this.beanResolver = new BeanResolver() {
				public Object resolve(EvaluationContext context, String beanName) throws AccessException {
					return beanFactory.getBean(beanName);
				}
			};
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

	@SuppressWarnings("unchecked")
	private Message<?> mapArgumentsToMessage(Object[] arguments) {
		Object messageOrPayload = null;
		boolean foundPayloadAnnotation = false;
		Map<String, Object> headers = new HashMap<String, Object>();
		if (this.payloadExpression != null) {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setVariable("args", arguments);
			if (this.beanResolver != null) {
				context.setBeanResolver(this.beanResolver);
			}
			messageOrPayload = this.payloadExpression.getValue(context);
		}
		for (int i = 0; i < this.parameterList.size(); i++) {
			Object argumentValue = arguments[i];		
			MethodParameter methodParameter = this.parameterList.get(i);
			Annotation annotation = this.findMappingAnnotation(methodParameter.getParameterAnnotations());
			if (annotation != null) {
				if (annotation.annotationType().equals(Payload.class)) {
					if (messageOrPayload != null) {
						this.throwExceptionForMultipleMessageOrPayloadParameters(methodParameter);
					}
					String expression = ((Payload) annotation).value();
					if (!StringUtils.hasText(expression)) {
						messageOrPayload = argumentValue;
					}
					else {
						messageOrPayload = this.evaluatePayloadExpression(expression, argumentValue);
					}
					foundPayloadAnnotation = true;
				}
				else if (annotation.annotationType().equals(Header.class)) {
					Header headerAnnotation = (Header) annotation;
					String headerName = this.determineHeaderName(headerAnnotation, methodParameter);
					if (headerAnnotation.required() && argumentValue == null) {
						throw new IllegalArgumentException("Received null argument value for required header: '" + headerName + "'");
					}
					headers.put(headerName, argumentValue);
				}
				else if (annotation.annotationType().equals(Headers.class)) {
					if (argumentValue != null) {
						if (!(argumentValue instanceof Map)) {
							throw new IllegalArgumentException("@Headers annotation is only valid for Map-typed parameters");
						}
						for (Object key : ((Map) argumentValue).keySet()) {	
							Assert.isInstanceOf(String.class, key, "Invalid header name [" + key +
									"], name type must be String.");
							Object value = ((Map) argumentValue).get(key);
							headers.put((String) key, value);
						}
					}
				}
			}
			else if (messageOrPayload == null) {
				messageOrPayload = argumentValue;
			}
			else if (Map.class.isAssignableFrom(methodParameter.getParameterType())) {
				if (messageOrPayload instanceof Map && !foundPayloadAnnotation) {
					throw new MessagingException("Ambiguous method parameters; found more than one " +
							"Map-typed parameter and neither one contains a @Payload annotation");
				}
				this.copyHeaders((Map) argumentValue, headers);
			}
			else if (this.payloadExpression == null) {
				this.throwExceptionForMultipleMessageOrPayloadParameters(methodParameter);
			}
		}
		Assert.isTrue(messageOrPayload != null, "unable to determine a Message or payload parameter on method [" + method + "]");
		MessageBuilder builder = (messageOrPayload instanceof Message)
				? MessageBuilder.fromMessage((Message<?>) messageOrPayload)
				: MessageBuilder.withPayload(messageOrPayload);
		builder.copyHeadersIfAbsent(headers);
		if (!CollectionUtils.isEmpty(staticHeaders)){
			builder.copyHeaders(staticHeaders);
		}
		return builder.build();
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

	@SuppressWarnings("unchecked")
	private void copyHeaders(Map argumentValue, Map<String, Object> headers) {
		for (Object key : argumentValue.keySet()) {	
			if (!(key instanceof String)) {
				throw new IllegalArgumentException("Invalid header name [" + key +
					"], name type must be String.");
			}
			Object value = argumentValue.get(key);
			headers.put((String) key, value);
		}
	}

	private void throwExceptionForMultipleMessageOrPayloadParameters(MethodParameter methodParameter) {
		throw new MessagingException(
				"At most one parameter (or expression via method-level @Payload) may be mapped to the " +
				"payload or Message. Found more than one on method [" + methodParameter.getMethod() + "]");
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
