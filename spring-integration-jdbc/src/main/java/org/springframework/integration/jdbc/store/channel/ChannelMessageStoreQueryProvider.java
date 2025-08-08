/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
				where %PREFIX%CHANNEL_MESSAGE.GROUP_KEY = :group_key and %PREFIX%CHANNEL_MESSAGE.REGION = :region
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
	 * Indicate if the queries for polling are using a single statement (e.g. DELETE ... RETURNING) to
	 * retrieve and delete the message from the channel store.
	 * @return true if a single statement is used, false if a select and delete is required.
	 * @since 6.2
	 */
	default boolean isSingleStatementForPoll() {
		return false;
	}

}
