/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

/**
 * Contains Oracle-specific queries for the
 * {@link org.springframework.integration.jdbc.store.JdbcChannelMessageStore}. Please
 * ensure that the used {@link org.springframework.jdbc.core.JdbcTemplate}'s fetchSize
 * property is {@code 1}.
 * <p>
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 */
public class OracleChannelMessageStoreQueryProvider implements ChannelMessageStoreQueryProvider {

	@Override
	public String getCreateMessageQuery() {
		return "INSERT into %PREFIX%CHANNEL_MESSAGE(MESSAGE_ID, GROUP_KEY, REGION, CREATED_DATE, MESSAGE_PRIORITY, "
				+ "MESSAGE_SEQUENCE, MESSAGE_BYTES)"
				+ " values (?, ?, ?, ?, ?, %PREFIX%MESSAGE_SEQ.NEXTVAL, ?)";
	}

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON
				+ "and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) "
				+ "order by CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPollFromGroupQuery() {
		return SELECT_COMMON +
				"order by CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return """
				SELECT /*+ INDEX(%PREFIX%CHANNEL_MESSAGE %PREFIX%CHANNEL_MSG_PRIORITY_IDX) */
					%PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES
				from %PREFIX%CHANNEL_MESSAGE
				where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key
					and %PREFIX%CHANNEL_MESSAGE.REGION = :region
					and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids)
				order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED
				""";
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return """
				SELECT /*+ INDEX(%PREFIX%CHANNEL_MESSAGE %PREFIX%CHANNEL_MSG_PRIORITY_IDX) */
					%PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES
				from %PREFIX%CHANNEL_MESSAGE
				where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key
					and %PREFIX%CHANNEL_MESSAGE.REGION = :region
				order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED
				""";
	}

}
