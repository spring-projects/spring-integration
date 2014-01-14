/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.handler.advice;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Base class for {@link MessageHandler} advice classes. Subclasses should provide
 * an implementation for {@link #doInvoke(ExecutionCallback, Object, Message)}.
 * Used to advise the handleRequestMessage method for {@link AbstractReplyProducingMessageHandler} or
 * {@link MessageHandler#handleMessage(Message)} for other message handlers.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 */
public abstract class AbstractRequestHandlerAdvice extends IntegrationObjectSupport
		implements MethodInterceptor {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@Override
	public final Object invoke(final MethodInvocation invocation) throws Throwable {

		Method method = invocation.getMethod();
		Object[] arguments = invocation.getArguments();
		boolean isMessageMethod = (method.getName().equals("handleRequestMessage") || method.getName().equals("handleMessage"))
				&& (arguments.length == 1 && arguments[0] instanceof Message);

		Object invocationThis = invocation.getThis();
		if (!isMessageMethod) {
			boolean isMessageHandler = invocationThis != null
					&& MessageHandler.class.isAssignableFrom(invocationThis.getClass());
			if (!isMessageHandler && logger.isWarnEnabled()) {
				String clazzName = invocationThis == null ? method.getDeclaringClass().getName() : invocationThis.getClass().getName();
				logger.warn("This advice " + this.getClass().getName() +
						" can only be used for MessageHandlers; an attempt to advise method '" + method.getName() +
						"' in '" + clazzName + "' is ignored");
			}
			return invocation.proceed();
		}
		else {
			Message<?> message = (Message<?>) arguments[0];
			try {
				return doInvoke(new ExecutionCallback() {

					@Override
					public Object execute() throws Exception {
						try {
							return invocation.proceed();
						}
						catch (Exception e) {
							throw e;
						}
						catch (Throwable e) {
							throw new ThrowableHolderException(e);
						}
					}

					@Override
					public Object cloneAndExecute() throws Exception {
						try {
							/*
				 			* If we don't copy the invocation carefully it won't keep a reference to the other
				 			* interceptors in the chain.
				 			*/
							if (invocation instanceof ProxyMethodInvocation) {
								return ((ProxyMethodInvocation) invocation).invocableClone().proceed();
							}
							else {
								throw new IllegalStateException(
										"MethodInvocation of the wrong type detected - this should not happen with Spring AOP," +
												" so please raise an issue if you see this exception");
							}
						}
						catch (Exception e) {
							throw e;
						}
						catch (Throwable e) {
							throw new ThrowableHolderException(e);
						}
					}
				}, invocationThis, message);
			}
			catch (Exception e) {
				throw this.unwrapThrowableIfNecessary(e);
			}
		}
	}

	/**
	 * Subclasses implement this method to apply behavior to the {@link MessageHandler}.
	 * <p>
	 * callback.execute() invokes the handler method and returns its result, or null.
	 *
	 * @param callback Subclasses invoke the execute() method on this interface to invoke the handler method.
	 * @param target   The target handler.
	 * @param message  The message that will be sent to the handler.
	 * @return the result after invoking the {@link MessageHandler}.
	 * @throws Exception Any Exception.
	 */
	protected abstract Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception;

	/**
	 * Unwrap the cause of a {@link AbstractRequestHandlerAdvice.ThrowableHolderException}.
	 * @param e The exception.
	 * @return The cause, or e, if not a {@link AbstractRequestHandlerAdvice.ThrowableHolderException}
	 */
	protected Exception unwrapExceptionIfNecessary(Exception e) {
		Exception actualException = e;
		if (e instanceof ThrowableHolderException) {
			if (e.getCause() instanceof Exception) {
				actualException = (Exception) e.getCause();
			}
		}
		return actualException;
	}

	/**
	 * Unwrap the cause of a {@link AbstractRequestHandlerAdvice.ThrowableHolderException}.
	 * @param e The exception.
	 * @return The cause, or e, if not a {@link AbstractRequestHandlerAdvice.ThrowableHolderException}
	 */
	protected Throwable unwrapThrowableIfNecessary(Exception e) {
		Throwable actualThrowable = e;
		if (e instanceof ThrowableHolderException) {
			actualThrowable = e.getCause();
		}
		return actualThrowable;
	}

	/**
	 * Called by subclasses in doInvoke() to proceed() the invocation. Callers
	 * unwrap {@link AbstractRequestHandlerAdvice.ThrowableHolderException}s and use
	 * the cause for evaluation and re-throwing purposes.
	 * See {@link AbstractRequestHandlerAdvice#unwrapExceptionIfNecessary(Exception)}.
	 */
	protected interface ExecutionCallback {

		/**
		 * Call this for a normal invocation.proceed().
		 *
		 * @return The result of the execution.
		 * @throws Exception Any Exception.
		 */
		Object execute() throws Exception;

		/**
		 * Call this when it is necessary to clone the invocation before
		 * calling proceed() - such as when the invocation might be called
		 * multiple times - for example in a retry advice.
		 *
		 * @return The result of the execution.
		 * @throws Exception Any Exception.
		 */
		Object cloneAndExecute() throws Exception;

	}

	@SuppressWarnings("serial")
	private class ThrowableHolderException extends RuntimeException {

		public ThrowableHolderException(Throwable cause) {
			super(cause);
		}

	}

}
