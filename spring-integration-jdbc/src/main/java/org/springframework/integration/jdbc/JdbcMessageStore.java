/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link MessageStore} using a relational database via JDBC. SQL scripts to create the necessary
 * tables are packaged as <code>org/springframework/integration/jdbc/schema-*.sql</code>, where <code>*</code> is the
 * target database type.
 *
 * Notice: Starting with Spring Integration 3.0, this class will move to package:
 * <code>org.springframework.integration.jdbc.store</code>.
 *
 * If you intend backing a {@link MessageChannel} using a JDBC-based Message Store,
 * please consider using the channel-specific {@link JdbcChannelMessageStore} instead.
 *
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Matt Stine
 * @author Gunnar Hillert
 * @author Will Schipp
 *
 * @since 2.0
 */
@ManagedResource
public class JdbcMessageStore extends AbstractMessageGroupStore implements MessageStore, InitializingBean {

	private static final Log logger = LogFactory.getLog(JdbcMessageStore.class);

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	private enum Query {
		GROUP_EXISTS("SELECT COUNT(GROUP_KEY) FROM %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=?"),

		CREATE_MESSAGE_GROUP("INSERT into %PREFIX%MESSAGE_GROUP" +
			"(GROUP_KEY, REGION, MARKED, COMPLETE, LAST_RELEASED_SEQUENCE, CREATED_DATE, UPDATED_DATE)"
			+ " values (?, ?, 0, 0, 0, ?, ?)"),

		UPDATE_MESSAGE_GROUP("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=? where GROUP_KEY=? and REGION=?"),

		REMOVE_MESSAGE_FROM_GROUP("DELETE from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and MESSAGE_ID=? and REGION=?"),

		REMOVE_GROUP_TO_MESSAGE_JOIN("DELETE from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and REGION=?"),

		COUNT_ALL_MESSAGES_IN_GROUPS("SELECT COUNT(MESSAGE_ID) from %PREFIX%GROUP_TO_MESSAGE where REGION=?"),

		COUNT_ALL_MESSAGES_IN_GROUP("SELECT COUNT(MESSAGE_ID) from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and REGION=?"),

		LIST_MESSAGEIDS_BY_GROUP_KEY("select MESSAGE_ID, CREATED_DATE " +
				"from %PREFIX%MESSAGE where MESSAGE_ID in (select MESSAGE_ID from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY=? and REGION=?) " +
				"ORDER BY CREATED_DATE"),

		LIST_MESSAGES_BY_GROUP_KEY("SELECT MESSAGE_ID, MESSAGE_BYTES, CREATED_DATE " +
				"from %PREFIX%MESSAGE where MESSAGE_ID in (SELECT MESSAGE_ID from %PREFIX%GROUP_TO_MESSAGE where GROUP_KEY = ?) and REGION=? " +
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

		GET_GROUP_INFO("SELECT COMPLETE, LAST_RELEASED_SEQUENCE, CREATED_DATE, UPDATED_DATE" +
				" from %PREFIX%MESSAGE_GROUP where GROUP_KEY = ? and REGION=?"),

		GET_MESSAGE("SELECT MESSAGE_ID, CREATED_DATE, MESSAGE_BYTES from %PREFIX%MESSAGE where MESSAGE_ID=? and REGION=?"),

		GET_GROUP_CREATED_DATE("SELECT CREATED_DATE from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=?"),

		GET_MESSAGE_COUNT("SELECT COUNT(MESSAGE_ID) from %PREFIX%MESSAGE where REGION=?"),

		DELETE_MESSAGE("DELETE from %PREFIX%MESSAGE where MESSAGE_ID=? and REGION=?"),

		CREATE_MESSAGE("INSERT into %PREFIX%MESSAGE(MESSAGE_ID, REGION, CREATED_DATE, MESSAGE_BYTES)"
				+ " values (?, ?, ?, ?)"),

		COUNT_ALL_GROUPS("SELECT COUNT(GROUP_KEY) from %PREFIX%MESSAGE_GROUP where REGION=?"),

		COMPLETE_GROUP("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, COMPLETE=1 where GROUP_KEY=? and REGION=?"),

		UPDATE_LAST_RELEASED_SEQUENCE("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, LAST_RELEASED_SEQUENCE=? where GROUP_KEY=? and REGION=?"),

		DELETE_MESSAGE_GROUP("DELETE from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=?"),

		CREATE_GROUP_TO_MESSAGE("INSERT into %PREFIX%GROUP_TO_MESSAGE" +
				"(GROUP_KEY, MESSAGE_ID, REGION)"
				+ " values (?, ?, ?)"),

		UPDATE_GROUP("UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=? where GROUP_KEY=? and REGION=?"),

		LIST_GROUP_KEYS("SELECT distinct GROUP_KEY as CREATED from %PREFIX%MESSAGE_GROUP where REGION=?");

		private String sql;

		Query(String sql) {
			this.sql = sql;
		}

		public String getSql() {
			return sql;
		}
	}

	public static final int DEFAULT_LONG_STRING_LENGTH = 2500;

	/**
	 * The name of the message header that stores a flag to indicate that the message has been saved. This is an
	 * optimization for the put method.
	 */
	public static final String SAVED_KEY = JdbcMessageStore.class.getSimpleName() + ".SAVED";

	/**
	 * The name of the message header that stores a timestamp for the time the message was inserted.
	 */
	public static final String CREATED_DATE_KEY = JdbcMessageStore.class.getSimpleName() + ".CREATED_DATE";

	private volatile String region = "DEFAULT";

	private volatile String tablePrefix = DEFAULT_TABLE_PREFIX;

	private volatile JdbcOperations jdbcTemplate;

	private volatile DeserializingConverter deserializer;

	private volatile SerializingConverter serializer;

	private volatile LobHandler lobHandler = new DefaultLobHandler();

	private volatile MessageMapper mapper = new MessageMapper();

	private volatile Map<Query, String> queryCache = new HashMap<Query, String>();

	/**
	 * Convenient constructor for configuration use.
	 */
	public JdbcMessageStore() {
		deserializer = new DeserializingConverter();
		serializer = new SerializingConverter();
	}

	/**
	 * Create a {@link MessageStore} with all mandatory properties.
	 *
	 * @param dataSource a {@link DataSource}
	 */
	public JdbcMessageStore(DataSource dataSource) {
		this();
		jdbcTemplate = new JdbcTemplate(dataSource);
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
	 * A unique grouping identifier for all messages persisted with this store. Using multiple regions allows the store
	 * to be partitioned (if necessary) for different purposes. Defaults to <code>DEFAULT</code>.
	 *
	 * @param region the region name to set
	 */
	public void setRegion(String region) {
		Assert.hasText(region, "Region must not be null or empty.");
		this.region = region;
	}

	/**
	 * The JDBC {@link DataSource} to use when interacting with the database. Either this property can be set or the
	 * {@link #setJdbcTemplate(JdbcOperations) jdbcTemplate}.
	 *
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * The {@link JdbcOperations} to use when interacting with the database. Either this property can be set or the
	 * {@link #setDataSource(DataSource) dataSource}.
	 *
	 * @param jdbcTemplate a {@link JdbcOperations}
	 */
	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Override the {@link LobHandler} that is used to create and unpack large objects in SQL queries. The default is
	 * fine for almost all platforms, but some Oracle drivers require a native implementation.
	 *
	 * @param lobHandler a {@link LobHandler}
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * A converter for serializing messages to byte arrays for storage.
	 *
	 * @param serializer the serializer to set
	 */
	@SuppressWarnings("unchecked")
	public void setSerializer(Serializer<? super Message<?>> serializer) {
		this.serializer = new SerializingConverter((Serializer<Object>) serializer);
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

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
	}

	@Override
	public Message<?> removeMessage(UUID id) {
		Message<?> message = getMessage(id);
		if (message == null) {
			return null;
		}
		int updated = jdbcTemplate.update(getQuery(Query.DELETE_MESSAGE), new Object[] { getKey(id), region }, new int[] {
				Types.VARCHAR, Types.VARCHAR });
		if (updated != 0) {
			return message;
		}
		return null;
	}

	@Override
	@ManagedAttribute
	public long getMessageCount() {
		return jdbcTemplate.queryForObject(getQuery(Query.GET_MESSAGE_COUNT), Integer.class, region);
	}

	@Override
	public Message<?> getMessage(UUID id) {
		List<Message<?>> list = jdbcTemplate.query(getQuery(Query.GET_MESSAGE), new Object[] { getKey(id), region }, mapper);
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T> Message<T> addMessage(final Message<T> message) {
		if (message.getHeaders().containsKey(SAVED_KEY)) {
			Message<T> saved = (Message<T>) getMessage(message.getHeaders().getId());
			if (saved != null) {
				if (saved.equals(message)) {
					return message;
				} // We need to save it under its own id
			}
		}

		final long createdDate = System.currentTimeMillis();
		Message<T> result = this.getMessageBuilderFactory().fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE)
				.setHeader(CREATED_DATE_KEY, new Long(createdDate)).build();

		Map innerMap = (Map) new DirectFieldAccessor(result.getHeaders()).getPropertyValue("headers");
		// using reflection to set ID since it is immutable through MessageHeaders
		innerMap.put(MessageHeaders.ID, message.getHeaders().get(MessageHeaders.ID));

		final String messageId = getKey(result.getHeaders().getId());
		final byte[] messageBytes = serializer.convert(result);

		jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Inserting message with id key=" + messageId);
				}
				ps.setString(1, messageId);
				ps.setString(2, region);
				ps.setTimestamp(3, new Timestamp(createdDate));
				lobHandler.getLobCreator().setBlobAsBytes(ps, 4, messageBytes);
			}
		});
		return result;
	}

	@Override
	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {
		final String groupKey = getKey(groupId);
		final String messageId = getKey(message.getHeaders().getId());
		boolean groupNotExist = jdbcTemplate.queryForObject(this.getQuery(Query.GROUP_EXISTS), Integer.class, groupKey, region) < 1;

		final Timestamp updatedDate = new Timestamp(System.currentTimeMillis());

		final Timestamp createdDate = groupNotExist ?
				updatedDate :
			    jdbcTemplate.queryForObject(getQuery(Query.GET_GROUP_CREATED_DATE), new Object[] { groupKey, region}, Timestamp.class);

		if (groupNotExist){
			try {
				this.doCreateMessageGroup(groupKey, createdDate);
			} catch (DuplicateKeyException e) {
				logger.warn("Lost race to create group; attempting update instead", e);
				this.doUpdateMessageGroup(groupKey, updatedDate);
			}
		}
		else {
			this.doUpdateMessageGroup(groupKey, updatedDate);
		}

		this.addMessage(message);

		jdbcTemplate.update(getQuery(Query.CREATE_GROUP_TO_MESSAGE), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Inserting message with id key=" + messageId + " and created date=" + createdDate);
				}
				ps.setString(1, groupKey);
				ps.setString(2, messageId);
				ps.setString(3, region);
			}
		});
		return getMessageGroup(groupId);

	}

	@Override
	@ManagedAttribute
	public int getMessageGroupCount() {
		return jdbcTemplate.queryForObject(getQuery(Query.COUNT_ALL_GROUPS), Integer.class, region);
	}

	@Override
	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		return jdbcTemplate.queryForObject(getQuery(Query.COUNT_ALL_MESSAGES_IN_GROUPS), Integer.class, region);
	}

	@Override
	@ManagedAttribute
	public int messageGroupSize(Object groupId) {
		String key = getKey(groupId);
		return jdbcTemplate.queryForObject(getQuery(Query.COUNT_ALL_MESSAGES_IN_GROUP), Integer.class, key, region);
	}

	@Override
	public MessageGroup getMessageGroup(Object groupId) {
		String key = getKey(groupId);
		final AtomicReference<Date> createDate = new AtomicReference<Date>();
		final AtomicReference<Date> updateDate = new AtomicReference<Date>();
		final AtomicReference<Boolean> completeFlag = new AtomicReference<Boolean>();
		final AtomicReference<Integer> lastReleasedSequenceRef = new AtomicReference<Integer>();

		List<Message<?>> messages = jdbcTemplate.query(getQuery(Query.LIST_MESSAGES_BY_GROUP_KEY), new Object[] { key, region }, mapper);

		jdbcTemplate.query(getQuery(Query.GET_GROUP_INFO), new Object[] { key, region},
				new RowCallbackHandler() {
					@Override
					public void processRow(ResultSet rs) throws SQLException {
						updateDate.set(rs.getTimestamp("UPDATED_DATE"));

						createDate.set(rs.getTimestamp("CREATED_DATE"));

						completeFlag.set(rs.getInt("COMPLETE") > 0);

						lastReleasedSequenceRef.set(rs.getInt("LAST_RELEASED_SEQUENCE"));
					}
				});

		if (createDate.get() == null && updateDate.get() == null) {
			if (logger.isWarnEnabled()) {
				for (Message<?> message : messages) {
					logger.warn("Missing group row for message id: " + message.getHeaders().getId());
				}
			}
			return new SimpleMessageGroup(groupId);
		}

		long timestamp = createDate.get().getTime();
		boolean complete = completeFlag.get().booleanValue();

		SimpleMessageGroup messageGroup = new SimpleMessageGroup(messages, groupId, timestamp, complete);
		messageGroup.setLastModified(updateDate.get().getTime());

		int lastReleasedSequenceNumber = lastReleasedSequenceRef.get();
		messageGroup.setLastReleasedMessageSequenceNumber(lastReleasedSequenceNumber);

		return messageGroup;
	}

	@Override
	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		final String groupKey = getKey(groupId);
		final String messageId = getKey(messageToRemove.getHeaders().getId());

		jdbcTemplate.update(getQuery(Query.REMOVE_MESSAGE_FROM_GROUP), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Removing message from group with group key=" + groupKey);
				}
				ps.setString(1, groupKey);
				ps.setString(2, messageId);
				ps.setString(3, region);
			}
		});
		this.removeMessage(messageToRemove.getHeaders().getId());
		this.updateMessageGroup(groupKey);
		return getMessageGroup(groupId);
	}

	@Override
	public void removeMessageGroup(Object groupId) {

		final String groupKey = getKey(groupId);

		for (UUID messageIds : this.getMessageIdsForGroup(groupId)) {
			this.removeMessage(messageIds);
		}

		jdbcTemplate.update(getQuery(Query.REMOVE_GROUP_TO_MESSAGE_JOIN), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Removing relationships for the group with group key=" + groupKey);
				}
				ps.setString(1, groupKey);
				ps.setString(2, region);
			}
		});

		jdbcTemplate.update(getQuery(Query.DELETE_MESSAGE_GROUP), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Marking messages with group key=" + groupKey);
				}
				ps.setString(1, groupKey);
				ps.setString(2, region);
			}
		});
	}

	@Override
	public void completeGroup(Object groupId) {
		final long updatedDate = System.currentTimeMillis();
		final String groupKey = getKey(groupId);

		jdbcTemplate.update(getQuery(Query.COMPLETE_GROUP), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Completing MessageGroup: " + groupKey);
				}
				ps.setTimestamp(1, new Timestamp(updatedDate));
				ps.setString(2, groupKey);
				ps.setString(3, region);
			}
		});
	}

	@Override
	public void setLastReleasedSequenceNumberForGroup(Object groupId, final int sequenceNumber) {
		Assert.notNull(groupId, "'groupId' must not be null");
		final long updatedDate = System.currentTimeMillis();
		final String groupKey = getKey(groupId);

		jdbcTemplate.update(getQuery(Query.UPDATE_LAST_RELEASED_SEQUENCE), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Updating  the sequence number of the last released Message in the MessageGroup: " + groupKey);
				}
				ps.setTimestamp(1, new Timestamp(updatedDate));
				ps.setInt(2, sequenceNumber);
				ps.setString(3, groupKey);
				ps.setString(4, region);
			}
		});
		this.updateMessageGroup(groupKey);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		String key = getKey(groupId);

		Message<?> polledMessage = this.doPollForMessage(key);
		if (polledMessage != null){
			this.removeMessageFromGroup(groupId, polledMessage);
		}
		return polledMessage;
	}

	@Override
	public Iterator<MessageGroup> iterator() {

		final Iterator<String> iterator = jdbcTemplate.query(getQuery(Query.LIST_GROUP_KEYS), new Object[] { region },
				new SingleColumnRowMapper<String>()).iterator();

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
	 *
	 * @param base the SQL query to be transformed
	 * @return a transformed query with replacements
	 */
	protected String getQuery(Query base) {
		String query = queryCache.get(base);

		if (query == null) {
			query = StringUtils.replace(base.getSql(), "%PREFIX%", tablePrefix);
			queryCache.put(base, query);
		}

		return query;
	}

	/**
	 * To be used to get a reference to JdbcOperations
	 * in case this class is subclassed
	 *
	 * @return the JdbcOperations implementation
	 */
	protected JdbcOperations getJdbcOperations(){
		return this.jdbcTemplate;
	}

	/**
	 * This method executes a call to the DB to get the oldest Message in the MessageGroup
	 * Override this method if need to. For example if you DB supports advanced function such as FIRST etc.
	 *
	 * @param groupIdKey String representation of message group ID
	 * @return a message; could be null if query produced no Messages
	 */
	protected Message<?> doPollForMessage(String groupIdKey) {
		List<Message<?>> messages = jdbcTemplate.query(getQuery(Query.POLL_FROM_GROUP), new Object[] { groupIdKey, region, groupIdKey, region }, mapper);
		Assert.isTrue(messages.size() == 0 || messages.size() == 1);
		if (messages.size() > 0){
			return messages.get(0);
		}
		return null;
	}

	private void doCreateMessageGroup(final String groupKey, final Timestamp createdDate){
		jdbcTemplate.update(getQuery(Query.CREATE_MESSAGE_GROUP), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Creating message group with id key=" + groupKey + " and created date=" + createdDate);
				}
				ps.setString(1, groupKey);
				ps.setString(2, region);
				ps.setTimestamp(3, createdDate);
				ps.setTimestamp(4, createdDate);
			}
		});
	}

	private void doUpdateMessageGroup(final String groupKey, final Timestamp updatedDate){
		jdbcTemplate.update(getQuery(Query.UPDATE_MESSAGE_GROUP), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Updating message group with id key=" + groupKey + " and updated date=" + updatedDate);
				}
				ps.setTimestamp(1, updatedDate);
				ps.setString(2, groupKey);
				ps.setString(3, region);
			}
		});
	}

	private void updateMessageGroup(final String groupId){
		jdbcTemplate.update(getQuery(Query.UPDATE_GROUP), new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				if (logger.isDebugEnabled()){
					logger.debug("Updating MessageGroup: " + groupId);
				}
				ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
				ps.setString(2, groupId);
				ps.setString(3, region);
			}
		});
	}

	private List<UUID> getMessageIdsForGroup(Object groupId){
		String key = getKey(groupId);

		final List<UUID> messageIds = new ArrayList<UUID>();

		jdbcTemplate.query(getQuery(Query.LIST_MESSAGEIDS_BY_GROUP_KEY), new Object[] { key, region },
				new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						messageIds.add(UUID.fromString(rs.getString(1)));
					}
				}
		);
		return messageIds;
	}

	private String getKey(Object input) {
		return input == null ? null : UUIDConverter.getUUID(input).toString();
	}

	/**
	 * Convenience class to be used to unpack a message from a result set row. Uses column named in the result set to
	 * extract the required data, so that select clause ordering is unimportant.
	 *
	 * @author Dave Syer
	 */
	private class MessageMapper implements RowMapper<Message<?>> {

		@Override
		public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
			Message<?> message = (Message<?>) deserializer.convert(lobHandler.getBlobAsBytes(rs, "MESSAGE_BYTES"));
			return message;
		}
	}
}
