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
package org.springframework.integration.jdbc.store.channel;

import org.springframework.integration.jdbc.store.JdbcChannelMessageStore;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Contains Oracle-specific queries for the {@link JdbcChannelMessageStore}.
 * Please ensure that the used {@link JdbcTemplate}'s fetchSize property is <code>1</code>.
 * <p>
 * Fore more details, please see: http://stackoverflow.com/questions/6117254/force-oracle-to-return-top-n-rows-with-skip-locked
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.2
 */
public class OracleChannelMessageStoreQueryProvider extends AbstractChannelMessageStoreQueryProvider {

	@Override
	public String getCreateMessageQuery() {
		return "INSERT into %PREFIX%CHANNEL_MESSAGE(MESSAGE_ID, GROUP_KEY, REGION, CREATED_DATE, MESSAGE_PRIORITY, MESSAGE_SEQUENCE, MESSAGE_BYTES)"
				+ " values (?, ?, ?, ?, ?, %PREFIX%MESSAGE_SEQ.NEXTVAL, ?)";
	}

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return "SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES from %PREFIX%CHANNEL_MESSAGE " +
				"where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region " +
				"and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) order by CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPollFromGroupQuery() {
		return "SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES from %PREFIX%CHANNEL_MESSAGE " +
				"where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region " +
				"order by CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return "SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES from %PREFIX%CHANNEL_MESSAGE " +
				"where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region " +
				"and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) " +
				"order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED";
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return "SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES from %PREFIX%CHANNEL_MESSAGE " +
				"where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region " +
				"order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE FOR UPDATE SKIP LOCKED";
	}

}
