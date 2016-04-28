/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;

import reactor.core.publisher.Mono;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public interface TestService {

	String requestReply(String input);

	byte[] requestReplyInBytes(String input);

	void oneWay(String input);

	String solicitResponse();

	Message<String> getMessage();

	Integer requestReplyWithIntegers(Integer input);

	String requestReplyWithMessageParameter(Message<?> message);

	Message<?> requestReplyWithMessageReturnValue(String input);

	@Payload("#gatewayMethod.name + #args.length")
	String requestReplyWithPayloadAnnotation();

	Future<Message<?>> async(String s);

	Mono<Message<?>> promise(String s);

	CompletableFuture<String> completable(String s);

	MyCompletableFuture customCompletable(String s);

	CompletableFuture<Message<?>> completableReturnsMessage(String s);

	MyCompletableMessageFuture customCompletableReturnsMessage(String s);

	class MyCompletableFuture extends CompletableFuture<String> {

	}

	class MyCompletableMessageFuture extends CompletableFuture<Message<?>> {

	}

}
