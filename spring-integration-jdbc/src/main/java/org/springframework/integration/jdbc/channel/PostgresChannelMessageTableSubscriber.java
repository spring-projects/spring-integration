/*
 * Copyright 2022-2022 the original author or authors.
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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A subscriber for new messages being received by a Postgres database via a
 * {@link JdbcChannelMessageStore}. This subscriber implementation is using
 * Postgres' <i>LISTEN</i>/<i>NOTIFY</i> mechanism to allow for receiving push
 * notifications for new messages what functions even if a message is written
 * and read from different JVMs or {@link JdbcChannelMessageStore}s.
 * <p/>
 * Note that this subscriber requires an unshared {@link PgConnection} which
 * remains open for any lifecycle. It is therefore recommended to execute a single
 * subscriber for any JVM. For this reason, this subscriber is region-agnostic.
 * To listen for messages for a given region and group id, use a
 * {@link PostgresSubscribableChannel} and register it with this subscriber.
 * <p/>
 * In order to function, the Postgres database that is used must define a trigger
 * for sending notifications upon newly arrived messages. This trigger is defined
 * in the <i>schema-postgresql.sql</i> file within this artifact but commented
 * out.
 *
 * @author Rafael Winterhalter
 * @author Artem Bilan
 * @since 6
 */
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

	/**
	 * Creates a new subscriber using the {@link JdbcChannelMessageStore#DEFAULT_TABLE_PREFIX}.
	 * @param connectionSupplier The connection supplier for the targeted Postgres database.
	 */
	public PostgresChannelMessageTableSubscriber(PgConnectionSupplier connectionSupplier) {
		this(connectionSupplier, JdbcChannelMessageStore.DEFAULT_TABLE_PREFIX);
	}

	/**
	 * Creates a new subscriber.
	 * @param tablePrefix The table prefix of the {@link JdbcChannelMessageStore} to subscribe to.
	 * @param connectionSupplier The connection supplier for the targeted Postgres database.
	 */
	public PostgresChannelMessageTableSubscriber(PgConnectionSupplier connectionSupplier, String tablePrefix) {
		Assert.notNull(connectionSupplier, "A connectionSupplier must be provided.");
		Assert.notNull(tablePrefix, "A table prefix must be set.");
		this.connectionSupplier = connectionSupplier;
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Defines an executor to use for listening for new messages. Note that the Postgres SQL driver implements
	 * listening for notifications as a blocking operation which will permanently block a thread of this executor
	 * while running.
	 * @param executor The executor to use or {@code null} if an executor should be created by this class.
	 */
	public void setExecutor(@Nullable ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Adds a new subscription to this subscriber.
	 * @param subscription The subscription to register.
	 * @return {@code true} if the subscription was not already added.
	 */
	public boolean subscribe(Subscription subscription) {
		Set<Subscription> subscriptions = this.subscriptions.computeIfAbsent(subscription.getRegion() + " " + getKey(subscription.getGroupId()), ignored -> ConcurrentHashMap.newKeySet());
		return subscriptions.add(subscription);
	}

	/**
	 * Removes a previous subscription from this subscriber.
	 * @param subscription The subscription to remove.
	 * @return {@code true} if the subscription was previously registered and is now removed.
	 */
	public boolean unsubscribe(Subscription subscription) {
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
						}
						catch (Throwable t) {
							try {
								conn.close();
							}
							catch (Throwable suppressed) {
								t.addSuppressed(suppressed);
							}
							throw t;
						}
						this.subscriptions.values().forEach(subscriptions -> subscriptions.forEach(Subscription::notifyUpdate));
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
						}
						finally {
							conn.close();
						}
					}
					catch (Exception e) {
						// The getNotifications method does not throw a meaningful message on interruption.
						// Therefore, we do not log an error, unless it occurred while active.
						if (isActive()) {
							LOGGER.error(e, "Failed to poll notifications from Postgres database");
						}
					}
					catch (Throwable t) {
						LOGGER.error(t, "Failed to poll notifications from Postgres database");
						return;
					}
				}
			}
			finally {
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
			}
			catch (SQLException ignored) {
			}
		}
		try {
			if (!this.latch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Failed to stop " + PostgresChannelMessageTableSubscriber.class.getName());
			}
		}
		catch (InterruptedException ignored) {
		}
	}

	@Override
	public boolean isRunning() {
		return this.latch.getCount() > 0;
	}

	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	/**
	 * A subscription to a {@link PostgresChannelMessageTableSubscriber} for
	 * receiving push notifications for new messages that are added to
	 * a {@link JdbcChannelMessageStore}.
	 */
	public interface Subscription {

		/**
		 * Indicates that a message was added to the represented region and
		 * group id. Note that this method might also be invoked if there are
		 * no new messages to read, for example if another subscription already
		 * read those messages or if a new messages might have arrived during
		 * a temporary connection loss.
		 */
		void notifyUpdate();

		/**
		 * Returns the region for which this subscription receives notifications.
		 * @return The relevant region of the {@link JdbcChannelMessageStore}.
		 */
		String getRegion();

		/**
		 * Returns the group id for which this subscription receives notifications.
		 * @return The group id of the {@link JdbcChannelMessageStore}.
		 */
		Object getGroupId();

	}
}
