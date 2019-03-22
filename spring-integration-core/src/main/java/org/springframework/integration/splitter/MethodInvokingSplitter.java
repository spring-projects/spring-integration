/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.splitter;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;

/**
 * A Message Splitter implementation that invokes the specified method
 * on the given object. The method's return value will be split if it
 * is a Collection or Array. If the return value is not a Collection or
 * Array, then the single Object will be returned as the payload of a
 * single reply Message.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class MethodInvokingSplitter extends AbstractMessageProcessingSplitter {

	public MethodInvokingSplitter(Object object, Method method) {
		super(new MethodInvokingMessageProcessor<Collection<?>>(object, method));
	}

	public MethodInvokingSplitter(Object object, String methodName) {
		super(new MethodInvokingMessageProcessor<Collection<?>>(object, methodName));
	}

	@SuppressWarnings("unchecked")
	public MethodInvokingSplitter(Object object) {
		super(object instanceof MessageProcessor<?> ? (MessageProcessor<Collection<?>>) object :
				new MethodInvokingMessageProcessor<Collection<?>>(object, Splitter.class));
	}

}
