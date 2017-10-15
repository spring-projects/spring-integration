/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.jdbc.store;

import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.jdbc.store.channel.ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.MessageGroupPreparedStatementSetter;
import org.springframework.integration.jdbc.store.channel.MessageRowMapper;
import org.springframework.integration.jdbc.store.channel.OracleChannelMessageStoreQueryProvider;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupFactory;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.integration.store.SimpleMessageGroupFactory;
import org.springframework.integration.support.converter.WhiteListDeserializingConverter;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Channel-specific implementation of {@link MessageGroupStore} using a relational
 * database via JDBC.
 *
 * This message store shall be used for message channels only.
 * </p>
 * <p>
 * As such, the {@link JdbcChannelMessageStore} uses database specific SQL queries.
 * </p>
 * <p>
 * Contrary to the {@link JdbcMessageStore}, this implementation uses a single database table,
 * optimized to operate like a queue.
 * The SQL scripts for creating the table are packaged
 * under {@code org/springframework/integration/jdbc/schema-*.sql},
 * where {@code *} denotes the target database type.
 * </p>
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Meherzad Lahewala
 * @since 2.2
 */
@ManagedResource
public class JdbcChannelMessageStore implements PriorityCapableChannelMessageStore, InitializingBean, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(JdbcChannelMessageStore.class);

	private final Set<String> idCache = new HashSet<String>();

	private final ReadWriteLock idCacheLock = new ReentrantReadWriteLock();

	private final Lock idCacheReadLock = this.idCacheLock.readLock();

	private final Lock idCacheWriteLock = this.idCacheLock.writeLock();

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	/**
	 * Default region property, used to partition the message store. For example,
	 * a separate Spring Integration application with overlapping channel names
	 * may use the same message store by providing a distinct region name.
	 */
	public static final String DEFAULT_REGION = "DEFAULT";

	private ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider;

	/**
	 * The name of the message header that stores a flag to indicate that the message has been saved. This is an
	 * optimization for the put method.
	 * @deprecated since 5.0. This constant isn't used any more.
	 */
	@Deprecated
	public static final String SAVED_KEY = JdbcChannelMessageStore.class.getSimpleName() + ".SAVED";

	/**
	 * The name of the message header that stores a timestamp for the time the message was inserted.
	 * @deprecated since 5.0. This constant isn't used any more.
	 */
	@Deprecated
	public static final String CREATED_DATE_KEY = JdbcChannelMessageStore.class.getSimpleName() + ".CREATED_DATE";

	private volatile String region = DEFAULT_REGION;

	private volatile String tablePrefix = DEFAULT_TABLE_PREFIX;

	private volatile JdbcTemplate jdbcTemplate;

	private volatile WhiteListDeserializingConverter deserializer;

	private volatile SerializingConverter serializer;

	private volatile LobHandler lobHandler = new DefaultLobHandler();

	private volatile MessageRowMapper messageRowMapper;

	private volatile MessageGroupPreparedStatementSetter messageGroupPreparedStatementSetter;

	private volatile Map<String, String> queryCache = new HashMap<String, String>();

	private volatile MessageGroupFactory messageGroupFactory = new SimpleMessageGroupFactory();

	private boolean usingIdCache = false;

	private boolean priorityEnabled;

	@SuppressWarnings("unused")
	private BeanFactory beanFactory;

	/**
	 * Convenient constructor for configuration use.
	 */
	public JdbcChannelMessageStore() {
		this.deserializer = new WhiteListDeserializingConverter();
		this.serializer = new SerializingConverter();
	}

	/**
	 * Create a {@link MessageStore} with all mandatory properties. The passed-in
	 * {@link DataSource} is used to instantiate a {@link JdbcTemplate}
	 *
	 * with {@link JdbcTemplate#setFetchSize(int)} set to <code>1</code>
	 * and with {@link JdbcTemplate#setMaxRows(int)} set to <code>1</code>.
	 *
	 * @param dataSource a {@link DataSource}
	 */
	public JdbcChannelMessageStore(DataSource dataSource) {
		this();
		this.jdbcTemplate = new JdbcTemplate(dataSource);

		this.jdbcTemplate.setFetchSize(1);
		this.jdbcTemplate.setMaxRows(1);
	}

	/**
	 * The JDBC {@link DataSource} to use when interacting with the database.
	 * The passed-in {@link DataSource} is used to instantiate a {@link JdbcTemplate}
	 * with {@link JdbcTemplate#setFetchSize(int)} set to <code>1</code>
	 * and with {@link JdbcTemplate#setMaxRows(int)} set to <code>1</code>.
	 *
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);

		this.jdbcTemplate.setFetchSize(1);
		this.jdbcTemplate.setMaxRows(1);
	}

	/**
	 * A converter for deserializing byte arrays to messages.
	 *
	 * @param deserializer the deserializer to set
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setDeserializer(Deserializer<? extends Message<?>> deserializer) {
		this.deserializer = new WhiteListDeserializingConverter((Deserializer) deserializer);
	}

	/**
	 * Add patterns for packages/classes that are allowed to be deserialized. A class can
	 * be fully qualified or a wildcard '*' is allowed at the beginning or end of the
	 * class name. Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 4.2.13
	 */
	public void addWhiteListPatterns(String... patterns) {
		this.deserializer.addWhiteListPatterns(patterns);
	}

	/**
	 * The {@link JdbcOperations} to use when interacting with the database. Either
	 * this property can be set or the {@link #setDataSource(DataSource) dataSource}.
	 *
	 * Please consider passing in a {@link JdbcTemplate} with a fetchSize property
	 * of 1. This is particularly important for Oracle to ensure First In, First Out (FIFO)
	 * message retrieval characteristics.
	 *
	 * @param jdbcTemplate a {@link JdbcOperations}
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "The provided jdbcTemplate must not be null.");
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Override the {@link LobHandler} that is used to create and unpack large objects in SQL queries. The default is
	 * fine for almost all platforms, but some Oracle drivers require a native implementation.
	 *
	 * @param lobHandler a {@link LobHandler}
	 */
	public void setLobHandler(LobHandler lobHandler) {
		Assert.notNull(lobHandler, "The provided LobHandler must not be null.");
		this.lobHandler = lobHandler;
	}

	/**
	 * Allows for passing in a custom {@link MessageRowMapper}. The {@link MessageRowMapper}
	 * is used to convert the selected database row representing the persisted
	 * message into the actual {@link Message} object.
	 *
	 * @param messageRowMapper Must not be null
	 */
	public void setMessageRowMapper(MessageRowMapper messageRowMapper) {
		Assert.notNull(messageRowMapper, "The provided MessageRowMapper must not be null.");
		this.messageRowMapper = messageRowMapper;
	}

	/**
	 * Allows for passing in a custom {@link MessageGroupPreparedStatementSetter}. 
	 * The {@link MessageGroupPreparedStatementSetter} is used to insert message in the database.
	 *
	 * @param messageGroupPreparedStatementSetter Must not be null
	 */
	public void setMessageGroupPreparedStatementSetter(
			MessageGroupPreparedStatementSetter messageGroupPreparedStatementSetter) {
		Assert.notNull(messageGroupPreparedStatementSetter,
				"The provided MessageGroupPreparedStatementSetter must not be null.");
		this.messageGroupPreparedStatementSetter = messageGroupPreparedStatementSetter;
	}

	/**
	 * Sets the database specific {@link ChannelMessageStoreQueryProvider} to use.
	 * The {@link JdbcChannelMessageStore} provides the SQL queries to retrieve messages from
	 * the database. See the JavaDocs {@link ChannelMessageStoreQueryProvider} (all known
	 * implementing classes) to see those implementations provided by the framework.
	 * <p> You can provide your own query implementations, if you need to support additional
	 * databases and/or need to fine-tune the queries for your requirements.
	 * @param channelMessageStoreQueryProvider Must not be null.
	 */
	public void setChannelMessageStoreQueryProvider(ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider) {
		Assert.notNull(channelMessageStoreQueryProvider,
				"The provided channelMessageStoreQueryProvider must not be null.");
		this.channelMessageStoreQueryProvider = channelMessageStoreQueryProvider;
	}

	/**
	 * A unique grouping identifier for all messages persisted with this store.
	 * Using multiple regions allows the store to be partitioned (if necessary)
	 * for different purposes. Defaults to <code>{@link #DEFAULT_REGION}</code>.
	 *
	 * @param region the region name to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}

	/**
	 * A converter for serializing messages to byte arrays for storage.
	 *
	 * @param serializer The serializer to set
	 */
	@SuppressWarnings("unchecked")
	public void setSerializer(Serializer<? super Message<?>> serializer) {
		Assert.notNull(serializer, "The provided serializer must not be null.");
		this.serializer = new SerializingConverter((Serializer<Object>) serializer);
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all the table names before queries are
	 * executed. Defaults to {@link #DEFAULT_TABLE_PREFIX}.
	 *
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * <p>Consider using this property when polling the database transactionally
	 * using multiple parallel threads, meaning when the configured poller is configured
	 * using a task executor.</p>
	 *
	 * <p>The issue is that the {@link #pollMessageFromGroup(Object)} looks for the
	 * oldest entry for a giving channel (groupKey) and region ({@link #setRegion(String)}).
	 * If you do that with multiple threads and you are using transactions, other
	 * threads may be waiting for that same locked row.</p>
	 *
	 * <p>If using the provided {@link OracleChannelMessageStoreQueryProvider}, don't set {@link #usingIdCache}
	 * to true, as the Oracle query will ignore locked rows.</p>
	 *
	 * <p>Using the id cache, the {@link JdbcChannelMessageStore} will store each
	 * message id in an in-memory collection for the duration of processing. With
	 * that, any polling threads will explicitly exclude those messages from
	 * being polled.</p>
	 *
	 * <p>For this to work, you must setup the corresponding
	 * {@link TransactionSynchronizationFactory}:</p>
	 *
	 * <pre class="code">
	 * {@code
	 * <int:transaction-synchronization-factory id="syncFactory">
	 *     <int:after-commit   expression="@jdbcChannelMessageStore.removeFromIdCache(headers.id.toString())" />
	 *     <int:after-rollback expression="@jdbcChannelMessageStore.removeFromIdCache(headers.id.toString())" />
	 * </int:transaction-synchronization-factory>
	 * }
	 * </pre>
	 *
	 * This {@link TransactionSynchronizationFactory} is then referenced in the
	 * transaction configuration of the poller:
	 *
	 * <pre class="code">
	 * {@code
	 * <int:poller fixed-delay="300" receive-timeout="500"
	 *     max-messages-per-poll="1" task-executor="pool">
	 *     <int:transactional propagation="REQUIRED" synchronization-factory="syncFactory"
	 *         isolation="READ_COMMITTED" transaction-manager="transactionManager" />
	 * </int:poller>
	 * }
	 * </pre>
	 *
	 * @param usingIdCache When <code>true</code> the id cache will be used.
	 */
	public void setUsingIdCache(boolean usingIdCache) {
		this.usingIdCache = usingIdCache;
	}

	public void setPriorityEnabled(boolean priorityEnabled) {
		this.priorityEnabled = priorityEnabled;
	}

	@Override
	public boolean isPriorityEnabled() {
		return this.priorityEnabled;
	}

	/**
	 * Specify the {@link MessageGroupFactory} to create {@link MessageGroup} object where
	 * it is necessary.
	 * Defaults to {@link SimpleMessageGroupFactory}.
	 * @param messageGroupFactory the {@link MessageGroupFactory} to use.
	 * @since 4.3
	 */
	public void setMessageGroupFactory(MessageGroupFactory messageGroupFactory) {
		Assert.notNull(messageGroupFactory, "'messageGroupFactory' must not be null");
		this.messageGroupFactory = messageGroupFactory;
	}

	protected MessageGroupFactory getMessageGroupFactory() {
		return this.messageGroupFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Check mandatory properties ({@link DataSource} and
	 * {@link #setChannelMessageStoreQueryProvider(ChannelMessageStoreQueryProvider)}). If no {@link MessageRowMapper} 
	 * and {@link MessageGroupPreparedStatementSetter} was explicitly set using
	 * {@link #setMessageRowMapper(MessageRowMapper)} and 
	 * {@link #setMessageGroupPreparedStatementSetter(MessageGroupPreparedStatementSetter)}  respectively, the default
	 * {@link MessageRowMapper} and {@link MessageGroupPreparedStatementSetter} will be instantiate using the 
	 * specified {@link #deserializer} and {@link #lobHandler}.
	 *
	 * Also, if the jdbcTemplate's fetchSize property ({@link JdbcTemplate#getFetchSize()})
	 * is not 1, a warning will be logged. When using the {@link JdbcChannelMessageStore}
	 * with Oracle, the fetchSize value of 1 is needed to ensure FIFO characteristics
	 * of polled messages. Please see the Oracle {@link ChannelMessageStoreQueryProvider} for more details.
	 *
	 * @throws Exception Any Exception.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(this.jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
		Assert.notNull(this.channelMessageStoreQueryProvider, "A channelMessageStoreQueryProvider must be provided.");

		if (this.messageRowMapper == null) {
			this.messageRowMapper = new MessageRowMapper(this.deserializer, this.lobHandler);
		}

		if (this.jdbcTemplate.getFetchSize() != 1 && logger.isWarnEnabled()) {
			logger.warn("The jdbcTemplate's fetch size is not 1. This may cause FIFO issues with Oracle databases.");
		}

		if (this.messageGroupPreparedStatementSetter == null) {
			this.messageGroupPreparedStatementSetter = (preparedStatement, message, groupId) -> {
				String groupKey = getKey(groupId);
				long createdDate = System.currentTimeMillis();
				String messageId = getKey(message.getHeaders().getId());
				byte[] messageBytes = this.serializer.convert(message);

				if (logger.isDebugEnabled()) {
					logger.debug("Inserting message with id key=" + messageId);
				}

				preparedStatement.setString(1, messageId);
				preparedStatement.setString(2, groupKey);
				preparedStatement.setString(3, this.region);
				preparedStatement.setLong(4, createdDate);

				Integer priority = message.getHeaders().get(IntegrationMessageHeaderAccessor.PRIORITY, Integer.class);

				if (JdbcChannelMessageStore.this.priorityEnabled && priority != null) {
					preparedStatement.setInt(5, priority);
				} else {
					preparedStatement.setNull(5, Types.NUMERIC);
				}

				this.lobHandler.getLobCreator().setBlobAsBytes(preparedStatement, 6, messageBytes);
			};
		}
		this.jdbcTemplate.afterPropertiesSet();
	}

	/**
	 * Store a message in the database. The groupId identifies the channel for which
	 * the message is to be stored.
	 *
	 * Keep in mind that the actual groupId (Channel Identifier) is converted to a String-based UUID identifier.
	 *
	 * @param groupId the group id to store the message under
	 * @param message a message
	 */
	@Override
	public MessageGroup addMessageToGroup(Object groupId, final Message<?> message) {
		try {
			this.jdbcTemplate.update(getQuery(this.channelMessageStoreQueryProvider.getCreateMessageQuery()),
					ps -> this.messageGroupPreparedStatementSetter.setValues(ps, message, groupId));
		} catch (DuplicateKeyException e) {
			if (logger.isDebugEnabled()) {
				String messageId = getKey(message.getHeaders().getId());
				logger.debug("The Message with id [" + messageId + "] already exists.\nIgnoring INSERT...");
			}
		}
		return getMessageGroup(groupId);
	}

	/**
	 * Helper method that converts the channel id to a UUID using
	 * {@link UUIDConverter#getUUID(Object)}.
	 *
	 * @param input Parameter may be null
	 * @return Returns null when the input is null otherwise the UUID as String.
	 */
	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	/**
	 * Not fully used. Only wraps the provided group id.
	 */
	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		return getMessageGroupFactory().create(groupId);
	}

	/**
	 * Method not implemented.
	 *
	 * @return The message group count.
	 * @throws UnsupportedOperationException Method not supported.
	 */
	@ManagedAttribute
	public int getMessageGroupCount() {
		return this.jdbcTemplate.queryForObject(this.getQuery("SELECT COUNT(DISTINCT GROUP_KEY) from %PREFIX%CHANNEL_MESSAGE where REGION = ?"),
				Integer.class, this.region);
	}

	/**
	 * Replace patterns in the input to produce a valid SQL query. This implementation lazily initializes a
	 * simple map-based cache, only replacing the table prefix on the first access to a named query. Further
	 * accesses will be resolved from the cache.
	 *
	 * @param sqlQuery The SQL query to be transformed.
	 * @return A transformed query with replacements.
	 */
	protected String getQuery(String sqlQuery) {
		String query = this.queryCache.get(sqlQuery);

		if (query == null) {
			query = StringUtils.replace(sqlQuery, "%PREFIX%", this.tablePrefix);
			this.queryCache.put(sqlQuery, query);
		}

		return query;
	}

	/**
	 * Returns the number of messages persisted for the specified channel id (groupId)
	 * and the specified region ({@link #setRegion(String)}).
	 *
	 * @return The message group size.
	 */
	@Override
	@ManagedAttribute
	public int messageGroupSize(Object groupId) {
		final String key = getKey(groupId);
		return this.jdbcTemplate.queryForObject(getQuery(this.channelMessageStoreQueryProvider.getCountAllMessagesInGroupQuery()),
				Integer.class, key, this.region);
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		this.jdbcTemplate.update(this.getQuery(this.channelMessageStoreQueryProvider.getDeleteMessageGroupQuery()),
				this.getKey(groupId), this.region);
	}

	/**
	 * Polls the database for a new message that is persisted for the given
	 * group id which represents the channel identifier.
	 */
	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {

		final String key = getKey(groupId);
		final Message<?> polledMessage = this.doPollForMessage(key);

		if (polledMessage != null) {
			if (!this.doRemoveMessageFromGroup(groupId, polledMessage)) {
				return null;
			}
		}

		return polledMessage;
	}

	/**
	 * This method executes a call to the DB to get the oldest Message in the
	 * MessageGroup which in the context of the {@link JdbcChannelMessageStore}
	 * means the channel identifier.
	 *
	 * @param groupIdKey String representation of message group (Channel) ID
	 * @return a message; could be null if query produced no Messages
	 */
	protected Message<?> doPollForMessage(String groupIdKey) {

		final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(this.jdbcTemplate);
		final MapSqlParameterSource parameters = new MapSqlParameterSource();

		parameters.addValue("region", this.region);
		parameters.addValue("group_key", groupIdKey);

		String query;

		final List<Message<?>> messages;

		this.idCacheReadLock.lock();
		try {
			if (this.usingIdCache && !this.idCache.isEmpty()) {
				if (this.priorityEnabled) {
					query = getQuery(this.channelMessageStoreQueryProvider.getPriorityPollFromGroupExcludeIdsQuery());
				}
				else {
					query = getQuery(this.channelMessageStoreQueryProvider.getPollFromGroupExcludeIdsQuery());
				}
				parameters.addValue("message_ids", this.idCache);
			}
			else {
				if (this.priorityEnabled) {
					query = getQuery(this.channelMessageStoreQueryProvider.getPriorityPollFromGroupQuery());
				}
				else {
					query = getQuery(this.channelMessageStoreQueryProvider.getPollFromGroupQuery());
				}
			}
			messages = namedParameterJdbcTemplate.query(query, parameters, this.messageRowMapper);
		}
		finally {
			this.idCacheReadLock.unlock();
		}


		Assert.state(messages.size() < 2,
				() -> "The query must return zero or 1 row; got " + messages.size() + " rows");
		if (messages.size() > 0) {

			final Message<?> message = messages.get(0);
			final String messageId = message.getHeaders().getId().toString();

			if (this.usingIdCache) {
				this.idCacheWriteLock.lock();
				try {
					boolean added = this.idCache.add(messageId);

					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Polled message with id '%s' added: '%s'.", messageId, added));
					}
				}
				finally {
					this.idCacheWriteLock.unlock();
				}
			}

			return message;
		}
		return null;
	}

	private boolean doRemoveMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		final UUID id = messageToRemove.getHeaders().getId();

		int updated = this.jdbcTemplate.update(getQuery(this.channelMessageStoreQueryProvider.getDeleteMessageQuery()),
				new Object[]{getKey(id), getKey(groupId), this.region}, new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});

		boolean result = updated != 0;
		if (result) {
			logger.debug(String.format("Message with id '%s' was deleted.", id));
		}
		else {
			logger.warn(String.format("Message with id '%s' was not deleted.", id));
		}

		return result;
	}

	/**
	 * <p>Remove a Message Id from the idCache. Should be used in conjunction
	 * with the Spring Integration Transaction Synchronization feature to remove
	 * a message from the Message Id cache once a transaction either succeeded or
	 * rolled back.</p>
	 * <p>Only applicable if {@link #setUsingIdCache(boolean)} is set to
	 * <code>true</code></p>.
	 *
	 * @param messageId The message identifier.
	 */
	public void removeFromIdCache(String messageId) {
		if (logger.isDebugEnabled()) {
			logger.debug("Removing Message Id: " + messageId);
		}
		this.idCacheWriteLock.lock();
		try {
			this.idCache.remove(messageId);
		}
		finally {
			this.idCacheWriteLock.unlock();
		}
	}

	/**
	 * Returns the size of the Message Id Cache, which caches Message Ids for
	 * those messages that are currently being processed.
	 *
	 * @return The size of the Message Id Cache
	 */
	@ManagedMetric
	public int getSizeOfIdCache() {
		return this.idCache.size();
	}

}
