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

package org.springframework.integration.jdbc.channel;

import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

final class PostgresChannelMessageTableSubscription {

	private final Object groupId;

	private final String region;

	private final Runnable onPossibleUpdate;

	PostgresChannelMessageTableSubscription(Object groupId, String region, Runnable onPossibleUpdate) {
		this.groupId = groupId;
		this.region = region;
		this.onPossibleUpdate = onPossibleUpdate;
	}

	Object getGroupId() {
		return this.groupId;
	}

	String getRegion() {
		return this.region;
	}

	void onPossibleUpdate() {
		this.onPossibleUpdate.run();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PostgresChannelMessageTableSubscription that = (PostgresChannelMessageTableSubscription) o;
		if (!this.groupId.equals(that.groupId)) {
			return false;
		}
		if (!this.region.equals(that.region)) {
			return false;
		}
		return this.onPossibleUpdate.equals(that.onPossibleUpdate);
	}

	@Override
	public int hashCode() {
		int result = this.groupId.hashCode();
		result = 31 * result + this.region.hashCode();
		result = 31 * result + this.onPossibleUpdate.hashCode();
		return result;
	}

	static SubscribableChannel asSubscribableChannel(
			PostgresChannelMessageTableSubscriber subscriber,
			JdbcChannelMessageStore messageStore,
			Object groupId) {
		return new SubscribableChannel() {
			@Override
			public boolean subscribe(MessageHandler handler) {
				Assert.isTrue(subscriber.isRunning(), PostgresChannelMessageTableSubscriber.class.getName() + " must be started");
				MessageHandlerDispatcher dispatcher = new MessageHandlerDispatcher(handler, messageStore, groupId);
				if (subscriber.subscribe(new PostgresChannelMessageTableSubscription(groupId,
						messageStore.getRegion(),
						dispatcher))) {
					dispatcher.run();
					return true;
				}
				else {
					return false;
				}
			}

			@Override
			public boolean unsubscribe(MessageHandler handler) {
				return subscriber.unsubscribe(new PostgresChannelMessageTableSubscription(groupId,
						messageStore.getRegion(),
						new MessageHandlerDispatcher(handler, messageStore, groupId)));
			}

			@Override
			public boolean send(Message<?> message, long timeout) {
				messageStore.addMessageToGroup(groupId, message);
				return true;
			}
		};
	}

	private static final class MessageHandlerDispatcher implements Runnable {

		private final MessageHandler handler;

		private final JdbcChannelMessageStore messageStore;

		private final Object groupId;

		private MessageHandlerDispatcher(MessageHandler handler, JdbcChannelMessageStore messageStore, Object groupId) {
			this.handler = handler;
			this.messageStore = messageStore;
			this.groupId = groupId;
		}

		@Override
		public void run() {
			Message<?> message = this.messageStore.pollMessageFromGroup(this.groupId);
			while (message != null) {
				this.handler.handleMessage(message);
				message = this.messageStore.pollMessageFromGroup(this.groupId);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MessageHandlerDispatcher that = (MessageHandlerDispatcher) o;
			if (!this.handler.equals(that.handler)) {
				return false;
			}
			if (!this.messageStore.equals(that.messageStore)) {
				return false;
			}
			return this.groupId.equals(that.groupId);
		}

		@Override
		public int hashCode() {
			int result = this.handler.hashCode();
			result = 31 * result + this.messageStore.hashCode();
			result = 31 * result + this.groupId.hashCode();
			return result;
		}
	}
}
