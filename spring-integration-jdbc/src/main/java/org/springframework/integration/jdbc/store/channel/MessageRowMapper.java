/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.messaging.Message;

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

	private final LobHandler lobHandler;

	/**
	 * Construct an instance based on the provided {@link AllowListDeserializingConverter}
	 * and {@link LobHandler}.
	 * @param deserializer the {@link AllowListDeserializingConverter} to use.
	 * @param lobHandler the {@link LobHandler} to use.
	 */
	public MessageRowMapper(AllowListDeserializingConverter deserializer, LobHandler lobHandler) {
		this.deserializer = deserializer;
		this.lobHandler = lobHandler;
	}

	@Override
	public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
		byte[] blobAsBytes = this.lobHandler.getBlobAsBytes(rs, "MESSAGE_BYTES");
		if (blobAsBytes == null) {
			return null;
		}
		else {
			return (Message<?>) this.deserializer.convert(blobAsBytes);
		}
	}

}
