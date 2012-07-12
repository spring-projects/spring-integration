/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.mongodb;

/**
 * All the common constants to be used by the Mongo DB constants
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 *
 */
public interface MongoDBIntegrationConstants {

	public final static String DEFAULT_COLLECTION_NAME = "messages";

	public final static String GROUP_ID_KEY = "_groupId";

	public final static String GROUP_COMPLETE_KEY = "_group_complete";

	public final static String LAST_RELEASED_SEQUENCE_NUMBER = "_last_released_sequence";

	public final static String GROUP_TIMESTAMP_KEY = "_group_timestamp";

	public final static String GROUP_UPDATE_TIMESTAMP_KEY = "_group_update_timestamp";

	public final static String PAYLOAD_TYPE_KEY = "_payloadType";

	public final static String CREATED_DATE = "_createdDate";
}
