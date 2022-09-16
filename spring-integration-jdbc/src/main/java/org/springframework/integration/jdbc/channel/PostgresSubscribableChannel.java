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

import java.util.concurrent.Executor;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Implements a subscribable channel for receiving push notifications for
 * messages send to a group id of a {@link JdbcChannelMessageStore}. Receiving
 * such push notifications is only possible if using a Postgres database.
 * <p/>
 * In order to function, the Postgres database that is used must define a trigger
 * for sending notifications upon newly arrived messages. This trigger is defined
 * in the <i>schema-postgresql.sql</i> file within this artifact but commented
 * out.
 */
public class PostgresSubscribableChannel extends AbstractSubscribableChannel
implements PostgresChannelMessageTableSubscriber.Subscription {

	private final JdbcChannelMessageStore jdbcChannelMessageStore;

	private final Object groupId;

	private final PostgresChannelMessageTableSubscriber messageTableSubscriber;

	private UnicastingDispatcher dispatcher = new UnicastingDispatcher(new SimpleAsyncTaskExecutor());

	/**
	 * Creates a subscribable channel for a Postgres database.
	 * @param jdbcChannelMessageStore The message store to use for the relevant region.
	 * @param groupId The group id that is targeted by the subscription.
	 * @param messageTableSubscriber The subscriber to use for receiving notifications.
	 */
	public PostgresSubscribableChannel(JdbcChannelMessageStore jdbcChannelMessageStore,
			Object groupId, PostgresChannelMessageTableSubscriber messageTableSubscriber) {
		Assert.notNull(jdbcChannelMessageStore, "A jdbcChannelMessageStore must be provided.");
		Assert.notNull(groupId, "A groupId must be set.");
		Assert.notNull(messageTableSubscriber, "A messageTableSubscriber must be set.");
		this.jdbcChannelMessageStore = jdbcChannelMessageStore;
		this.groupId = groupId;
		this.messageTableSubscriber = messageTableSubscriber;
	}

	/**
	 * Sets the executor to use for dispatching newly received messages.
	 * @param executor The executor to use.
	 */
	public void setDispatcherExecutor(Executor executor) {
		Assert.notNull(executor, "An executor must be provided.");
		this.dispatcher = new UnicastingDispatcher(executor);
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		boolean subscribed = super.subscribe(handler);
		if (this.dispatcher.getHandlerCount() == 1) {
			this.messageTableSubscriber.subscribe(this);
			notifyUpdate();
		}
		return subscribed;
	}

	@Override
	public boolean unsubscribe(MessageHandler handle) {
		boolean unsubscribed = super.unsubscribe(handle);
		if (this.dispatcher.getHandlerCount() == 0) {
			this.messageTableSubscriber.unsubscribe(this);
		}
		return unsubscribed;
	}

	@Override
	protected MessageDispatcher getDispatcher() {
		return this.dispatcher;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		this.jdbcChannelMessageStore.addMessageToGroup(this.groupId, message);
		return true;
	}

	@Override
	public void notifyUpdate() {
		Message<?> message;
		while ((message = this.jdbcChannelMessageStore.pollMessageFromGroup(this.groupId)) != null) {
			this.dispatcher.dispatch(message);
		}
	}

	@Override
	public String getRegion() {
		return this.jdbcChannelMessageStore.getRegion();
	}

	@Override
	public Object getGroupId() {
		return this.groupId;
	}
}
