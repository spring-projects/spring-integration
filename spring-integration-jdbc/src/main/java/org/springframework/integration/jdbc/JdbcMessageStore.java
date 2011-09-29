/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.integration.Message;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link MessageStore} using a relational database via JDBC. SQL scripts to create the necessary
 * tables are packaged as <code>org/springframework/integration/jdbc/schema-*.sql</code>, where <code>*</code> is the
 * target database type.
 * 
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ManagedResource
public class JdbcMessageStore extends AbstractMessageGroupStore implements MessageStore {

	private static final Log logger = LogFactory.getLog(JdbcMessageStore.class);

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	private static final String GET_MESSAGE = "SELECT MESSAGE_ID, CREATED_DATE, MESSAGE_BYTES from %PREFIX%MESSAGE where MESSAGE_ID=? and REGION=?";

	private static final String GET_MESSAGE_COUNT = "SELECT COUNT(MESSAGE_ID) from %PREFIX%MESSAGE where REGION=?";

	private static final String DELETE_MESSAGE = "DELETE from %PREFIX%MESSAGE where MESSAGE_ID=? and REGION=?";

	private static final String CREATE_MESSAGE = "INSERT into %PREFIX%MESSAGE(MESSAGE_ID, REGION, CREATED_DATE, MESSAGE_BYTES)"
			+ " values (?, ?, ?, ?)";

	private static final String LIST_MESSAGES_BY_GROUP_KEY = "SELECT MESSAGE_ID, CREATED_DATE, GROUP_KEY, MESSAGE_BYTES, MARKED, COMPLETE from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=? order by CREATED_DATE";

	private static final String COUNT_ALL_GROUPS = "SELECT COUNT(GROUP_KEY) from %PREFIX%MESSAGE_GROUP where REGION=?";

	private static final String COUNT_ALL_MARKED_MESSAGES_IN_GROUPS = "SELECT COUNT(MESSAGE_ID) from %PREFIX%MESSAGE_GROUP where MARKED=1 AND REGION=?";

	private static final String COUNT_ALL_MESSAGES_IN_GROUPS = "SELECT COUNT(MESSAGE_ID) from %PREFIX%MESSAGE_GROUP where REGION=?";

	private static final String MARK_MESSAGES_IN_GROUP = "UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, MARKED=1 where MARKED=0 and GROUP_KEY=? and REGION=?";

	private static final String MARK_MESSAGE_IN_GROUP = "UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, MARKED=1 where MESSAGE_ID=? and MARKED=0 and GROUP_KEY=? and REGION=?";
	
	private static final String COMPLETE_GROUP = "UPDATE %PREFIX%MESSAGE_GROUP set UPDATED_DATE=?, COMPLETE=1 where GROUP_KEY=? and REGION=?";

	private static final String REMOVE_MESSAGE_FROM_GROUP = "DELETE from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=? and MESSAGE_ID=?";

	private static final String DELETE_MESSAGE_GROUP = "DELETE from %PREFIX%MESSAGE_GROUP where GROUP_KEY=? and REGION=?";

	private static final String CREATE_MESSAGE_IN_GROUP = "INSERT into %PREFIX%MESSAGE_GROUP(MESSAGE_ID, REGION, CREATED_DATE, GROUP_KEY, MARKED, COMPLETE, MESSAGE_BYTES)"
			+ " values (?, ?, ?, ?, 0, 0, ?)";

	private static final String LIST_GROUP_KEYS = "SELECT distinct GROUP_KEY as CREATED from %PREFIX%MESSAGE_GROUP where REGION=?";

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
	 * Replace patterns in the input to produce a valid SQL query. This implementation replaces the table prefix.
	 * 
	 * @param base the SQL query to be transformed
	 * @return a transformed query with replacements
	 */
	protected String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
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

