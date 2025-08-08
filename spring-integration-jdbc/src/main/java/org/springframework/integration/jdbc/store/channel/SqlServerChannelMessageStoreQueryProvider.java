/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

/**
 * Channel message store query provider for Microsoft SQL Server / Azure SQL database.
 *
 * @author Sundara Balaji
 * @author Adama Sorho
 * @author Artem Bilan
 *
 * @since 5.1
 */
public class SqlServerChannelMessageStoreQueryProvider implements ChannelMessageStoreQueryProvider {

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON +
				"and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) order by CREATED_DATE, MESSAGE_SEQUENCE";
	}

	@Override
	public String getPollFromGroupQuery() {
		return SELECT_COMMON +
				"order by CREATED_DATE, MESSAGE_SEQUENCE";
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON +
				"and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) " +
				"order by MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE";
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return SELECT_COMMON +
				"order by MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE";
	}

	@Override
	public String getCreateMessageQuery() {
		return """
				INSERT into %PREFIX%CHANNEL_MESSAGE(
					MESSAGE_ID,
					GROUP_KEY,
					REGION,
					CREATED_DATE,
					MESSAGE_PRIORITY,
					MESSAGE_SEQUENCE,
					MESSAGE_BYTES)
				values (?, ?, ?, ?, ?,(NEXT VALUE FOR %PREFIX%MESSAGE_SEQ), ?)
				""";
	}

}
