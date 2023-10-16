/*
 * Copyright 2002-2023 the original author or authors.
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
 * Common interface used in order to configure the
 * {@link org.springframework.integration.jdbc.store.JdbcChannelMessageStore} to provide
 * database-specific queries.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Adama Sorho
 * @author Johannes Edmeier
 *
 * @since 2.2
 */
public interface ChannelMessageStoreQueryProvider {

	String SELECT_COMMON = """
				SELECT %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID, %PREFIX%CHANNEL_MESSAGE.MESSAGE_BYTES
				from %PREFIX%CHANNEL_MESSAGE
				where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region\s
			""";

	/**
	 * Get the query used to retrieve a count of all messages currently persisted
	 * for a channel.
	 * @return query string
	 */
	default String getCountAllMessagesInGroupQuery() {
		return "SELECT COUNT(MESSAGE_ID) from %PREFIX%CHANNEL_MESSAGE where GROUP_KEY=? and REGION=?";
	}

	/**
	 * Query that retrieves a message for the provided message id, channel and
	 * region.
	 * @return query string
	 */
	default String getMessageQuery() {
		return """
				SELECT MESSAGE_ID, CREATED_DATE, MESSAGE_BYTES
				from %PREFIX%CHANNEL_MESSAGE
				where MESSAGE_ID=? and GROUP_KEY=? and REGION=?
				""";
	}

	/**
	 * Query that retrieve a count of all messages for a region.
	 * @return query string
	 */
	default String getMessageCountForRegionQuery() {
		return "SELECT COUNT(MESSAGE_ID) from %PREFIX%CHANNEL_MESSAGE where REGION=?";
	}

	/**
	 * Query to delete a single message from the database.
	 * @return query string
	 */
	default String getDeleteMessageQuery() {
		return "DELETE from %PREFIX%CHANNEL_MESSAGE where MESSAGE_ID=? and GROUP_KEY=? and REGION=?";
	}

	/**
	 * Query to add a single message to the database.
	 * @return query string
	 */
	default String getCreateMessageQuery() {
		return """
				INSERT into %PREFIX%CHANNEL_MESSAGE(
					MESSAGE_ID,
					GROUP_KEY,
					REGION,
					CREATED_DATE,
					MESSAGE_PRIORITY,
					MESSAGE_BYTES)
				values (?, ?, ?, ?, ?, ?)
				""";
	}

	/**
	 * Query to delete all messages that belong to a specific channel.
	 * @return query string
	 */
	default String getDeleteMessageGroupQuery() {
		return "DELETE from %PREFIX%CHANNEL_MESSAGE where GROUP_KEY=? and REGION=?";
	}

	/**
	 * Get the query used to retrieve the oldest message for a channel excluding
	 * messages that match the provided message ids.
	 * @return query string
	 */
	String getPollFromGroupExcludeIdsQuery();

	/**
	 * Get the query used to retrieve the oldest message for a channel.
	 * @return query string
	 */
	String getPollFromGroupQuery();

	/**
	 * Get the query used to retrieve the oldest message by priority for a channel excluding
	 * messages that match the provided message ids.
	 * @return query string
	 */
	String getPriorityPollFromGroupExcludeIdsQuery();

	/**
	 * Get the query used to retrieve the oldest message by priority for a channel.
	 * @return query string
	 */
	String getPriorityPollFromGroupQuery();

	/**
	 * Indicates if the queries for polling are using a single statement (e.g. DELETE ... RETURNING) to
	 * retrieve and delete the message from the channel store.
	 * @return true if a single statement is used, false if a select and delete is required.
	 * @since 6.2
	 */
	default boolean isSingleStatementForPoll() {
		return false;
	}

}
