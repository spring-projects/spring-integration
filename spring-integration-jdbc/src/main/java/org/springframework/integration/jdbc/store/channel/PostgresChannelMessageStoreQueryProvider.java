/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Adama Sorho
 * @author Johannes Edmeier
 *
 * @since 2.2
 */
public class PostgresChannelMessageStoreQueryProvider implements ChannelMessageStoreQueryProvider {

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return """
				delete
				from %PREFIX%CHANNEL_MESSAGE
				where CTID = (select CTID
								from %PREFIX%CHANNEL_MESSAGE
								where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key
								and %PREFIX%CHANNEL_MESSAGE.REGION = :region
								and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids)
							order by CREATED_DATE, MESSAGE_SEQUENCE
							limit 1 for update skip locked)
				returning MESSAGE_ID, MESSAGE_BYTES;
				""";
	}

	@Override
	public String getPollFromGroupQuery() {
		return """
				delete
				from %PREFIX%CHANNEL_MESSAGE
				where CTID = (select CTID
								from %PREFIX%CHANNEL_MESSAGE
								where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key
								and %PREFIX%CHANNEL_MESSAGE.REGION = :region
							order by CREATED_DATE, MESSAGE_SEQUENCE
							limit 1 for update skip locked)
				returning MESSAGE_ID, MESSAGE_BYTES;
				""";
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return """
				delete
				from %PREFIX%CHANNEL_MESSAGE
				where CTID = (select CTID
								from %PREFIX%CHANNEL_MESSAGE
								where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key
								and %PREFIX%CHANNEL_MESSAGE.REGION = :region
								and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids)
							order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE
							limit 1 for update skip locked)
				returning MESSAGE_ID, MESSAGE_BYTES;
				""";
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return """
				delete
				from %PREFIX%CHANNEL_MESSAGE
				where CTID = (select CTID
								from %PREFIX%CHANNEL_MESSAGE
								where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key
								and %PREFIX%CHANNEL_MESSAGE.REGION = :region
							order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE
							limit 1 for update skip locked)
				returning MESSAGE_ID, MESSAGE_BYTES;
				""";
	}

	@Override
	public boolean isSingleStatementForPoll() {
		return true;
	}

}
