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

package org.springframework.integration.jdbc.store;

import org.springframework.integration.jdbc.store.channel.ChannelMessageStoreQueryProvider;
import org.springframework.integration.jdbc.store.channel.DeleteReturningPostgresChannelMessageStoreQueryProvider;
import org.springframework.integration.store.MessageStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import javax.sql.DataSource;

/**
 * Implementation of {@link MessageStore} for Postgres using a single statement to poll for messages.
 *
 * @author Johannes Edmeier
 * @since 6.2
 */
public class PostgresJdbcChannelMessageStore extends JdbcChannelMessageStore {

	public PostgresJdbcChannelMessageStore() {
		super();
	}

	public PostgresJdbcChannelMessageStore(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Message<?> pollMessageFromGroup(Object groupId) {
		return doPollForMessage(getKey(groupId));
	}

	@Override
	public void setChannelMessageStoreQueryProvider(ChannelMessageStoreQueryProvider channelMessageStoreQueryProvider) {
		Assert.isInstanceOf(DeleteReturningPostgresChannelMessageStoreQueryProvider.class, channelMessageStoreQueryProvider,
				"The provided channelMessageStoreQueryProvider must be an instance of DeleteReturningPostgresChannelMessageStoreQueryProvider");
		super.setChannelMessageStoreQueryProvider(channelMessageStoreQueryProvider);
	}
}
