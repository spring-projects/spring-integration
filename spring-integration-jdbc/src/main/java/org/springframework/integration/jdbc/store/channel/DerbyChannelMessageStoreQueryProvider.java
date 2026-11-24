/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jdbc.store.channel;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Adama Sorho
 *
 * @since 2.2
 */
public class DerbyChannelMessageStoreQueryProvider implements ChannelMessageStoreQueryProvider {

	@Override
	public String getPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON
				+ "and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) "
				+ "order by CREATED_DATE, MESSAGE_SEQUENCE FETCH FIRST ROW ONLY";
	}

	@Override
	public String getPollFromGroupQuery() {
		return SELECT_COMMON
				+ "order by CREATED_DATE, MESSAGE_SEQUENCE FETCH FIRST ROW ONLY";
	}

	@Override
	public String getPriorityPollFromGroupExcludeIdsQuery() {
		return SELECT_COMMON
				+ "and %PREFIX%CHANNEL_MESSAGE.MESSAGE_ID not in (:message_ids) "
				+ "order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE FETCH FIRST ROW ONLY";
	}

	@Override
	public String getPriorityPollFromGroupQuery() {
		return SELECT_COMMON
				+ "order by MESSAGE_PRIORITY DESC NULLS LAST, CREATED_DATE, MESSAGE_SEQUENCE FETCH FIRST ROW ONLY";
	}

}
