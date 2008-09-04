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

import org.springframework.integration.handler.MessageListMethodAdapter;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;

/**
 * Aggregator adapter for methods annotated with {@link org.springframework.integration.annotation.Aggregator @Aggregator}
 * and for '<code>aggregator</code>' elements that include a '<code>method</code>' attribute
 * (e.g. &lt;aggregator ref="beanReference" method="methodName"/&gt;).
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class MethodInvokingAggregator extends MessageListMethodAdapter implements Aggregator {

	public MethodInvokingAggregator(Object object, Method method) {
		super(object, method);
	}

	public MethodInvokingAggregator(Object object, String methodName) {
		super(object, methodName);
	}


	public Message<?> aggregate(List<Message<?>> messages) {
		Object returnedValue = this.executeMethod(messages);
		if (returnedValue == null) {
			return null;
		}
		if (returnedValue instanceof Message) {
			return (Message<?>) returnedValue;
		}
		return new GenericMessage<Object>(returnedValue);
	}

}
