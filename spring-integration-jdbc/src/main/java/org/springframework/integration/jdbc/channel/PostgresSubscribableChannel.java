package org.springframework.integration.jdbc.channel;

import java.util.concurrent.Executor;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.UnicastingDispatcher;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

public class PostgresSubscribableChannel extends AbstractSubscribableChannel
implements PostgresChannelMessageTableSubscriber.Subscription {

	private final JdbcChannelMessageStore jdbcChannelMessageStore;

	private final Object groupId;

	private final PostgresChannelMessageTableSubscriber messageTableSubscriber;

	private UnicastingDispatcher dispatcher = new UnicastingDispatcher(new SimpleAsyncTaskExecutor());

	public PostgresSubscribableChannel(JdbcChannelMessageStore jdbcChannelMessageStore,
			Object groupId, PostgresChannelMessageTableSubscriber messageTableSubscriber) {

		this.jdbcChannelMessageStore = jdbcChannelMessageStore;
		this.groupId = groupId;
		this.messageTableSubscriber = messageTableSubscriber;
	}

	public void setDispatcherExecutor(Executor executor) {
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
			notifyUpdate();
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
