/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc.store;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.jdbc.JdbcMessageStore;
import org.springframework.integration.jdbc.store.channel.DerbyQueryProvider;
import org.springframework.integration.jdbc.store.channel.MessageMapper;
import org.springframework.integration.jdbc.store.channel.MySqlQueryProvider;
import org.springframework.integration.jdbc.store.channel.OracleQueryProvider;
import org.springframework.integration.jdbc.store.channel.PostgresQueryProvider;
import org.springframework.integration.jdbc.store.channel.QueryProvider;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
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
 * <strong>NOTICE</strong>: This implementation may change for Spring Integration
 * 3.0. It is provided for use-cases where the current {@link JdbcMessageStore}
 * is not delivering the desired performance characteristics.
 * </p>
 *
 * <p>
 * As such, the {@link JdbcChannelMessageStore} uses database specific SQL queries.
 * </p>
 * <p>
 * Contrary to the {@link JdbcMessageStore}, this implementation uses one single
 * database table only. The SQL scripts to create the necessary table are packaged
 * under <code>org/springframework/integration/jdbc/messagestore/channel/schema-*.sql</code>,
 * where <code>*</code> denotes the target database type.
 * </p
 * >
 * @author Gunnar Hillert
 * @since 2.2
 */
@ManagedResource
public class JdbcChannelMessageStore extends AbstractMessageGroupStore implements InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcChannelMessageStore.class);
	private final Set<String> idCache = Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());

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

	private QueryProvider queryProvider;

	public static final int DEFAULT_LONG_STRING_LENGTH = 2500;

	/**
	 * The name of the message header that stores a flag to indicate that the message has been saved. This is an
	 * optimization for the put method.
	 */
	public static final String SAVED_KEY = JdbcChannelMessageStore.class.getSimpleName() + ".SAVED";

	/**
	 * The name of the message header that stores a timestamp for the time the message was inserted.
	 */
	public static final String CREATED_DATE_KEY = JdbcChannelMessageStore.class.getSimpleName() + ".CREATED_DATE";

	private volatile String region = DEFAULT_REGION;

	private volatile String tablePrefix = DEFAULT_TABLE_PREFIX;

	private volatile JdbcTemplate jdbcTemplate;

	private volatile DeserializingConverter deserializer;

	private volatile SerializingConverter serializer;

	private volatile LobHandler lobHandler = new DefaultLobHandler();

	private volatile MessageMapper messageMapper;

	private volatile Map<String, String> queryCache = new HashMap<String, String>();

	private boolean usingIdCache = false;

	/**
	 * Convenient constructor for configuration use.
	 */
	public JdbcChannelMessageStore() {
		deserializer = new DeserializingConverter();
		serializer = new SerializingConverter();
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
		jdbcTemplate = new JdbcTemplate(dataSource);

		this.jdbcTemplate.setFetchSize(1);
		this.jdbcTemplate.setMaxRows(1);

		this.jdbcTemplate.afterPropertiesSet();
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
		jdbcTemplate = new JdbcTemplate(dataSource);

		this.jdbcTemplate.setFetchSize(1);
		this.jdbcTemplate.setMaxRows(1);

		this.jdbcTemplate.afterPropertiesSet();
	}

	/**
	 * A converter for deserializing byte arrays to messages.
	 *
	 * @param deserializer the deserializer to set
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setDeserializer(Deserializer<? extends Message<?>> deserializer) {
		this.deserializer = new DeserializingConverter((Deserializer) deserializer);
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
	 * Method not implemented.
	 * @throws UnsupportedOperationException
	 */
	public void setLastReleasedSequenceNumberForGroup(Object groupId, final int sequenceNumber) {
		throw new UnsupportedOperationException("Not implemented");
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
	 * Allows for passing in a custom {@link MessageMapper}. The {@link MessageMapper}
	 * is used to convert the selected database row representing the persisted
	 * message into the actual {@link Message} object.
	 *
	 * @param messageMapper Must not be null
	 */
	public void setMessageMapper(MessageMapper messageMapper) {
		Assert.notNull(messageMapper, "The provided MessageMapper must not be null.");
		this.messageMapper = messageMapper;
	}

	/**
	 * <p>
	 * Sets the database specific {@link QueryProvider} to use. The {@link JdbcChannelMessageStore}
	 * provides the SQL queries to retrieve messages from the database. The
	 * following {@link QueryProvider} are provided:
	 * </p>
	 * <ul>
	 *     <li>{@link DerbyQueryProvider}</li>
	 *     <li>{@link MySqlQueryProvider}</li>
	 *     <li>{@link OracleQueryProvider}</li>
	 *     <li>{@link PostgresQueryProvider}</li>
	 * </ul>
	 * <p>
	 * Beyond, you can provide your own query implementations, in case you need
	 * to support additional databases and/or need to fine-tune the queries for
	 * your requirements.
	 * </p>
	 *
	 * @param queryProvider Must not be null.
	 */
	public void setQueryProvider(QueryProvider queryProvider) {
		Assert.notNull(queryProvider, "The provided queryProvider must not be null.");
		this.queryProvider = queryProvider;
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
	 * <p>If using the provided {@link OracleQueryProvider}, don't set {@link #usingIdCache}
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
	 * <pre>
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
	 * <pre>
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

	/**
	 * Check mandatory properties ({@link DataSource} and
	 * {@link #setQueryProvider(QueryProvider)}). If no {@link MessageMapper} was
	 * explicitly set using {@link #setMessageMapper(MessageMapper)}, the default
	 * {@link MessageMapper} will be instantiate using the specified {@link #deserializer}
	 * and {@link #lobHandler}.
	 *
	 * Also, if the jdbcTemplate's fetchSize property ({@link JdbcTemplate#getFetchSize()})
	 * is not 1, a warning will be logged. When using the {@link JdbcChannelMessageStore}
	 * with Oracle, the fetchSize value of 1 is needed to ensure FIFO characteristics
	 * of polled messages. Please see the Oracle {@link QueryProvider} for more details.
	 *
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
		Assert.notNull(this.queryProvider, "A queryProvider must be provided.");

		if (this.messageMapper == null) {
			this.messageMapper = new MessageMapper(this.deserializer, this.lobHandler);
		}

		if (this.jdbcTemplate.getFetchSize() != 1 && logger.isWarnEnabled()) {
			logger.warn("The jdbcTemplate's fetchsize is not 1 but %s. This may cause FIFO issues with Oracle databases.");
		}

	}

	/**
	 * Store a message in the database. The groupId identifies the channel for which
	 * the message is to be stored.
	 *
	 * Keep in mind that the actual groupdId (Channel
	 * Identifier) is converted to a String-based UUID identifier.
	 *
	 * @param groupId the group id to store the message under
	 * @param message a message
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {

		final String groupKey = getKey(groupId);

		final long createdDate = System.currentTimeMillis();
		final Message<?> result = MessageBuilder.fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE)
				.setHeader(CREATED_DATE_KEY, new Long(createdDate)).build();

		final Map innerMap = (Map) new DirectFieldAccessor(result.getHeaders()).getPropertyValue("headers");
		// using reflection to set ID since it is immutable through MessageHeaders
		innerMap.put(MessageHeaders.ID, message.getHeaders().get(MessageHeaders.ID));

		final String messageId = getKey(result.getHeaders().getId());
		final byte[] messageBytes = serializer.convert(result);

		jdbcTemplate.update(getQuery(queryProvider.getCreateMessageQuery()), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Inserting message with id key=" + messageId);
				}
				ps.setString(1, messageId);
				ps.setString(2, groupKey);
				ps.setString(3, region);
				ps.setLong(4, createdDate);
				lobHandler.getLobCreator().setBlobAsBytes(ps, 5, messageBytes);
			}
		});

		return getMessageGroup(groupId);
	}

	/**
	 * Method not implemented.
	 * @throws UnsupportedOperationException
	 */
	public void completeGroup(Object groupId) {
		throw new UnsupportedOperationException("Not implemented");
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

		final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
		final MapSqlParameterSource parameters = new MapSqlParameterSource();

		parameters.addValue("region", region);
		parameters.addValue("group_key", groupIdKey);

		final String query;

		synchronized (idCache) {
			if (this.usingIdCache && !this.idCache.isEmpty()) {
				query = getQuery(this.queryProvider.getPollFromGroupExcludeIdsQuery());
				parameters.addValue("message_ids", idCache);
			} else {
				query = getQuery(this.queryProvider.getPollFromGroupQuery());
			}
		}

		final List<Message<?>> messages = namedParameterJdbcTemplate.query(query, parameters, messageMapper);

		Assert.isTrue(messages.size() == 0 || messages.size() == 1);
		if (messages.size() > 0){

			final Message<?>message = messages.get(0);
			final String messageId = message.getHeaders().getId().toString();

			if (this.usingIdCache) {

				boolean added = this.idCache.add(messageId);

				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Polled message with id '%s' added: '%s'.", messageId, added));
				}
			}

			return message;
		}
		return null;
	}

	/**
	 * Helper method that converts the channel id to a UUID.
	 *
	 * @param input
	 * @return
	 */
	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	/**
	 * Method not implemented.
	 * @throws UnsupportedOperationException
	 */
	@ManagedAttribute
	public long getMessageCount() {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Method not implemented.
	 * @throws UnsupportedOperationException
	 */
	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Not fully used. Only wraps the provided group id.
	 */
	public MessageGroup getMessageGroup(Object groupId) {
		return new SimpleMessageGroup(groupId);
	}

	/**
	 * Method not implemented.
	 * @throws UnsupportedOperationException
	 */
	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Replace patterns in the input to produce a valid SQL query. This implementation lazily initializes a
	 * simple map-based cache, only replacing the table prefix on the first access to a named query. Further
	 * accesses will be resolved from the cache.
	 *
	 * @param sqlQuery the SQL query to be transformed
	 * @return a transformed query with replacements
	 */
	protected String getQuery(String sqlQuery) {
		String query = queryCache.get(sqlQuery);

		if (query == null) {
			query = StringUtils.replace(sqlQuery, "%PREFIX%", tablePrefix);
			queryCache.put(sqlQuery, query);
		}

		return query;
	}

	/**
	 * Method not implemented.
	 * @throws UnsupportedOperationException
	 */
	public Iterator<MessageGroup> iterator() {
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Returns the number of messages persisted for the specified channel id (groupId)
	 * and the specified region ({@link #setRegion(String)}).
	 */
	@ManagedAttribute
	public int messageGroupSize(Object groupId) {
		final String key = getKey(groupId);
		return jdbcTemplate.queryForInt(getQuery(queryProvider.getCountAllMessagesInGroupQuery()), key, this.region);
	}

	/**
	 * Polls the database for a new message that is persisted for the given
	 * group id which represents the channel identifier.
	 */
	public Message<?> pollMessageFromGroup(Object groupId) {

		final String key = getKey(groupId);
		final Message<?> polledMessage = this.doPollForMessage(key);

		if (polledMessage != null){
			this.removeMessageFromGroup(groupId, polledMessage);
		}

		return polledMessage;
	}

	/**
	 * Remove a single message from the database.
	 *
	 * @param groupId The channel id to remove the message from
	 * @param messageToRemove The message to remove
	 *
	 */
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {

		final UUID id = messageToRemove.getHeaders().getId();

		int updated = jdbcTemplate.update(getQuery(queryProvider.getDeleteMessageQuery()), new Object[] { getKey(id), getKey(groupId), region }, new int[] {
					Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });

		if (updated != 0) {
			logger.debug(String.format("Message with id '%s' was deleted.", id));
		} else {
			logger.warn(String.format("Message with id '%s' was not deleted.", id));
		}

		return getMessageGroup(groupId);
	}

	/**
	 * <p>Remove a Message Id from the idCache. Should be used in conjunction
	 * with the Spring Integration Transaction Synchronization feature to remove
	 * a message from the Message Id cache once a transaction either succeeded or
	 * rolled back.</p>
	 * <p>Only applicable if {@link #setUsingIdCache(boolean)} is set to
	 * <code>true</code></p>.
	 *
	 * @param messageId
	 */
	public void removeFromIdCache(String messageId) {
		if (logger.isDebugEnabled()) {
			logger.debug("Removing Message Id:" + messageId);
		}
		this.idCache.remove(messageId);
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

	/**
	 * Will remove all messages from the message channel.
	 */
	public void removeMessageGroup(Object groupId) {

		final String groupKey = getKey(groupId);

		jdbcTemplate.update(getQuery(queryProvider.getDeleteMessageGroupQuery()), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Marking messages with group key=" + groupKey);
				}
				ps.setString(1, groupKey);
				ps.setString(2, region);
			}
		});

	}

}
