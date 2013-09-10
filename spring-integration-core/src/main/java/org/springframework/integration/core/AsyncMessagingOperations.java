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

package org.springframework.integration.core;

import java.util.concurrent.Future;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.MessagePostProcessor;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public interface AsyncMessagingOperations {

	Future<?> asyncSend(Message<?> message);

	Future<?> asyncSend(MessageChannel channel, Message<?> message);

	Future<?> asyncSend(String channelName, Message<?> message);

	Future<?> asyncConvertAndSend(Object message);

	Future<?> asyncConvertAndSend(MessageChannel channel, Object message);

	Future<?> asyncConvertAndSend(String channelName, Object message);

	Future<Message<?>> asyncReceive();

	Future<Message<?>> asyncReceive(PollableChannel channel);

	Future<Message<?>> asyncReceive(String channelName);

	<R> Future<R> asyncReceiveAndConvert();

	<R> Future<R> asyncReceiveAndConvert(PollableChannel channel);

	<R> Future<R> asyncReceiveAndConvert(String channelName);

	Future<Message<?>> asyncSendAndReceive(Message<?> requestMessage);

	Future<Message<?>> asyncSendAndReceive(MessageChannel channel, Message<?> requestMessage);

	Future<Message<?>> asyncSendAndReceive(String channelName, Message<?> requestMessage);

	<R> Future<R> asyncConvertSendAndReceive(Object request);

	<R> Future<R> asyncConvertSendAndReceive(MessageChannel channel, Object request);

	<R> Future<R> asyncConvertSendAndReceive(String channelName, Object request);

	<R> Future<R> asyncConvertSendAndReceive(Object request, MessagePostProcessor requestPostProcessor);

	<R> Future<R> asyncConvertSendAndReceive(MessageChannel channel, Object request, MessagePostProcessor requestPostProcessor);

	<R> Future<R> asyncConvertSendAndReceive(String channelName, Object request, MessagePostProcessor requestPostProcessor);

}