	/**
	 * Check mandatory properties (data source and incrementer).
	 * 
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
	}

	public Message<?> removeMessage(UUID id) {
		Message<?> message = getMessage(id);
		if (message == null) {
			return null;
		}
		int updated = jdbcTemplate.update(getQuery(DELETE_MESSAGE), new Object[] { getKey(id), region }, new int[] {
				Types.VARCHAR, Types.VARCHAR });
		if (updated != 0) {
			return message;
		}
		return null;
	}

	@ManagedAttribute
	public long getMessageCount() {
		return jdbcTemplate.queryForInt(getQuery(GET_MESSAGE_COUNT), region);
	}

	public Message<?> getMessage(UUID id) {
		List<Message<?>> list = jdbcTemplate.query(getQuery(GET_MESSAGE), new Object[] { getKey(id), region }, mapper);
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	public <T> Message<T> addMessage(final Message<T> message) {
		if (message.getHeaders().containsKey(SAVED_KEY)) {
			@SuppressWarnings("unchecked")
			Message<T> saved = (Message<T>) getMessage(message.getHeaders().getId());
			if (saved != null) {
				if (saved.equals(message)) {
					return message;
				} // We need to save it under its own id
			}
		}

		final long createdDate = System.currentTimeMillis();
		Message<T> result = MessageBuilder.fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE)
				.setHeader(CREATED_DATE_KEY, new Long(createdDate)).build();
		final String messageId = getKey(result.getHeaders().getId());
		final byte[] messageBytes = serializer.convert(result);

		jdbcTemplate.update(getQuery(CREATE_MESSAGE), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Inserting message with id key=" + messageId);
				ps.setString(1, messageId);
				ps.setString(2, region);
				ps.setTimestamp(3, new Timestamp(createdDate));
				lobHandler.getLobCreator().setBlobAsBytes(ps, 4, messageBytes);
			}
		});
		return result;
	}

	public MessageGroup addMessageToGroup(Object groupId, Message<?> message) {

		final long createdDate = System.currentTimeMillis();
		final String messageId = getKey(message.getHeaders().getId());
		final String groupKey = getKey(groupId);
		final byte[] messageBytes = serializer.convert(message);

		jdbcTemplate.update(getQuery(CREATE_MESSAGE_IN_GROUP), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Inserting message with id key=" + messageId + " and created date=" + createdDate);
				ps.setString(1, messageId);
				ps.setString(2, region);
				ps.setTimestamp(3, new Timestamp(createdDate));
				ps.setString(4, groupKey);
				lobHandler.getLobCreator().setBlobAsBytes(ps, 5, messageBytes);
			}
		});

		return getMessageGroup(groupId);

	}

	@ManagedAttribute
	public int getMessageGroupCount() {
		return jdbcTemplate.queryForInt(getQuery(COUNT_ALL_GROUPS), region);
	}

	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		return jdbcTemplate.queryForInt(getQuery(COUNT_ALL_MESSAGES_IN_GROUPS), region);
	}

	@ManagedAttribute
	public int getMarkedMessageCountForAllMessageGroups() {
		return jdbcTemplate.queryForInt(getQuery(COUNT_ALL_MARKED_MESSAGES_IN_GROUPS), region);
	}

	public MessageGroup getMessageGroup(Object groupId) {
		String key = getKey(groupId);
		final List<Message<?>> marked = new ArrayList<Message<?>>();
		final List<Message<?>> unmarked = new ArrayList<Message<?>>();
		final AtomicReference<Date> date = new AtomicReference<Date>();
		final AtomicReference<Boolean> completeFlag = new AtomicReference<Boolean>();
		
		jdbcTemplate.query(getQuery(LIST_MESSAGES_BY_GROUP_KEY), new Object[] { key, region },
				new RowCallbackHandler() {
					int count = 0;

					public void processRow(ResultSet rs) throws SQLException {
						int markedFlag = rs.getInt("MARKED");
						Message<?> message = mapper.mapRow(rs, count++);
						if (markedFlag > 0) {
							marked.add(message);
						}
						else {
							unmarked.add(message);
						}
						date.set(rs.getTimestamp("CREATED_DATE"));
						
						completeFlag.set(rs.getInt("COMPLETE") > 0);
					}
				});
		if (marked.isEmpty() && unmarked.isEmpty()) {
			return new SimpleMessageGroup(groupId);
		}
		Assert.state(date.get() != null, "Could not locate created date for groupId=" + groupId);
		long timestamp = date.get().getTime();
		boolean complete = completeFlag.get().booleanValue();
		return new SimpleMessageGroup(unmarked, marked, groupId, timestamp, complete);
	}

	public MessageGroup markMessageGroup(MessageGroup group) {

		final long updatedDate = System.currentTimeMillis();
		final String groupKey = getKey(group.getGroupId());

		jdbcTemplate.update(getQuery(MARK_MESSAGES_IN_GROUP), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Marking messages with group key=" + groupKey);
				ps.setTimestamp(1, new Timestamp(updatedDate));
				ps.setString(2, groupKey);
				ps.setString(3, region);
			}
		});

		return getMessageGroup(group.getGroupId());

	}

	public MessageGroup removeMessageFromGroup(Object groupId, Message<?> messageToRemove) {
		final String groupKey = getKey(groupId);
		final String messageId = getKey(messageToRemove.getHeaders().getId());

		jdbcTemplate.update(getQuery(REMOVE_MESSAGE_FROM_GROUP), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Removing message from group with group key=" + groupKey);
				ps.setString(1, groupKey);
				ps.setString(2, region);
				ps.setString(3, messageId);
			}
		});
		return getMessageGroup(groupId);
	}

	/**
	 * {@inheritDoc}
	 */
	public MessageGroup markMessageFromGroup(Object groupId, Message<?> messageToMark) {

		final long updatedDate = System.currentTimeMillis();
		final String groupKey = getKey(groupId);
		final String messageId = getKey(messageToMark.getHeaders().getId());

		jdbcTemplate.update(getQuery(MARK_MESSAGE_IN_GROUP), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Marking message " + messageId + " in group with group key=" + groupKey);
				ps.setTimestamp(1, new Timestamp(updatedDate));
				ps.setString(2, messageId);
				ps.setString(3, groupKey);
				ps.setString(4, region);
			}
		});
		return getMessageGroup(groupId);
	}

	public void removeMessageGroup(Object groupId) {

		final String groupKey = getKey(groupId);

		jdbcTemplate.update(getQuery(DELETE_MESSAGE_GROUP), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Marking messages with group key=" + groupKey);
				ps.setString(1, groupKey);
				ps.setString(2, region);
			}
		});
	}
	
	public void completeGroup(Object groupId) {
		final long updatedDate = System.currentTimeMillis();
		final String groupKey = getKey(groupId);
		
		jdbcTemplate.update(getQuery(COMPLETE_GROUP), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Completing MessageGroup: " + groupKey);
				ps.setTimestamp(1, new Timestamp(updatedDate));
				ps.setString(2, groupKey);
				ps.setString(3, region);
			}
		});
	}

	public Iterator<MessageGroup> iterator() {

		final Iterator<String> iterator = jdbcTemplate.query(getQuery(LIST_GROUP_KEYS), new Object[] { region },
				new SingleColumnRowMapper<String>()).iterator();

		return new Iterator<MessageGroup>() {

			public boolean hasNext() {
				return iterator.hasNext();
			}

			public MessageGroup next() {
				return getMessageGroup(iterator.next());
			}

			public void remove() {
				throw new UnsupportedOperationException("Cannot remove MessageGroup from this iterator.");
			}

		};

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

		public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
			Message<?> message = (Message<?>) deserializer.convert(lobHandler.getBlobAsBytes(rs, "MESSAGE_BYTES"));
			return message;
		}
	}
}
