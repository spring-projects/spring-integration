/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jdbc.store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageMetadata;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link MessageStore} using a relational database via JDBC. SQL scripts to create the necessary
 * tables are packaged as {@code org/springframework/integration/jdbc/schema-*.sql}, where {@code *} is the
 * target database type.
 * <p>
 * If you intend backing a {@link org.springframework.messaging.MessageChannel}
 * using a JDBC-based Message Store,
 * please consider using the channel-specific {@link JdbcChannelMessageStore} instead.
 * This implementation is intended for correlation components (e.g. {@code <aggregator>}),
 * {@code <delayer>} and similar.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Matt Stine
 * @author Gunnar Hillert
 * @author Will Schipp
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JdbcMessageStore extends AbstractMessageGroupStore implements MessageStore {

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	private enum Query {
		CREATE_MESSAGE_GROUP("INSERT into %PREFIX%MESSAGE_GROUP" +
				"(GROUP_KEY, REGION, COMPLETE, LAST_RELEASED_SEQUENCE, CREATED_DATE, UPDATED_DATE)"
				+ " values (?, ?, 0, 0, ?, ?)"),

		UPDATE_MESSAGE_GROUP("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, CONDITION=? " +
				"where GROUP_KEY=? and REGION=?"),

		REMOVE_MESSAGE_FROM_GROUP("DELETE from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and MESSAGE_ID=? and " +
				"REGION=?"),

		REMOVE_GROUP_TO_MESSAGE_JOIN("DELETE from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and REGION=?"),

		COUNT_ALL_MESSAGES_IN_GROUPS("SELECT COUNT(MESSAGE_ID) from %PREFIX%GROUP_TO_MESSAGE where REGION=?"),

		COUNT_ALL_MESSAGES_IN_GROUP("SELECT COUNT(MESSAGE_ID) from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and " +
				"REGION=?"),

		LIST_MESSAGES_BY_GROUP_KEY("SELECT MESSAGE_ID, MESSAGE_BYTES, CREATED_DATE " +
				"from %PREFIX%MESSAGE where MESSAGE_ID in " +
				"(SELECT MESSAGE_ID from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY = ? and REGION = ?) and REGION = ? " +
				"ORDER BY CREATED_DATE"),

		POLL_FROM_GROUP("SELECT %PREFIX%MESSAGE.MESSAGE_ID, %PREFIX%MESSAGE.MESSAGE_BYTES from %PREFIX%MESSAGE " +
				"where %PREFIX%MESSAGE.MESSAGE_ID = " +
				"(SELECT min(m.MESSAGE_ID) from %PREFIX%MESSAGE m " +
				"join %PREFIX%GROUP_TO_MESSAGE on m.MESSAGE_ID = %PREFIX%GROUP_TO_MESSAGE.MESSAGE_ID " +
				"where CREATED_DATE = " +
				"(SELECT min(CREATED_DATE) from %PREFIX%MESSAGE, %PREFIX%GROUP_TO_MESSAGE " +
				"where %PREFIX%MESSAGE.MESSAGE_ID = %PREFIX%GROUP_TO_MESSAGE.MESSAGE_ID " +
				"and %PREFIX%GROUP_TO_MESSAGE.GROUP_KEY = ? " +
				"and %PREFIX%MESSAGE.REGION = ?) " +
				"and %PREFIX%GROUP_TO_MESSAGE.GROUP_KEY = ? " +
				"and m.REGION = ?)"),

		GET_GROUP_INFO("SELECT COMPLETE, LAST_RELEASED_SEQUENCE, CREATED_DATE, UPDATED_DATE, CONDITION" +
				" from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=?"),

		GET_MESSAGE("SELECT MESSAGE_ID, CREATED_DATE, MESSAGE_BYTES from %PREFIX%MESSAGE where MESSAGE_ID=? and " +
				"REGION=?"),

		GET_MESSAGE_COUNT("SELECT COUNT(MESSAGE_ID) from %PREFIX%MESSAGE where REGION=?"),

		DELETE_MESSAGE("DELETE from %PREFIX%MESSAGE where MESSAGE_ID=? and REGION=?"),

		CREATE_MESSAGE("INSERT into %PREFIX%MESSAGE(MESSAGE_ID, REGION, CREATED_DATE, MESSAGE_BYTES)"
				+ " values (?, ?, ?, ?)"),

		COUNT_ALL_GROUPS("SELECT COUNT(GROUP_KEY) from %PREFIX%MESSAGE_GROUP where REGION=?"),

		COMPLETE_GROUP("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, COMPLETE=1 where GROUP_KEY=? and REGION=?"),

		UPDATE_LAST_RELEASED_SEQUENCE("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, LAST_RELEASED_SEQUENCE=? where " +
				"GROUP_KEY=? and REGION=?"),

		DELETE_MESSAGES_FROM_GROUP("DELETE from %PREFIX%MESSAGE where MESSAGE_ID in " +
				"(SELECT MESSAGE_ID from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY = ? and REGION = ?) and REGION = ?"),

		DELETE_MESSAGE_GROUP("DELETE from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=?"),

		CREATE_GROUP_TO_MESSAGE("INSERT into %PREFIX%GROUP_TO_MESSAGE" +
				"(GROUP_KEY, MESSAGE_ID, REGION)"
				+ " values (?, ?, ?)"),

		UPDATE_GROUP("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=? where GROUP_KEY=? and REGION=?"),

		LIST_GROUP_KEYS("SELECT distinct GROUP_KEY as CREATED from %PREFIX%MESSAGE_GROUP where REGION=?");

		private final String sql;

		Query(String sql) {
			this.sql = sql;
		}

		public String getSql() {
			return this.sql;
		}
	}

	private final MessageMapper mapper = new MessageMapper();

	private final JdbcOperations jdbcTemplate;

	private final Map<Query, String> queryCache = new HashMap<>();

	private String region = "DEFAULT";

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private AllowListDeserializingConverter deserializer;

	private SerializingConverter serializer;

	private LobHandler lobHandler = new DefaultLobHandler();

	/**
	 * Create a {@link MessageStore} with all mandatory properties.
	 * @param dataSource a {@link DataSource}
	 */
	public JdbcMessageStore(DataSource dataSource) {
		this(new JdbcTemplate(dataSource));
	}

	/**
	 * Create a {@link MessageStore} with all mandatory properties.
	 * @param jdbcOperations a {@link JdbcOperations}
	 * @since 4.3.9
	 */
	public JdbcMessageStore(JdbcOperations jdbcOperations) {
		Assert.notNull(jdbcOperations, "'dataSource' must not be null");
		this.jdbcTemplate = jdbcOperations;
		this.deserializer = new AllowListDeserializingConverter();
		this.serializer = new SerializingConverter();
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all the table names before queries are
	 * executed. Defaults to {@link #DEFAULT_TABLE_PREFIX}.
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * A unique grouping identifier for all messages persisted with this store. Using multiple regions allows the store
	 * to be partitioned (if necessary) for different purposes. Defaults to {@code DEFAULT}.
	 * @param region the region name to set
	 */
	public void setRegion(String region) {
		Assert.hasText(region, "Region must not be null or empty.");
		this.region = region;
	}

	/**
	 * Override the {@link LobHandler} that is used to create and unpack large objects in SQL queries. The default is
	 * fine for almost all platforms, but some Oracle drivers require a native implementation.
	 * @param lobHandler a {@link LobHandler}
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * A converter for serializing messages to byte arrays for storage.
	 * @param serializer the serializer to set
	 */
	@SuppressWarnings("unchecked")
	public void setSerializer(Serializer<? super Message<?>> serializer) {
		this.serializer = new SerializingConverter((Serializer<Object>) serializer);
	}

	/**
	 * A converter for deserializing byte arrays to messages.
	 * @param deserializer the deserializer to set
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setDeserializer(Deserializer<? extends Message<?>> deserializer) {
		this.deserializer = new AllowListDeserializingConverter((Deserializer) deserializer);
	}

	/**
	 * Add patterns for packages/classes that are allowed to be deserialized. A class can
	 * be fully qualified or a wildcard '*' is allowed at the beginning or end of the
	 * class name. Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param patterns the patterns.
	 * @since 5.4
	 */
	public void addAllowedPatterns(String... patterns) {
		this.deserializer.addAllowedPatterns(patterns);
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Message<?> message = getMessage(id);
		if (message == null) {
			return null;
		}
		int updated = this.jdbcTemplate.update(getQuery(Query.DELETE_MESSAGE), getKey(id), this.region);
		if (updated != 0) {
			return message;
		}
		return null;
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		return this.jdbcTemplate.queryForObject(getQuery(Query.GET_MESSAGE_COUNT), // NOSONAR query never returns null
				Long.class, this.region);
	}

	@Override
	public Message<?> getMessage(UUID id) {
		List<Message<?>> list =
				this.jdbcTemplate.query(getQuery(Query.GET_MESSAGE), this.mapper, getKey(id), this.region);
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Override
	public MessageMetadata getMessageMetadata(UUID id) {
		List<MessageMetadata> list =
				this.jdbcTemplate.query(getQuery(Query.GET_MESSAGE),
						(rs, rn) -> {
							MessageMetadata messageMetadata =
									new MessageMetadata(UUID.fromString(rs.getString("MESSAGE_ID")));
							messageMetadata.setTimestamp(rs.getTimestamp("CREATED_DATE").getTime());
							return messageMetadata;
						}, getKey(id), this.region);
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Message<T> addMessage(final Message<T> message) {
		UUID id = message.getHeaders().getId();
		Assert.notNull(id, "Cannot store messages without an ID header");
		final String messageId = getKey(id);
		final byte[] messageBytes = this.serializer.convert(message);

		if (logger.isDebugEnabled()) {
			logger.debug("Inserting message with id key=" + messageId);
		}

		try {
			this.jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE), ps -> {
				ps.setString(1, messageId); // NOSONAR - magic number
				ps.setString(2, this.region); // NOSONAR - magic number
				ps.setTimestamp(3, new Timestamp(System.currentTimeMillis())); // NOSONAR - magic number

				this.lobHandler.getLobCreator().setBlobAsBytes(ps, 4, messageBytes); // NOSONAR - magic number
			});
		}
		catch (DuplicateKeyException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("The Message with id [" + id + "] already exists.\n" +
						"Ignoring INSERT and SELECT existing...");
			}
			return (Message<T>) getMessage(id);
		}
		return message;
	}

	@Override
	public void addMessagesToGroup(Object groupId, Message<?>... messages) {
		String groupKey = getKey(groupId);
		Map<String, Object> groupInfo = getGroupMetadata(groupKey);

		Timestamp updatedDate = new Timestamp(System.currentTimeMillis());
		boolean groupNotExist = groupInfo.isEmpty();
		Timestamp createdDate = groupNotExist ? updatedDate : (Timestamp) groupInfo.get("CREATED_DATE");

		for (Message<?> message : messages) {
			addMessage(message);
		}
		this.jdbcTemplate.batchUpdate(getQuery(Query.CREATE_GROUP_TO_MESSAGE),
				Arrays.asList(messages),
				100,
				(ps, messageToAdd) -> {
					String messageId = getKey(messageToAdd.getHeaders().getId());
					if (logger.isDebugEnabled()) {
						logger.debug("Inserting message with id key=" + messageId +
								" and created date=" + createdDate);
					}
					ps.setString(1, groupKey); // NOSONAR - magic number
					ps.setString(2, messageId); // NOSONAR - magic number
					ps.setString(3, JdbcMessageStore.this.region); // NOSONAR - magic number
				});

		if (groupNotExist) {
			try {
				doCreateMessageGroup(groupKey, createdDate);
			}
			catch (DuplicateKeyException e) {
				logger.warn("Lost race to create group; attempting update instead", e);
				updateMessageGroup(groupKey);
			}
		}
		else {
			updateMessageGroup(groupKey);
		}
	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		return this.jdbcTemplate.queryForObject(getQuery(Query.COUNT_ALL_GROUPS), // NOSONAR query never returns null
				Integer.class, this.region);
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		return this.jdbcTemplate
				.queryForObject(getQuery(Query.COUNT_ALL_MESSAGES_IN_GROUPS), // NOSONAR query never returns null
						Integer.class, this.region);
	}

	@Override
	@ManagedAttribute
	public int messageGroupSize(Object groupId) {
		String key = getKey(groupId);
		return this.jdbcTemplate
				.queryForObject(getQuery(Query.COUNT_ALL_MESSAGES_IN_GROUP), // NOSONAR query never returns null
						Integer.class, key, this.region);
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		String key = getKey(groupId);
		Map<String, Object> groupInfo = getGroupMetadata(key);

		if (groupInfo.isEmpty()) {
			return new SimpleMessageGroup(groupId);
		}

		MessageGroup messageGroup = getMessageGroupFactory()
				.create(this, groupId, ((Timestamp) groupInfo.get("CREATED_DATE")).getTime(),
						((Long) groupInfo.get("COMPLETE")) > 0);
		messageGroup.setLastModified(((Timestamp) groupInfo.get("UPDATED_DATE")).getTime());
		messageGroup.setLastReleasedMessageSequenceNumber(((Long) groupInfo.get("LAST_RELEASED_SEQUENCE")).intValue());
		messageGroup.setCondition((String) groupInfo.get("CONDITION"));
		return messageGroup;
	}

	private Map<String, Object> getGroupMetadata(String groupKey) {
		try {
			return this.jdbcTemplate.queryForMap(getQuery(Query.GET_GROUP_INFO), groupKey, this.region);
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			return Collections.emptyMap();
		}
	}

	@Override
	public void removeMessagesFromGroup(Object groupId, Collection<Message<?>> messages) {
		Assert.notNull(groupId, "'groupId' must not be null");
		Assert.notNull(messages, "'messages' must not be null");

		final String groupKey = getKey(groupId);

		if (logger.isDebugEnabled()) {
			logger.debug("Removing messages from group with group key=" + groupKey);
		}
		this.jdbcTemplate.batchUpdate(getQuery(Query.REMOVE_MESSAGE_FROM_GROUP),
				messages,
				getRemoveBatchSize(),
				(ps, messageToRemove) -> {
					ps.setString(1, groupKey); // NOSONAR - magic number
					ps.setString(2, getKey(messageToRemove.getHeaders().getId())); // NOSONAR - magic number
					ps.setString(3, JdbcMessageStore.this.region); // NOSONAR - magic number
				});

		this.jdbcTemplate.batchUpdate(getQuery(Query.DELETE_MESSAGE),
				messages,
				getRemoveBatchSize(),
				(ps, messageToRemove) -> {
					ps.setString(1, getKey(messageToRemove.getHeaders().getId())); // NOSONAR - magic number
					ps.setString(2, JdbcMessageStore.this.region); // NOSONAR - magic number
				});

		updateMessageGroup(groupKey);
	}

	@Override
	public void removeMessageGroup(Object groupId) {
		String groupKey = getKey(groupId);

		this.jdbcTemplate.update(getQuery(Query.DELETE_MESSAGES_FROM_GROUP), groupKey, this.region, this.region);

		if (logger.isDebugEnabled()) {
			logger.debug("Removing relationships for the group with group key=" + groupKey);
		}
		this.jdbcTemplate.update(getQuery(Query.REMOVE_GROUP_TO_MESSAGE_JOIN), groupKey, this.region);

		if (logger.isDebugEnabled()) {
			logger.debug("Deleting messages with group key=" + groupKey);
		}

		this.jdbcTemplate.update(getQuery(Query.DELETE_MESSAGE_GROUP), groupKey, this.region);
	}

	@Override
	public void completeGroup(Object groupId) {
		final String groupKey = getKey(groupId);

		if (logger.isDebugEnabled()) {
			logger.debug("Completing MessageGroup: " + groupKey);
		}
		this.jdbcTemplate.update(getQuery(Query.COMPLETE_GROUP),
				new Timestamp(System.currentTimeMillis()), groupKey, this.region);
	}

	@Override
	public void setGroupCondition(Object groupId, String condition) {
		Assert.notNull(groupId, "'groupId' must not be null");
		String groupKey = getKey(groupId);
		Timestamp updatedDate = new Timestamp(System.currentTimeMillis());
		if (logger.isDebugEnabled()) {
			logger.debug("Updating message group with id key=" + groupKey + " and updated date=" + updatedDate);
		}
		this.jdbcTemplate.update(getQuery(Query.UPDATE_MESSAGE_GROUP), updatedDate, condition, groupKey, this.region);
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, int sequenceNumber) {
		Assert.notNull(groupId, "'groupId' must not be null");
		String groupKey = getKey(groupId);

		if (logger.isDebugEnabled()) {
			logger.debug("Updating  the sequence number of the last released Message in the MessageGroup: " + groupKey);
		}
		this.jdbcTemplate.update(getQuery(Query.UPDATE_LAST_RELEASED_SEQUENCE),
				new Timestamp(System.currentTimeMillis()), sequenceNumber, groupKey, this.region);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		String key = getKey(groupId);

		Message<?> polledMessage = doPollForMessage(key);
		if (polledMessage != null) {
			removeMessagesFromGroup(groupId, polledMessage);
		}
		return polledMessage;
	}

	@Override
	public Message<?> getOneMessageFromGroup(Object groupId) {
		return doPollForMessage(getKey(groupId));
	}

	@Override
	public Collection<Message<?>> getMessagesForGroup(Object groupId) {
		return this.jdbcTemplate.query(getQuery(Query.LIST_MESSAGES_BY_GROUP_KEY), this.mapper, getKey(groupId),
				this.region, this.region);
	}

	@Override
	public Stream<Message<?>> streamMessagesForGroup(Object groupId) {
		return this.jdbcTemplate.queryForStream(getQuery(Query.LIST_MESSAGES_BY_GROUP_KEY), this.mapper,
				getKey(groupId), this.region, this.region);
	}

	@Override
	public Iterator<MessageGroup> iterator() {

		final Iterator<String> iterator = this.jdbcTemplate.query(getQuery(Query.LIST_GROUP_KEYS),
				new SingleColumnRowMapper<String>(), this.region)
				.iterator();

		return new Iterator<MessageGroup>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public MessageGroup next() {
				return getMessageGroup(iterator.next());
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Cannot remove MessageGroup from this iterator.");
			}

		};
	}

	/**
	 * Replace patterns in the input to produce a valid SQL query. This implementation lazily initializes a
	 * simple map-based cache, only replacing the table prefix on the first access to a named query. Further
	 * accesses will be resolved from the cache.
	 * @param base the SQL query to be transformed
	 * @return a transformed query with replacements
	 */
	protected String getQuery(Query base) {
		String query = this.queryCache.get(base);

		if (query == null) {
			query = StringUtils.replace(base.getSql(), "%PREFIX%", this.tablePrefix);
			this.queryCache.put(base, query);
		}

		return query;
	}

	/**
	 * To be used to get a reference to JdbcOperations
	 * in case this class is subclassed
	 * @return the JdbcOperations implementation
	 */
	protected JdbcOperations getJdbcOperations() {
		return this.jdbcTemplate;
	}

	/**
	 * This method executes a call to the DB to get the oldest Message in the MessageGroup
	 * Override this method if need to. For example if your DB supports advanced function such as FIRST etc.
	 * @param groupIdKey String representation of message group ID
	 * @return a message; could be null if query produced no Messages
	 */
	protected Message<?> doPollForMessage(String groupIdKey) {
		List<Message<?>> messages = this.jdbcTemplate.query(getQuery(Query.POLL_FROM_GROUP), this.mapper,
				groupIdKey, this.region, groupIdKey, this.region);
		Assert.state(messages.size() < 2,
				() -> "The query must return zero or 1 row; got " + messages.size() + " rows");
		if (messages.size() > 0) {
			return messages.get(0);
		}
		return null;
	}

	private void doCreateMessageGroup(String groupKey, Timestamp createdDate) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating message group with id key=" + groupKey + " and created date=" + createdDate);
		}
		this.jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE_GROUP), groupKey, this.region, createdDate, createdDate);
	}

	private void updateMessageGroup(String groupId) {
		if (logger.isDebugEnabled()) {
			logger.debug("Updating MessageGroup: " + groupId);
		}
		this.jdbcTemplate.update(getQuery(Query.UPDATE_GROUP),
				new Timestamp(System.currentTimeMillis()), groupId, this.region);
	}

	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	/**
	 * Convenience class to be used to unpack a message from a result set row. Uses column named in the result set to
	 * extract the required data, so that select clause ordering is unimportant.
	 */
	private class MessageMapper implements RowMapper<Message<?>> {

		@Override
		public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
			byte[] messageBytes = JdbcMessageStore.this.lobHandler.getBlobAsBytes(rs, "MESSAGE_BYTES");
			if (messageBytes == null) {
				return null;
			}
			else {
				return (Message<?>) JdbcMessageStore.this.deserializer.convert(messageBytes);
			}
		}

	}

}
