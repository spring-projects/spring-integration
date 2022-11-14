/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import reactor.core.publisher.Mono;

import org.springframework.integration.annotation.Gateway;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public interface TestService {

	String requestReply(String input);

	byte[] requestReplyInBytes(String input);

	void oneWay(String input);

	@Payload("args[0]")
	void oneWayWithTimeouts(String input, Long sendTimeout, Long receiveTimeout);

	String solicitResponse();

	Message<String> getMessage();

	Integer requestReplyWithIntegers(Integer input);

	String requestReplyWithMessageParameter(Message<?> message);

	Message<?> requestReplyWithMessageReturnValue(String input);

	@Payload("method.name + args.length")
	String requestReplyWithPayloadAnnotation();

	Future<Message<?>> async(String s);

	Mono<Message<?>> promise(String s);

	CompletableFuture<String> completable(String s);

	MyCompletableFuture customCompletable(String s);

	CompletableFuture<Message<?>> completableReturnsMessage(String s);

	MyCompletableMessageFuture customCompletableReturnsMessage(String s);

	@Gateway(requestChannel = "errorChannel")
	default void defaultMethodGateway(Object payload) {
		throw new UnsupportedOperationException();
	}

	@Gateway(payloadExpression = "'fromGwExpression'", requestChannel = "requestChannel")
	String noArgWithGateway();

	@Payload("'fromPayloadAnnExpression'")
	@Gateway(payloadExpression = "'fromGwExpression'", requestChannel = "requestChannel")
	String noArgWithGatewayAndPayload();

	class MyCompletableFuture extends CompletableFuture<String> {

	}

	class MyCompletableMessageFuture extends CompletableFuture<Message<?>> {

	}

}
