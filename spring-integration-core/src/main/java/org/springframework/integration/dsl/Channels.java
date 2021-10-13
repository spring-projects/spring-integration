/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Queue;
import java.util.concurrent.Executor;

import org.springframework.integration.store.ChannelMessageStore;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.messaging.Message;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public final class Channels {

	static final Channels INSTANCE = new Channels();

	public DirectChannelSpec direct() {
		return MessageChannels.direct();
	}

	public DirectChannelSpec direct(String id) {
		return MessageChannels.direct(id);
	}

	public QueueChannelSpec queue() {
		return MessageChannels.queue();
	}

	public QueueChannelSpec queue(String id) {
		return MessageChannels.queue(id);
	}

	public QueueChannelSpec queue(Integer capacity) {
		return MessageChannels.queue(capacity);
	}

	public QueueChannelSpec queue(String id, Integer capacity) {
		return MessageChannels.queue(id, capacity);
	}

	public QueueChannelSpec queue(Queue<Message<?>> queue) {
		return MessageChannels.queue(queue);
	}

	public QueueChannelSpec queue(String id, Queue<Message<?>> queue) {
		return MessageChannels.queue(id, queue);
	}

	public QueueChannelSpec.MessageStoreSpec queue(ChannelMessageStore messageGroupStore, Object groupId) {
		return MessageChannels.queue(messageGroupStore, groupId);
	}

	public QueueChannelSpec.MessageStoreSpec queue(String id, ChannelMessageStore messageGroupStore, Object groupId) {
		return MessageChannels.queue(id, messageGroupStore, groupId);
	}

	public PriorityChannelSpec priority() {
		return MessageChannels.priority();
	}

	public PriorityChannelSpec priority(String id) {
		return MessageChannels.priority(id);
	}

	public PriorityChannelSpec priority(String id, PriorityCapableChannelMessageStore messageGroupStore,
			Object groupId) {
		return MessageChannels.priority(id, messageGroupStore, groupId);
	}

	public RendezvousChannelSpec rendezvous() {
		return MessageChannels.rendezvous();
	}

	public PriorityChannelSpec priority(PriorityCapableChannelMessageStore messageGroupStore, Object groupId) {
		return MessageChannels.priority(messageGroupStore, groupId);
	}

	public RendezvousChannelSpec rendezvous(String id) {
		return MessageChannels.rendezvous(id);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe() {
		return MessageChannels.publishSubscribe();
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(boolean requireSubscribers) {
		return MessageChannels.publishSubscribe(requireSubscribers);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(Executor executor) {
		return MessageChannels.publishSubscribe(executor);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(Executor executor, boolean requireSubscribers) {
		return MessageChannels.publishSubscribe(executor, requireSubscribers);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(String id, Executor executor) {
		return MessageChannels.publishSubscribe(id, executor);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(String id, Executor executor, boolean requireSubscribers) {
		return MessageChannels.publishSubscribe(id, executor, requireSubscribers);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(String id) {
		return MessageChannels.publishSubscribe(id);
	}

	public PublishSubscribeChannelSpec<?> publishSubscribe(String id, boolean requireSubscribers) {
		return MessageChannels.publishSubscribe(id, requireSubscribers);
	}

	public ExecutorChannelSpec executor(Executor executor) {
		return MessageChannels.executor(executor);
	}

	public ExecutorChannelSpec executor(String id, Executor executor) {
		return MessageChannels.executor(id, executor);
	}


	public FluxMessageChannelSpec flux() {
		return MessageChannels.flux();
	}

	public FluxMessageChannelSpec flux(String id) {
		return MessageChannels.flux(id);
	}

	private Channels() {
	}

}
