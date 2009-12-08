/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.context.expression.MapAccessor;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.util.Assert;
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
 * @Header as necessary, but typically there should be only one parameter
 * expecting multiple headers (with or without the @Headers annotation).
 * <p/>
 * If a Map or Properties object is expected, and the payload is not itself
 * assignable to that type or capable of being converted to that type, then
 * the MessageHeaders' values will be passed in the case of a Map-typed
 * parameter, or the MessageHeaders' String-based values will be passed in the
 * case of a Properties-typed parameter. In these cases multiple unannotated
 * parameters are legal. If, however, the actual payload type is a Map or
 * Properties instance, then this ambiguity cannot be resolved. For that
 * reason, it is highly recommended to use the explicit
 * {@link Headers @Headers} annotation whenever possible.
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
public class ArgumentArrayMessageMapper implements InboundMessageMapper<Object[]>, OutboundMessageMapper<Object[]> {

	private static ConversionService conversionService;

	static { // see INT-829
		conversionService = ConversionServiceFactory.createDefaultConversionService();
		ConverterRegistry registry = (ConverterRegistry) conversionService;
		registry.removeConvertible(Object.class, Map.class);
		registry.removeConvertible(Map.class, Object.class);
		registry.removeConvertible(Object.class, String.class);
		registry.addConverter(new Converter<Number, String>() {
			public String convert(Number source) { return source.toString(); }
		});
		registry.addConverter(new Converter<Date, String>() {
			public String convert(Date source) { return source.toString(); }
		});
	}


	private final ExpressionParser expressionParser = new SpelExpressionParser();

	private final Method method;

	private final List<MethodParameter> parameterList;


	public ArgumentArrayMessageMapper(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.parameterList = this.getMethodParameterList(method);
	}


	public Object[] fromMessage(Message<?> message) {
		Assert.notNull(message, "cannot map a null Message");
		this.validateMessageMapppings(message);
		return this.mapMessageToArguments(message);
	}

	public Message<?> toMessage(Object[] arguments) {
		Assert.notNull(arguments, "cannot map null arguments to Message");
		if (arguments.length != this.parameterList.size()) {
			String prefix = (arguments.length < this.parameterList.size()) ? "Not enough" : "Too many";
			throw new IllegalArgumentException(prefix + " parameters provided for method [" + method +
					"], expected " + this.parameterList.size() + " but received " + arguments.length + ".");
		}
		Map<String, Object> messageArgumentsMap = this.mapArgumentsToMessage(arguments, null);
		return this.buildMessageFromArgumentMap(messageArgumentsMap);
	}

