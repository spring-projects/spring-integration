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


/**
 * Common interface used in order to configure the
 * {@link JdbcChannelMessageStore} to provide database-specific queries.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.2
 */
public interface ChannelMessageStoreQueryProvider {

	/**
	 * Get the query used to retrieve a count of all messages currently persisted
	 * for a channel.
	 *
	 * @return Sql Query
	 */
	String getCountAllMessagesInGroupQuery();

	/**
	 * Get the query used to retrieve the oldest message for a channel excluding
	 * messages that match the provided message ids.
	 *
	 * @return Sql Query
	 */
	String getPollFromGroupExcludeIdsQuery();

	/**
	 * Get the query used to retrieve the oldest message for a channel.
	 *
	 * @return Sql Query
	 */
	String getPollFromGroupQuery();

	/**
	 * Get the query used to retrieve the oldest message by priority for a channel excluding
	 * messages that match the provided message ids.
	 *
	 * @return Sql Query
	 */
	String getPriorityPollFromGroupExcludeIdsQuery();

	/**
	 * Get the query used to retrieve the oldest message by priority for a channel.
	 *
	 * @return Sql Query
	 */
	String getPriorityPollFromGroupQuery();

	/**
	 * Query that retrieves a message for the provided message id, channel and
	 * region.
	 *
	 * @return Sql Query
	 */
	String getMessageQuery();

	/**
	 * Query that retrieve a count of all messages for a region.
	 *
	 * @return Sql Query
	 */
	String getMessageCountForRegionQuery();

	/**
	 * Query to delete a single message from the database.
	 *
	 * @return Sql Query
	 */
	String getDeleteMessageQuery();

	/**
	 * Query to add a single message to the database.
	 *
	 * @return Sql Query
	 */
	String getCreateMessageQuery();

	/**
	 * Query to delete all messages that belong to a specific channel.
	 *
	 * @return Sql Query
	 */
	String getDeleteMessageGroupQuery();

}
