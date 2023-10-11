package org.springframework.integration.jdbc.store.channel;

/**
 * @author Johannes Edmeier
 *
 * @since 6.2
 */
public class DeleteReturningPostgresChannelMessageStoreQueryProvider extends PostgresChannelMessageStoreQueryProvider {

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
							limit 1)
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
							limit 1)
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
							limit 1)
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
							limit 1)
				returning MESSAGE_ID, MESSAGE_BYTES;
				""";
	}

}