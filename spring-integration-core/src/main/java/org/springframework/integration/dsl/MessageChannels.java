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
public final class MessageChannels {

	public static DirectChannelSpec direct() {
		return new DirectChannelSpec();
	}

	public static DirectChannelSpec direct(String id) {
		return direct().id(id);
	}

	public static QueueChannelSpec queue() {
		return new QueueChannelSpec();
	}

	public static QueueChannelSpec queue(String id) {
		return queue().id(id);
	}

	public static QueueChannelSpec queue(Queue<Message<?>> queue) {
		return new QueueChannelSpec(queue);
	}

	public static QueueChannelSpec queue(String id, Queue<Message<?>> queue) {
		return queue(queue).id(id);
	}

	public static QueueChannelSpec queue(Integer capacity) {
		return new QueueChannelSpec(capacity);
	}

	public static QueueChannelSpec queue(String id, Integer capacity) {
		return queue(capacity).id(id);
	}

	public static QueueChannelSpec.MessageStoreSpec queue(ChannelMessageStore messageGroupStore, Object groupId) {
		return new QueueChannelSpec.MessageStoreSpec(messageGroupStore, groupId);
	}

	public static QueueChannelSpec.MessageStoreSpec queue(String id, ChannelMessageStore messageGroupStore,
			Object groupId) {

		return queue(messageGroupStore, groupId).id(id);
	}

	public static ExecutorChannelSpec executor(Executor executor) {
		return new ExecutorChannelSpec(executor);
	}

	public static ExecutorChannelSpec executor(String id, Executor executor) {
		return executor(executor).id(id);
	}

	public static RendezvousChannelSpec rendezvous() {
		return new RendezvousChannelSpec();
	}

	public static RendezvousChannelSpec rendezvous(String id) {
		return rendezvous().id(id);
	}

	public static PriorityChannelSpec priority() {
		return new PriorityChannelSpec();
	}

	public static PriorityChannelSpec priority(String id) {
		return priority().id(id);
	}

	public static PriorityChannelSpec priority(PriorityCapableChannelMessageStore messageGroupStore, Object groupId) {
		return priority().messageStore(messageGroupStore, groupId);
	}

	public static PriorityChannelSpec priority(String id,
			PriorityCapableChannelMessageStore messageGroupStore, Object groupId) {

		return priority(messageGroupStore, groupId).id(id);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe() {
		return publishSubscribe(false);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(boolean requireSubscribers) {
		return new PublishSubscribeChannelSpec<>(requireSubscribers);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(String id) {
		return publishSubscribe(id, false);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(String id, boolean requireSubscribers) {
		return publishSubscribe(requireSubscribers).id(id);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(Executor executor) {
		return publishSubscribe(executor, false);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(Executor executor, boolean requireSubscribers) {
		return new PublishSubscribeChannelSpec<>(executor, requireSubscribers);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(String id, Executor executor) {
		return publishSubscribe(id, executor, false);
	}

	public static PublishSubscribeChannelSpec<?> publishSubscribe(String id, Executor executor,
			boolean requireSubscribers) {

		return publishSubscribe(executor, requireSubscribers).id(id);
	}

	public static FluxMessageChannelSpec flux() {
		return new FluxMessageChannelSpec();
	}

	public static FluxMessageChannelSpec flux(String id) {
		return flux().id(id);
	}

	private MessageChannels() {
	}

}
