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

package org.springframework.integration.splitter;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;

// TODO: remove this class after refactoring the annotation support

/**
 * A {@link MessageHandler} implementation for splitting a single Message
 * into multiple reply Messages. If an object and method (or methodName)
 * pair are provided, the provided method will be invoked and its return
 * value will be split if it is a Collection or Array. If no object and
 * method are provided, this handler will split the Message payload
 * itself if it is a Collection or an Array. In either case, if the
 * Message payload or return value from a Method invocation is not a
 * Collection or Array, then the single Object will be returned as the
 * payload of a single reply Message.
 * 
 * @author Mark Fisher
 */
public class SplitterMessageHandler implements MessageHandler {

	private final Splitter splitter;


	public SplitterMessageHandler(Object object, Method method) {
		this.splitter = new MethodInvokingSplitter(object, method);
	}

	public SplitterMessageHandler(Object object, String methodName) {
		this.splitter = new MethodInvokingSplitter(object, methodName);
	}

	public SplitterMessageHandler() {
		this.splitter = new DefaultSplitter();
	}


	public Message<?> handle(Message<?> message) {
		List<Message<?>> results = this.splitter.split(message);
		if (results == null || results.isEmpty()) {
			return null;
		}
		return new CompositeMessage(results);
	}

}
