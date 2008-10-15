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

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.core.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.util.DefaultMethodResolver;
import org.springframework.integration.util.MethodResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link AbstractMessageAggregator} adapter for methods annotated with
 * {@link Aggregator @Aggregator} annotation and for <code>aggregator</code>
 * elements (e.g. &lt;aggregator ref="beanReference" method="methodName"/&gt;).
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class MethodInvokingAggregator extends AbstractMessageAggregator {

	private final MethodResolver methodResolver = new DefaultMethodResolver(Aggregator.class);

	private final MessageListMethodAdapter methodInvoker;


	public MethodInvokingAggregator(Object object, Method method) {
		this.methodInvoker = new MessageListMethodAdapter(object, method);
	}

	public MethodInvokingAggregator(Object object, String methodName) {
		this.methodInvoker = new MessageListMethodAdapter(object, methodName);
	}

	public MethodInvokingAggregator(Object object) {
		Assert.notNull(object, "object must not be null");
		Method method = this.methodResolver.findMethod(object); 
		Assert.notNull(method, "unable to resolve Aggregator method on target class ["
				+ object.getClass() + "]");
		this.methodInvoker = new MessageListMethodAdapter(object, method);
	}


	public Message<?> aggregateMessages(List<Message<?>> messages) {
		if (CollectionUtils.isEmpty(messages)) {
			return null;
		}
		Object returnedValue = this.methodInvoker.executeMethod(messages);
		if (returnedValue == null) {
			return null;
		}
		if (returnedValue instanceof Message) {
			return (Message<?>) returnedValue;
		}
		return new GenericMessage<Object>(returnedValue);
	}

}
