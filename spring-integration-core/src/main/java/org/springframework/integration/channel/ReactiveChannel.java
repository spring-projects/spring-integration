/*
 * Copyright 2016 the original author or authors.
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
import org.reactivestreams.Subscription;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

import reactor.core.flow.Cancellation;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.subscriber.BaseSubscriber;
import reactor.core.subscriber.SignalEmitter;

/**
 * @author Artem Bilan
 * @since 5.0
 */
public class ReactiveChannel implements MessageChannel, Publisher<Message<?>> {

	private final Processor<Message<?>, Message<?>> processor;

	private final SignalEmitter<Message<?>> emitter;

	public ReactiveChannel() {
		this(EmitterProcessor.async(SyncScheduler.INSTANCE));
	}

	public ReactiveChannel(Processor<Message<?>, Message<?>> processor) {
		Assert.notNull(processor, "'processor' must not be null");
		this.processor = processor;
		this.emitter = SignalEmitter.create(processor);
	}

	Subscriber<Message<?>> asSubscriber() {
		return new BaseSubscriber<Message<?>>() {

			@Override
			public void onSubscribe(Subscription subscription) {
				Assert.notNull(subscription, "'subscription' must not be null");
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Message<?> message) {
				send(message);
			}

		};
	}

	@Override
	public boolean send(Message<?> message) {
		return send(message, -1);
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		return this.emitter.submit(message, timeout) > -1;
	}

	@Override
	public void subscribe(Subscriber<? super Message<?>> subscriber) {
		this.processor.subscribe(subscriber);
	}


	private static final class SyncScheduler implements Scheduler {

		private final static Scheduler INSTANCE = new SyncScheduler();

		private final Worker worker = new Worker() {

			@Override
			public Cancellation schedule(Runnable task) {
				task.run();
				return () -> {
				};
			}

			@Override
			public void shutdown() {

			}

		};

		@Override
		public Cancellation schedule(Runnable task) {
			return this.worker.schedule(task);
		}

		@Override
		public Worker createWorker() {
			return this.worker;
		}

	}

}
