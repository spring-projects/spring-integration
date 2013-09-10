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
package org.springframework.integration.jdbc.store.channel;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.messaging.Message;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Convenience class to be used to unpack a {@link Message} from a result set
 * row. Uses column named in the result set to extract the required data, so
 * that select clause ordering is unimportant.
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class MessageRowMapper implements RowMapper<Message<?>> {

	private final DeserializingConverter deserializer;
	private final LobHandler lobHandler;

	public MessageRowMapper(DeserializingConverter deserializer, LobHandler lobHandler) {
		this.deserializer = deserializer;
		this.lobHandler = lobHandler;
	}

	public Message<?> mapRow(ResultSet rs, int rowNum) throws SQLException {
		return (Message<?>) deserializer.convert(lobHandler.getBlobAsBytes(rs, "MESSAGE_BYTES"));
	}

}
