/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import reactor.core.subscription.ReactiveSession;

/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveEndpoint extends AbstractEndpoint {

	private final Publisher<Message<?>> inputChannel;

	private final Subscriber<Message<?>> subscriber;

	private ReactiveSession<Message<?>> reactiveSession;

	public ReactiveEndpoint(Publisher<Message<?>> inputChannel, Subscriber<Message<?>> subscriber) {
		Assert.isInstanceOf(Publisher.class, inputChannel,
				"The 'inputChannel', must implement org.reactivestreams.Publisher.");
		Assert.notNull(subscriber);
		this.inputChannel = inputChannel;
		this.subscriber = subscriber;
	}

	@Override
	protected void doStart() {
		this.reactiveSession = ReactiveSession.create(this.subscriber);
		this.inputChannel.subscribe(this.reactiveSession);
	}

	@Override
	protected void doStop() {
		this.reactiveSession.finish();
	}

}
