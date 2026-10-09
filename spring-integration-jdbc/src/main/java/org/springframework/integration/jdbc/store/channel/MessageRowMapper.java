/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Convenience class to be used to unpack a {@link Message} from a result set
 * row. Uses column named in the result set to extract the required data, so
 * that select clause ordering is unimportant.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class MessageRowMapper implements RowMapper<Message<?>> {

	private final AllowListDeserializingConverter deserializer;

	/**
	 * Construct an instance based on the provided {@link AllowListDeserializingConverter}.
	 * @param deserializer the {@link AllowListDeserializingConverter} to use.
	 * @since 6.4
	 */
	public MessageRowMapper(AllowListDeserializingConverter deserializer) {
		Assert.notNull(deserializer, "'deserializer' must not be null");
		this.deserializer = deserializer;
	}

	@Override
	public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
		byte[] blobAsBytes = rs.getBytes("MESSAGE_CONTENT");
		return (Message<?>) this.deserializer.convert(Objects.requireNonNull(blobAsBytes));
	}

}
