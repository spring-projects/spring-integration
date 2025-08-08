/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
