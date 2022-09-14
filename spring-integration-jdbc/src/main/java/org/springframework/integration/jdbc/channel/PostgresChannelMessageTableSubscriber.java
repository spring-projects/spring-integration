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

import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public final class PostgresChannelMessageTableSubscriber implements SmartLifecycle {

	private static final LogAccessor LOGGER = new LogAccessor(PostgresChannelMessageTableSubscriber.class);

	private final PgConnectionSupplier connectionSupplier;

	private final String tablePrefix;

	@Nullable
	private ExecutorService executor;

	private CountDownLatch latch = new CountDownLatch(0);

	private Future<?> future = CompletableFuture.completedFuture(null);

	@Nullable
	private volatile PgConnection connection;

	private final Map<String, Set<Subscription>> subscriptions = new ConcurrentHashMap<>();

	public PostgresChannelMessageTableSubscriber(PgConnectionSupplier connectionSupplier) {
		this(connectionSupplier, JdbcChannelMessageStore.DEFAULT_TABLE_PREFIX);
	}

	public PostgresChannelMessageTableSubscriber(PgConnectionSupplier connectionSupplier, String tablePrefix) {
		Assert.notNull(connectionSupplier, "A connectionSupplier must be provided.");
		Assert.notNull(tablePrefix, "A table prefix must be set.");
		this.connectionSupplier = connectionSupplier;
		this.tablePrefix = tablePrefix;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public SubscribableChannel toSubscribableChannel(
			JdbcChannelMessageStore messageStore,
			Object groupId) {
		return PostgresChannelMessageTableSubscription.asSubscribableChannel(this, messageStore, groupId);
	}

	boolean subscribe(Subscription subscription) {
		Set<Subscription> subscriptions = this.subscriptions.computeIfAbsent(subscription.getRegion() + " " + getKey(subscription.getGroupId()), ignored -> ConcurrentHashMap.newKeySet());
		return subscriptions.add(subscription);
	}

	boolean unsubscribe(Subscription subscription) {
		Set<Subscription> subscriptions = this.subscriptions.get(subscription.getRegion() + " " + getKey(subscription.getGroupId()));
		return subscriptions != null && subscriptions.remove(subscription);
	}

	@Override
	public synchronized void start() {
		if (this.latch.getCount() > 0) {
			return;
		}
		ExecutorService executor = this.executor;
		if (executor == null) {
			executor = Executors.newSingleThreadExecutor(
					job -> {
						Thread t = new Thread(job);
						t.setDaemon(true);
						t.setName("postgres-channel-message-table-subscriber");
						return t;
					}
			);
			this.executor = executor;
		}
		this.latch = new CountDownLatch(1);
		this.future = executor.submit(() -> {
			try {
				while (isActive()) {
					try {
						PgConnection conn = this.connectionSupplier.get();
						try (Statement stmt = conn.createStatement()) {
							stmt.execute("LISTEN " + this.tablePrefix.toLowerCase() + "channel_message_notify");
						} catch (Throwable t) {
							try {
								conn.close();
							} catch (Throwable suppressed) {
								t.addSuppressed(suppressed);
							}
							throw t;
						}
						try {
							this.connection = conn;
							while (isActive()) {
								PGNotification[] notifications = conn.getNotifications(0);
								// Unfortunately, there is no good way of interrupting a notification poll but by closing its connection.
								if (!isActive()) {
									return;
								}
								if (notifications != null) {
									for (PGNotification notification : notifications) {
										String parameter = notification.getParameter();
										Set<Subscription> subscriptions = this.subscriptions.get(parameter);
										if (subscriptions == null) {
											continue;
										}
										for (Subscription subscription : subscriptions) {
											subscription.notifyUpdate();
										}
									}
								}
							}
						} finally {
							conn.close();
						}
					} catch (Exception e) {
						// The getNotifications method does not throw a meaningful message on interruption.
						// Therefore, we do not log an error, unless it occurred while active.
						if (isActive()) {
							LOGGER.error(e, "Failed to poll notifications from Postgres database");
						}
					} catch (Throwable t) {
						LOGGER.error(t, "Failed to poll notifications from Postgres database");
						return;
					}
				}
			} finally {
				this.latch.countDown();
			}
		});
	}

	private boolean isActive() {
		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	@Override
	public synchronized void stop() {
		Future<?> future = this.future;
		if (future.isDone()) {
			return;
		}
		future.cancel(true);
		PgConnection conn = this.connection;
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException ignored) {
			}
		}
		try {
			if (!this.latch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Failed to stop " + PostgresChannelMessageTableSubscriber.class.getName());
			}
		} catch (InterruptedException ignored) {
		}
	}

	@Override
	public boolean isRunning() {
		return this.latch.getCount() > 0;
	}

	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	public interface Subscription {

		void notifyUpdate();

		String getRegion();

		Object getGroupId();

	}

}
