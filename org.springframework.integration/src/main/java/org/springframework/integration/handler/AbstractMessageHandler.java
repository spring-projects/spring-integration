/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.annotation.MethodArgumentMessageMapper;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.util.DefaultMethodInvoker;
import org.springframework.integration.util.MethodInvoker;
import org.springframework.integration.util.NameResolvingMethodInvoker;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A base class for any {@link MessageHandler} that may act as an adapter
 * by invoking a "plain" (not Message-aware) method for a given target object.
 * When used as an adapter, the target Object is mandatory and either a
 * {@link Method} reference or a 'methodName' must be provided. If no Object
 * and Method are provided, the handler will simply process the request
 * Message's payload.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageHandler implements MessageHandler, Ordered, InitializingBean {

	protected static final Log logger = LogFactory.getLog(AbstractMessageHandler.class);

	private volatile boolean methodExpectsMessage;

	private volatile Object object;

	private volatile Method method;

	private volatile String methodName;

	private volatile MessageMapper<Object, Object[]> methodArgumentMapper;

	private volatile MethodInvoker invoker;

	private volatile int order = Ordered.LOWEST_PRECEDENCE;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public AbstractMessageHandler(Object object, Method method) {
		Assert.notNull(object, "object must not be null");
		this.object = object;
		this.setMethod(method);
	}

	public AbstractMessageHandler(Object object, String methodName) {
		Assert.notNull(object, "object must not be null");
		this.object = object;
		this.setMethodName(methodName);
	}

	public AbstractMessageHandler() {
	}


	public void setObject(Object object) {
		this.object = object;
	}

	public void setMethod(Method method) {
		Assert.notNull(method, "method must not be null");
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length == 0) {
			throw new ConfigurationException("method must accept at least one parameter");
		}
		if (parameterTypes.length == 1 && Message.class.isAssignableFrom(parameterTypes[0])) {
			this.methodExpectsMessage = true;
		}
		this.method = method;
		this.methodName = method.getName();
	}

	public void setMethodName(String methodName) {
		Assert.notNull(methodName, "methodName must not be null");
		if (this.method != null && !this.method.getName().equals(methodName)) {
			this.method = null;
		}
		this.methodName = methodName;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.object != null) {
				if (this.method == null) {
					if (this.methodName == null) {
						throw new IllegalStateException("either the 'method' or 'methodName' must be provided when the 'object' property has been set");
					}
					final List<Method> candidates = new ArrayList<Method>();
					ReflectionUtils.doWithMethods(this.object.getClass(), new ReflectionUtils.MethodCallback() {
						public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
							if (method.getName().equals(AbstractMessageHandler.this.methodName)) {
								candidates.add(method);
							}
						}
					});
					if (candidates.size() == 0) {
						throw new ConfigurationException("no such method '" + this.methodName
								+ "' on target class [" + this.object.getClass() + "]"); 
					}
					if (candidates.size() == 1) {
						this.setMethod(candidates.get(0));
					}
				}
				if (this.method != null) {
					this.invoker = new DefaultMethodInvoker(this.object, this.method);
					this.methodArgumentMapper = new MethodArgumentMessageMapper(this.method);
				}
				else {
					// TODO: resolve the candidate method and/or create a dynamic resolver
					this.invoker = new NameResolvingMethodInvoker(this.object, this.methodName);
				}
			}
			this.initialized = true;
		}
	}

	public Message<?> handle(Message<?> requestMessage) {
		if (requestMessage == null || requestMessage.getPayload() == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("message handler received a null message");
			}
			return null;
		}
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Object result = (this.invoker != null) ?
				this.invokeHandlerMethod(requestMessage) : requestMessage.getPayload();
		if (result == null) {
			return null;
		}
		if (result instanceof Message) {
			return this.postProcessReplyMessage((Message<?>) result, requestMessage);
		}
		return this.createReplyMessage(result, requestMessage);
	}

	/**
	 * Subclasses must implement this method to process a return value that
	 * is already a Message instance.
	 * 
	 * @param replyMessage the Message returned from an adapter method
	 * @param requestMessage the original request Message
	 * @return the Message to be sent to the reply MessageTarget
	 */
	protected abstract Message<?> postProcessReplyMessage(Message<?> replyMessage, Message<?> requestMessage);

	/**
	 * Subclasses must implement this method to generate the reply Message when
	 * the return value is not a Message instance.
	 * 
	 * @param result the return value from an adapter method, or the Message payload if not acting as an adapter
	 * @param requestMessage the original request Message
	 * @return the Message to be sent to the reply MessageTarget
	 */
	protected abstract Message<?> createReplyMessage(Object result, Message<?> requestMessage);


	private Object invokeHandlerMethod(Message<?> message) {
		Object args[] = null;
		Object mappingResult = this.methodExpectsMessage ? message
				: this.mapMessageToMethodArguments(message);
		if (mappingResult != null && mappingResult.getClass().isArray()
				&& (Object.class.isAssignableFrom(mappingResult.getClass().getComponentType()))) {
			args = (Object[]) mappingResult;
		}
		else {
			args = new Object[] { mappingResult }; 
		}
		try {
			Object result = null;
			try {
				result = this.invoker.invokeMethod(args);
			}
			catch (NoSuchMethodException e) {
				try {
					result = this.invoker.invokeMethod(message);
					this.methodExpectsMessage = true;
				}
				catch (NoSuchMethodException e2) {
					throw new MessageHandlingException(message, "unable to resolve method for args: "
							+ StringUtils.arrayToCommaDelimitedString(args));
				}
			}
			if (result == null) {
				return null;
			}
			return result;
		}
		catch (InvocationTargetException e) {
			throw new MessageHandlingException(message, "Handler method '"
					+ this.methodName + "' threw an Exception.", e.getTargetException());
		}
		catch (Throwable e) {
			throw new MessageHandlingException(message, "Failed to invoke handler method '"
					+ this.methodName + "' with arguments: " + ObjectUtils.nullSafeToString(args), e);
		}
	}

	private Object[] mapMessageToMethodArguments(Message message) {
		if (this.methodArgumentMapper != null) {
			return this.methodArgumentMapper.mapMessage(message);
		}
		return new Object[] { message.getPayload() };
	}

}
