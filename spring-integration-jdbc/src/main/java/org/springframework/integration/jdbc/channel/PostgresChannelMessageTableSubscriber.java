/*
 * Copyright 2022-2024 the original author or authors.
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
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.postgresql.PGNotification;
import org.postgresql.jdbc.PgConnection;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
 * {@link Subscription} and register it with this subscriber.
 * <p/>
 * In order to function, the Postgres database that is used must define a trigger
 * for sending notifications upon newly arrived messages. This trigger is defined
 * in the <i>schema-postgresql.sql</i> file within this artifact but commented
 * out.
 *
 * @author Rafael Winterhalter
 * @author Artem Bilan
 * @author Igor Lovich
 * @author Christian Tzolov
 * @author Johannes Edmeier
 *
 * @since 6.0
 */
public final class PostgresChannelMessageTableSubscriber implements SmartLifecycle {

	private static final LogAccessor LOGGER = new LogAccessor(PostgresChannelMessageTableSubscriber.class);

	private final Lock lock = new ReentrantLock();

	private final Map<String, Set<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();

	private final PgConnectionSupplier connectionSupplier;

	private final String tablePrefix;

	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("postgres-channel-message-table-subscriber-");

	private CountDownLatch latch = new CountDownLatch(0);

	private Future<?> future = CompletableFuture.completedFuture(null);

	@Nullable
	private volatile PgConnection connection;

	private Duration notificationTimeout = Duration.ofSeconds(60);

	/**
	 * Create a new subscriber using the {@link JdbcChannelMessageStore#DEFAULT_TABLE_PREFIX}.
	 * @param connectionSupplier The connection supplier for the targeted Postgres database.
	 */
	public PostgresChannelMessageTableSubscriber(PgConnectionSupplier connectionSupplier) {
		this(connectionSupplier, JdbcChannelMessageStore.DEFAULT_TABLE_PREFIX);
	}

	/**
	 * Create a new subscriber.
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
	 * Provide a managed {@link AsyncTaskExecutor} for Postgres listener daemon.
	 * @param taskExecutor the {@link AsyncTaskExecutor} to use.
	 * @since 6.2
	 */
	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "A 'taskExecutor' must not be null.");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Set the timeout for the notification polling.
	 * If for the specified duration no notificiation are received the underlying connection is closed and re-established.
	 * Setting a value of {@code Duration.ZERO} will disable the timeout and wait forever.
	 * This might cause problems in DB failover scenarios.
	 * @param notificationTimeout the timeout for the notification polling.
	 * @since 6.1.8
	 */
	public void setNotificationTimeout(Duration notificationTimeout) {
		Assert.notNull(notificationTimeout, "'notificationTimeout' must not be null.");
		this.notificationTimeout = notificationTimeout;
	}

	/**
	 * Add a new subscription to this subscriber.
	 * @param subscription The subscription to register.
	 * @return {@code true} if the subscription was not already added.
	 */
	public boolean subscribe(Subscription subscription) {
		String subscriptionKey = subscription.getRegion() + " " + getKey(subscription.getGroupId());
		Set<Subscription> subscriptions =
				this.subscriptionsMap.computeIfAbsent(subscriptionKey, __ -> ConcurrentHashMap.newKeySet());
		return subscriptions.add(subscription);
	}

	/**
	 * Remove a previous subscription from this subscriber.
	 * @param subscription The subscription to remove.
	 * @return {@code true} if the subscription was previously registered and is now removed.
	 */
	public boolean unsubscribe(Subscription subscription) {
		String subscriptionKey = subscription.getRegion() + " " + getKey(subscription.getGroupId());
		Set<Subscription> subscriptions = this.subscriptionsMap.get(subscriptionKey);
		return subscriptions != null && subscriptions.remove(subscription);
	}

	@Override
	public void start() {
		this.lock.lock();
		try {
			if (this.latch.getCount() > 0) {
				return;
			}

			this.latch = new CountDownLatch(1);

			CountDownLatch startingLatch = new CountDownLatch(1);
			this.future = this.taskExecutor.submit(() -> {
				doStart(startingLatch);
			});

			try {
				if (!startingLatch.await(5, TimeUnit.SECONDS)) {
					throw new IllegalStateException("Failed to start " + this);
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Failed to start " + this, ex);
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	private void doStart(CountDownLatch startingLatch) {
		try {
			while (isActive()) {
				try {
					PgConnection conn = this.connectionSupplier.get();
					try (Statement stmt = conn.createStatement()) {
						stmt.execute("LISTEN " + this.tablePrefix.toLowerCase(Locale.ROOT) + "channel_message_notify");
					}
					catch (Exception ex) {
						try {
							conn.close();
						}
						catch (Exception suppressed) {
							ex.addSuppressed(suppressed);
						}
						throw ex;
					}
					this.subscriptionsMap.values()
							.forEach(subscriptions -> subscriptions.forEach(Subscription::notifyUpdate));
					try {
						this.connection = conn;
						while (isActive()) {
							startingLatch.countDown();

							PGNotification[] notifications = conn.getNotifications((int) this.notificationTimeout.toMillis());
							// Unfortunately, there is no good way of interrupting a notification
							// poll but by closing its connection.
							if (!isActive()) {
								return;
							}
							if ((notifications == null || notifications.length == 0) && !conn.isValid(1)) {
								//We did not receive any notifications within the timeout period.
								//If the connection is still valid, we will continue polling
								//Otherwise, we will close the connection and re-establish it.
								break;
							}
							for (PGNotification notification : notifications) {
								String parameter = notification.getParameter();
								Set<Subscription> subscriptions = this.subscriptionsMap.get(parameter);
								if (subscriptions == null) {
									continue;
								}
								for (Subscription subscription : subscriptions) {
									subscription.notifyUpdate();
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
			}
		}
		finally {
			this.latch.countDown();
		}

	}

	private boolean isActive() {
		if (Thread.interrupted()) {
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	@Override
	public void stop() {
		this.lock.lock();
		try {
			if (this.future.isDone()) {
				return;
			}
			this.future.cancel(true);
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
					throw new IllegalStateException("Failed to stop " + this);
				}
			}
			catch (InterruptedException ignored) {
			}
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	public boolean isRunning() {
		return this.latch.getCount() > 0;
	}

	private static String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	/**
	 * A subscription to a {@link PostgresChannelMessageTableSubscriber} for
	 * receiving push notifications for new messages that are added to
	 * a {@link JdbcChannelMessageStore}.
	 */
	public interface Subscription {

		/**
		 * Indicate that a message was added to the represented region and
		 * group id. Note that this method might also be invoked if there are
		 * no new messages to read, for example if another subscription already
		 * read those messages or if a new messages might have arrived during
		 * a temporary connection loss.
		 */
		void notifyUpdate();

		/**
		 * Return the region for which this subscription receives notifications.
		 * @return The relevant region of the {@link JdbcChannelMessageStore}.
		 */
		String getRegion();

		/**
		 * Return the group id for which this subscription receives notifications.
		 * @return The group id of the {@link PostgresSubscribableChannel}.
		 */
		Object getGroupId();

	}

}
