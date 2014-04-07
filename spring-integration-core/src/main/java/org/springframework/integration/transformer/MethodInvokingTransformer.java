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

package org.springframework.integration.transformer;

import java.lang.reflect.Method;

import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.util.Assert;

/**
 * A Message Transformer implementation that invokes the specified method
 * on the given object. The method's return value will be considered as
 * the payload of a new Message unless the return value is itself already
 * a Message.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingTransformer extends AbstractMessageProcessingTransformer {

	public MethodInvokingTransformer(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Object>(object, method));
		Assert.state(!Void.class.isAssignableFrom(method.getReturnType()), "'transformer' method must not be 'void'.");
	}

	public MethodInvokingTransformer(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Object>(object, methodName));
	}

	public MethodInvokingTransformer(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<?>) object :
				new MethodInvokingMessageProcessor<Object>(object, Transformer.class));
	}

}
