/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Adama Sorho
 *
 * @since 2.2
 */
public class MySqlChannelMessageStoreQueryProvider implements ChannelMessageStoreQueryProvider {

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON
				+ "and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) "
				+ "order by CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPollFromGroupQuery() {
		return SELECT_COMMON +
				"order by CREATED_DATE, MESSAGE_SEQUENCE LIMIT 1 FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON +
				"and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) " +
				"order by MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return SELECT_COMMON +
				"order by MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE " +
				"LIMIT 1 FOR UPDATE SKIP LOCKED";
	}

}
