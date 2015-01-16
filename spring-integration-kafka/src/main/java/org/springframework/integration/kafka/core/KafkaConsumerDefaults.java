/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.core;

import kafka.api.OffsetRequest;

/**
 * Kafka adapter specific message headers.
 *
 * @author Soby Chacko
 * @author Marius Bogoevici
 * @since 0.5
 */
public abstract class KafkaConsumerDefaults {

	//High level consumer
	public static final String GROUP_ID = "groupid";

	public static final int SOCKET_TIMEOUT_INT = 30000;

	public static final String SOCKET_TIMEOUT = Integer.toString(SOCKET_TIMEOUT_INT);

	public static final int SOCKET_BUFFER_SIZE_INT = 64*1024;

	public static final String SOCKET_BUFFER_SIZE = Integer.toString(SOCKET_BUFFER_SIZE_INT);

	public static final int FETCH_SIZE_INT = 300 * 1024;

	public static final String FETCH_SIZE = Integer.toString(FETCH_SIZE_INT);

	public static final String BACKOFF_INCREMENT = "1000";

	public static final String QUEUED_CHUNKS_MAX = "100";

	public static final String AUTO_COMMIT_ENABLE = "true";

	public static final String AUTO_COMMIT_INTERVAL = "10000";

	public static final String AUTO_OFFSET_RESET = "smallest";

	//Overriding the default value of -1, which will make the consumer to wait indefinitely
	public static final String CONSUMER_TIMEOUT = "5000";

	public static final String REBALANCE_RETRIES_MAX = "4";

	public static final int MIN_FETCH_BYTES = 1;

	public static final int MAX_WAIT_TIME_IN_MS = 100;

	public static final long DEFAULT_OFFSET_RESET = OffsetRequest.EarliestTime();

	public static final int FETCH_METADATA_TIMEOUT = 10000;

}
