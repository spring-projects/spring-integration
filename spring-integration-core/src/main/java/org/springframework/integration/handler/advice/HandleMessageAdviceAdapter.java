/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.handler.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.util.Assert;

/**
 * A {@link HandleMessageAdvice} implementation with a plain delegation
 * to the provided {@link MethodInterceptor}.
 * <p> This advice should be used for consumer endpoints to proxy exactly
 * a {@link org.springframework.messaging.MessageHandler#handleMessage} and the whole-subflow therefore;
 * unlike regular proxying which is applied only for the
 * {@link org.springframework.integration.handler.AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage}.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class HandleMessageAdviceAdapter implements HandleMessageAdvice {

	private final MethodInterceptor delegate;

	public HandleMessageAdviceAdapter(MethodInterceptor delegate) {
		Assert.notNull(delegate, "The 'delegate' must not be null");
		this.delegate = delegate;
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		return this.delegate.invoke(invocation);
	}

}
