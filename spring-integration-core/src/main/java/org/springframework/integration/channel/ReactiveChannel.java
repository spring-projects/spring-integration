/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import reactor.core.publisher.ProcessorGroup;
import reactor.core.subscriber.ReactiveSession;

/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveChannel implements MessageChannel, Publisher<Message<?>> {

	private final Processor<Message<?>, Message<?>> processor;

	private final ReactiveSession<Message<?>> reactiveSession;

	public ReactiveChannel() {
		this(ProcessorGroup.<Message<?>>sync().get());
	}

	public ReactiveChannel(Processor<Message<?>, Message<?>> processor) {
		this.processor = processor;
		this.reactiveSession = ReactiveSession.create(processor);
	}

	@Override
	public boolean send(Message<?> message) {
		return send(message, -1);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean send(Message<?> message, long timeout) {
		this.reactiveSession.submit(message, timeout);
		return true;
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.processor.subscribe(subscriber);
	}

}