	private Object[] mapMessageToArguments(Message<?> message) {
		final Map<String, Object> messageArgumentsMap = new LinkedHashMap<String, Object>();
		for (MethodParameter methodParameter : this.parameterList) {
			String parameterName = methodParameter.getParameterName();
			String[] expressions = null;
			Annotation mappingAnnotation = null;
			Object value = null;
			mappingAnnotation = this.findMappingAnnotation(methodParameter.getParameterAnnotations());
			if (mappingAnnotation == null) {
				if ("headers".equals(parameterName) || "payload".equals(parameterName)) {
					expressions = new String[] { parameterName };
				}
				else if ("message".equals(parameterName)) {
					// just in case 'parameterName' is 'message' but type is not Message
					expressions = new String[] { "#this", "payload" };
				}
				else {
					expressions = new String[] { "payload." + parameterName, "headers." + parameterName, "payload", "headers", "#this" };
				}
				value = this.getValueFromMessageBasedOnEL(message, methodParameter.getParameterType(), false, expressions);
			}
			else {
				if (mappingAnnotation.annotationType().isAssignableFrom(Header.class)) {
					value = this.mapHeaderThruAnnotation(mappingAnnotation, message, methodParameter, null)[1];
				}
				else if (mappingAnnotation.annotationType().isAssignableFrom(Headers.class)) {
					expressions = new String[]{"headers"};
					value = this.getValueFromMessageBasedOnEL(message, methodParameter.getParameterType(), false, expressions);
				}
				else if (mappingAnnotation.annotationType().isAssignableFrom(Payload.class)) {
					String payloadExpression = ((Payload) mappingAnnotation).value();
					if (payloadExpression.length() == 0) {
						expressions = new String[] { "payload" };
					}
					else {
						expressions = new String[] { "payload." + payloadExpression };
					}
					value = this.getValueFromMessageBasedOnEL(message, methodParameter.getParameterType(), true, expressions);
				}
				else {
					throw new IllegalArgumentException("unsupported mapping annotation: " + mappingAnnotation);
				}
			}
			messageArgumentsMap.put(methodParameter.getParameterIndex() + ":" + parameterName, value);		
		}
		return messageArgumentsMap.values().toArray();
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
	private Map<String, Object> mapArgumentsToMessage(Object[] arguments, Message<?> message) {
		MethodParameter messageOrPayloadParameter = null;
		Map<String, Object> messageArgumentsMap = new LinkedHashMap<String, Object>();
		for (int i = 0; i < this.parameterList.size(); i++) {
			Object argumentValue = arguments[i];		
			MethodParameter methodParameter = (MethodParameter) this.parameterList.get(i);
			Annotation annotation = this.findMappingAnnotation(methodParameter.getParameterAnnotations());
			if (annotation != null) {
				if (annotation.annotationType().equals(Payload.class)) {
					if (((Payload) annotation).value().length() != 0) {
						throw new MessagingException(
								"The Payload annotation does not support an expression when mapping to a Message.");
					}
					messageArgumentsMap.put("payload", argumentValue);
					if (messageOrPayloadParameter != null) {
						this.throwExceptionForMultipleMessageOrPayloadParameters(methodParameter);
					}
					messageOrPayloadParameter = methodParameter;
				}
				else if (annotation.annotationType().equals(Header.class)) {
					Object[] header = this.mapHeaderThruAnnotation(annotation, message, methodParameter, argumentValue);
					messageArgumentsMap.put((String) header[0], header[1]);
				}
				else if (annotation.annotationType().equals(Headers.class)) {
					if (argumentValue != null) {
						messageArgumentsMap.putAll(((Map) argumentValue));
						for (Object key : ((Map) argumentValue).keySet()) {	
							Assert.isInstanceOf(String.class, key, "Invalid header name [" + key +
									"], name type must be String.");
							Object value = ((Map) argumentValue).get(key);
							messageArgumentsMap.put((String) key, value);
						}
					}
				}
			}
			else if (messageOrPayloadParameter == null) {
				if (argumentValue instanceof Message<?>) {
					messageArgumentsMap.put("message", argumentValue);
				}
				else {
					messageArgumentsMap.put("payload", argumentValue);				
				}
				messageOrPayloadParameter = methodParameter;
			}
			else if (Map.class.isAssignableFrom(methodParameter.getParameterType())) {
				if (Map.class.isAssignableFrom(messageOrPayloadParameter.getParameterType())
						&& messageOrPayloadParameter.getParameterAnnotation(Payload.class) == null) {
					throw new MessagingException("Ambiguous method parameters; found more than one " +
							"Map-typed parameter and neither one contains a @Payload annotation");
				}
				messageArgumentsMap.put("headers", argumentValue);
			}
			else {
				this.throwExceptionForMultipleMessageOrPayloadParameters(methodParameter);
			}
		}
		Assert.isTrue(messageOrPayloadParameter != null, "unable to determine a Message or payload parameter on method [" + method + "]");
		return messageArgumentsMap;
	}

	private void throwExceptionForMultipleMessageOrPayloadParameters(MethodParameter methodParameter) {
		throw new MessagingException(
				"At most one parameter may be mapped to the payload or Message, " +
				"found more than one on method [" + methodParameter.getMethod() + "]");
	}

	private Message<?> buildMessageFromArgumentMap(Map<String, Object> messageArgumentsMap) {
		MessageBuilder<?> builder = null;
		Message<?> message = (Message<?>) messageArgumentsMap.get("message");
		if (message != null) {
			builder = MessageBuilder.fromMessage(message);
		}
		else {
			builder = MessageBuilder.withPayload(messageArgumentsMap.get("payload"));
		}
		Object headers = messageArgumentsMap.get("headers");
		if (headers != null && headers instanceof Map) {
			builder.copyHeadersIfAbsent((Map<String, Object>) headers);
		}
		for (Object headerName : messageArgumentsMap.keySet()) {
			if (!headerName.equals("payload") && !headerName.equals("message") && !headerName.equals("headers")) {
				// everything else is a header
				builder.setHeader((String) headerName, messageArgumentsMap.get(headerName));
			}
		}
		return builder.build();	
	}

	/**
	 * @param header
	 * @param message
	 * @param methodParameter
	 * @param headerValue = will be present when mapping from Arg to Message and will be null the other way
	 * @return
	 */
	private Object[] mapHeaderThruAnnotation(Annotation header, Message<?> message, MethodParameter methodParameter, Object headerValue) {
		String valueAttribute = (String) AnnotationUtils.getValue(header);
		String headerName = StringUtils.hasText(valueAttribute) ? valueAttribute : methodParameter.getParameterName();
		Assert.notNull(headerName, "Cannot determine header name. Possible reasons: -debug is " +
				"disabled or header name is not explicitly provided via @Header annotation.");
		if (message != null) {
			headerValue = this.getValueFromMessageBasedOnEL(message, methodParameter.getParameterType(), false, "headers." + headerName);
		}
		this.evaluateHeader(header, headerName, headerValue, message);
		return new Object[]{headerName, headerValue};
	}

	private void evaluateHeader(Annotation headerAnnotation, String headerName, Object headerValue, Message<?> message) {
		boolean required = ((Boolean) AnnotationUtils.getAnnotationAttributes(headerAnnotation).get("required")).booleanValue();	
		if (required && headerValue == null) {
			if (message != null) {
				throw new MessageHandlingException(message, "Message is missing required header: '" + headerName + "'");
			}
			throw new IllegalArgumentException("Argument is missing required header: '" + headerName + "'");
		}
	}

	@SuppressWarnings("unchecked") 
	private Object getValueFromMessageBasedOnEL(Message message, Class targetType, boolean rethrowException, String... expressions) {
		Object value = null;
		targetType = new TypeDescriptor(targetType).getObjectType(); //converts primitives to Object wrappers
		for (String expression : expressions) {
			StandardEvaluationContext context = new StandardEvaluationContext(message);
			context.setTypeConverter(new StandardTypeConverter(conversionService));
			try { 
				Expression exp = expressionParser.parseExpression(expression); 
				context.addPropertyAccessor(new MapAccessor());	
				value = exp.getValue(context);
				if (value != null && this.canConvertToType(value, targetType)) {
					// to accommodate Map->Properties conversion
					value = expression.equals("headers") ? conversionService.convert(value, targetType) : value;
					break;
				}
				else {
					value = null;
				}
			}
			catch (Throwable e) {
				if (rethrowException) {
					throw new MessageHandlingException(message, e);
				}
			}
		}
		return value;
	}

	private List<MethodParameter> getMethodParameterList(Method method) {
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

	@SuppressWarnings("unchecked")
	private boolean canConvertToType(Object value, Class targetType) {
		return conversionService.canConvert(value.getClass(), targetType) ||
			   (value instanceof String && ((String)value).indexOf("=") > 0); // to address payload String "foo=bar" 
																			  // mapping to Properties/Map
	}

	@SuppressWarnings("unchecked")
	public void validateMessageMapppings(Message<?> message) {
		// Validate against a Map with no annotations
		if (message.getPayload() instanceof Map) {
			boolean foundOneMatch = false;
			for (MethodParameter parameter : this.parameterList) {
				String name = parameter.getParameterName();
				Class<?> type = parameter.getParameterType();
				if (parameter.getParameterAnnotations().length == 0 &&
						!(name.equals("payload") || name.equals("headers")) &&
						(type.isAssignableFrom(Properties.class) || type.isAssignableFrom(Map.class))) {
					if (foundOneMatch) {
						throw new IllegalArgumentException("Ambiguous parameters. " +
								"Cannot determine parameter mappings between Method: [" + method + "] and Message: " + message +
								". Try annotating individual parameters with @Payload, @Header, or @Headers");
					}
					foundOneMatch = true;
				}
			}
		}
	}

}
