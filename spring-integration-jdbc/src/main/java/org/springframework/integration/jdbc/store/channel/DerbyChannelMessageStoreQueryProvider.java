/*
 * Copyright 2002-2013 the original author or authors.
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

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.2
 *        <p/>
 *        https://blogs.oracle.com/kah/entry/derby_10_5_preview_fetch
 */
public class DerbyChannelMessageStoreQueryProvider extends AbstractChannelMessageStoreQueryProvider {

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return this.getPollQuery(true, false);
	}

	@Override
	public String getPollFromGroupQuery() {
		return this.getPollQuery(false, false);
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return this.getPollQuery(true, true);
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return this.getPollQuery(false, true);
	}

	private String getPollQuery(boolean excludeIds, boolean byPriority) {
		StringBuilder sb = new StringBuilder("SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES " +
				"from %PREFIX%CHANNEL_MESSAGE " +
				"where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region ");
		if (excludeIds) {
			sb.append(" and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) ");
		}
		sb.append(" order by ");
		if (byPriority) {
			sb.append(" PRIORITY DESC, ");
		}
		sb.append(" CREATED_DATE ASC ");
		sb.append("FETCH FIRST ROW ONLY");

		return sb.toString();
	}

}
