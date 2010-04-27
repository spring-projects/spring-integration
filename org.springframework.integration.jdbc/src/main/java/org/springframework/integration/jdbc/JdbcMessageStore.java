package org.springframework.integration.jdbc;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.jdbc.util.SerializationUtils;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link MessageStore} using a relational database via JDBC.
 * SQL scripts to create the necessary tables are packaged as
 * <code>org/springframework/integration/jdbc/schema-*.sql</code>, where
 * <code>*</code> is the target database type.
 * 
 * @author Dave Syer
 * 
 */
public class JdbcMessageStore implements MessageStore {

	private static final Log logger = LogFactory.getLog(JdbcMessageStore.class);

	/**
	 * Default value for the table prefix property.
	 */
	public static final String DEFAULT_TABLE_PREFIX = "INT_";

	private static final String LIST_MESSAGES_BY_CORRELATION_KEY = "SELECT MESSAGE_ID, CREATED_DATE, CORRELATION_KEY, MESSAGE_BYTES from %PREFIX%MESSAGE where CORRELATION_KEY=?";

	private static final String GET_MESSAGE = "SELECT MESSAGE_ID, CREATED_DATE, CORRELATION_KEY, MESSAGE_BYTES from %PREFIX%MESSAGE where MESSAGE_ID=?";

	private static final String DELETE_MESSAGE = "DELETE from %PREFIX%MESSAGE where MESSAGE_ID=?";

	private static final String CREATE_MESSAGE = "INSERT into %PREFIX%MESSAGE(MESSAGE_ID, CREATED_DATE, CORRELATION_KEY, MESSAGE_BYTES)"
			+ " values (?, ?, ?, ?)";

	public static final int DEFAULT_LONG_STRING_LENGTH = 2500;

	/**
	 * The name of the message header that stores a flag to indicate that the
	 * message has been saved. This is an optimization for the put method.
	 */
	public static final String SAVED_KEY = JdbcMessageStore.class.getSimpleName() + ".SAVED";

	/**
	 * The name of the message header that stores a timestamp for the time the
	 * message was inserted.
	 */
	public static final String CREATED_DATE_KEY = JdbcMessageStore.class.getSimpleName() + ".CREATED_DATE";

	private String tablePrefix = DEFAULT_TABLE_PREFIX;

	private JdbcOperations jdbcTemplate;

	private LobHandler lobHandler = new DefaultLobHandler();

	private MessageMapper mapper = new MessageMapper();

	/**
	 * Convenient constructor for configuration use.
	 */
	public JdbcMessageStore() {
	}

	/**
	 * Create a {@link MessageStore} with all mandatory properties.
	 * 
	 * @param dataSource a {@link DataSource}
	 */
	public JdbcMessageStore(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * Replace patterns in the input to produce a valid SQL query. This
	 * implementation replaces the table prefix.
	 * 
	 * @param base the SQL query to be transformed
	 * @return a transformed query with replacements
	 */
	protected String getQuery(String base) {
		return StringUtils.replace(base, "%PREFIX%", tablePrefix);
	}

	/**
	 * Public setter for the table prefix property. This will be prefixed to all
	 * the table names before queries are executed. Defaults to
	 * {@link #DEFAULT_TABLE_PREFIX}.
	 * 
	 * @param tablePrefix the tablePrefix to set
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	/**
	 * The JDBC {@link DataSource} to use when interacting with the database.
	 * Either this property can be set or the
	 * {@link #setJdbcTemplate(JdbcOperations) jdbcTemplate}.
	 * 
	 * @param dataSource a {@link DataSource}
	 */
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/**
	 * The {@link JdbcOperations} to use when interacting with the database.
	 * Either this property can be set or the {@link #setDataSource(DataSource)
	 * dataSource}.
	 * 
	 * @param dataSource a {@link DataSource}
	 */
	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * Override the {@link LobHandler} that is used to create and unpack large
	 * objects in SQL queries. The default is fine for almost all platforms, but
	 * some Oracle drivers require a native implementation.
	 * 
	 * @param lobHandler a {@link LobHandler}
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * Check mandatory properties (data source and incrementer).
	 * 
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "A DataSource or JdbcTemplate must be provided");
	}

	public Message<?> delete(UUID id) {

		Message<?> message = get(id);
		if (message == null) {
			return null;
		}

		int updated = jdbcTemplate.update(getQuery(DELETE_MESSAGE), new Object[] { getKey(id) },
				new int[] { Types.VARCHAR });

		if (updated != 0) {
			return message;
		}

		return null;

	}

	public Message<?> get(UUID id) {
		List<Message<?>> list = jdbcTemplate.query(getQuery(GET_MESSAGE), new Object[] { getKey(id) }, mapper);
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	public List<Message<?>> list(Object correlationId) {
		return jdbcTemplate.query(getQuery(LIST_MESSAGES_BY_CORRELATION_KEY), new Object[] { getKey(correlationId) },
				mapper);
	}

	public <T> Message<T> put(final Message<T> message) {

		if (message.getHeaders().containsKey(SAVED_KEY)) {
			@SuppressWarnings("unchecked")
			Message<T> saved = (Message<T>) get(message.getHeaders().getId());
			if (saved != null) {
				if (saved.equals(message)) {
					return message;
				} // We need to save it under its own id
			}
		}

		final long createdDate = System.currentTimeMillis();
		Message<T> result = MessageBuilder.fromMessage(message).setHeader(SAVED_KEY, Boolean.TRUE).setHeader(
				CREATED_DATE_KEY, new Long(createdDate)).build();
		final String messageId = getKey(result.getHeaders().getId());
		final String correlationId = getKey(result.getHeaders().getCorrelationId());
		final byte[] messageBytes = SerializationUtils.serialize(result);

		jdbcTemplate.update(getQuery(CREATE_MESSAGE), new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				logger.debug("Inserting message with id key=" + messageId);
				ps.setString(1, messageId);
				ps.setTimestamp(2, new Timestamp(createdDate));
				ps.setString(3, correlationId);
				lobHandler.getLobCreator().setBlobAsBytes(ps, 4, messageBytes);
			}
		});

		return result;

	}

	private String getKey(Object input) {

		if (input == null) {
			return null;
		}

		if (input instanceof UUID) {
			return input.toString();
		}

		if (input instanceof String && ((String) input).length() < 100) {
			return (String) input;
		}

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
		}

		byte[] bytes = digest.digest(SerializationUtils.serialize(input));
		return String.format("%032x", new BigInteger(1, bytes));

	}

	/**
	 * Convenience class to be used to unpack a message from a result set row.
	 * Uses column named in the result set to extract the required data, so that
	 * select clause ordering is unimportant.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class MessageMapper implements RowMapper<Message<?>> {

		public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
			Message<?> message = (Message<?>) SerializationUtils.deserialize(lobHandler.getBlobAsBytes(rs,
					"MESSAGE_BYTES"));
			return message;
		}

	}

}
