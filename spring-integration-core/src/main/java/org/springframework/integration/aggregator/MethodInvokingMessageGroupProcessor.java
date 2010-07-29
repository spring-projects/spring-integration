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

package org.springframework.integration.aggregator;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.integration.Message;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;

/**
 * MessageGroupProcessor that serves as an adapter for the invocation of a POJO method.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Dave Syer
 * @since 2.0
 */
public class MethodInvokingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	private final MessageListProcessor adapter;

	/**
	 * Creates a wrapper around the object passed in. This constructor will look for a method that can process
	 * a list of messages.
	 * 
	 * @param target the object to wrap
	 */
	public MethodInvokingMessageGroupProcessor(Object target) {
		this.adapter = getAdapter(target, Aggregator.class);
		Assert.notNull(this.adapter, "No aggregator method could be found for object of type: "+target.getClass());
	}

	/**
	 * Creates a wrapper around the object passed in. This constructor will look for a named method specifically and
	 * fail when it cannot find a method with the given name.
	 * 
	 * @param target the object to wrap
	 * @param methodName the name of the method to invoke
	 */
	public MethodInvokingMessageGroupProcessor(Object target, String methodName) {
		this.adapter = new MethodInvokingMessageListProcessor(target, methodName);
	}

	/**
	 * Creates a wrapper around the object passed in.
	 * 
	 * @param target the object to wrap
	 * @param method the method to invoke
	 */
	public MethodInvokingMessageGroupProcessor(Object target, Method method) {
		this.adapter = new MethodInvokingMessageListProcessor(target, method);
	}

	@Override
	protected final Object aggregatePayloads(MessageGroup group) {
		final Collection<Message<?>> messagesUpForProcessing = group.getUnmarked();
		Object result = this.adapter.process(messagesUpForProcessing);
		return result;
	}

}
