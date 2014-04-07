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


/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.2
 */
public abstract class AbstractChannelMessageStoreQueryProvider implements ChannelMessageStoreQueryProvider {

	public String getCountAllMessagesInGroupQuery() {
		return "SELECT COUNT(MESSAGE_ID) from %PREFIX%CHANNEL_MESSAGE where GROUP_KEY=? and REGION=?";
	}

	public String getMessageQuery() {
		return "SELECT MESSAGE_ID, CREATED_DATE, MESSAGE_BYTES from %PREFIX%CHANNEL_MESSAGE where MESSAGE_ID=? and GROUP_KEY=? and REGION=?";
	}

	public String getMessageCountForRegionQuery() {
		return "SELECT COUNT(MESSAGE_ID) from %PREFIX%CHANNEL_MESSAGE where REGION=?";
	}

	public String getDeleteMessageQuery() {
		return "DELETE from %PREFIX%CHANNEL_MESSAGE where MESSAGE_ID=? and GROUP_KEY=? and REGION=?";
	}

	public String getCreateMessageQuery() {
		return "INSERT into %PREFIX%CHANNEL_MESSAGE(MESSAGE_ID, GROUP_KEY, REGION, CREATED_DATE, MESSAGE_PRIORITY, MESSAGE_BYTES)"
				+ " values (?, ?, ?, ?, ?, ?)";
	}

	public String getDeleteMessageGroupQuery() {
		return "DELETE from %PREFIX%CHANNEL_MESSAGE where GROUP_KEY=? and REGION=?";
	}

}
