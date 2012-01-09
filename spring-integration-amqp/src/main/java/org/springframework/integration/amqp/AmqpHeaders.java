/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.amqp;

import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving AMQP
 * MessageProperties from/to integration Message Headers.
 * 
 * @author Mark Fisher
 */
public abstract class AmqpHeaders {

	/**
	 * Prefix used for AMQP related headers in order to distinguish from
	 * user-defined headers and other internal headers (e.g. replyTo).
	 * @see DefaultAmqpHeaderMapper
	 */
	public static final String PREFIX = "amqp_";


	// Header Name Constants

	public static final String APP_ID = PREFIX + "appId";

	public static final String CLUSTER_ID = PREFIX + "clusterId";

	public static final String CONTENT_ENCODING = PREFIX + "contentEncoding";

	public static final String CONTENT_LENGTH = PREFIX + "contentLength";

	public static final String CONTENT_TYPE = PREFIX + "contentType";

	public static final String CORRELATION_ID = PREFIX + "correlationId";

	public static final String DELIVERY_MODE = PREFIX + "deliveryMode";

	public static final String DELIVERY_TAG = PREFIX + "deliveryTag";

	public static final String EXPIRATION = PREFIX + "expiration";

	public static final String MESSAGE_COUNT = PREFIX + "messageCount";

	public static final String MESSAGE_ID = PREFIX + "messageId";

	public static final String RECEIVED_EXCHANGE = PREFIX + "receivedExchange";

	public static final String RECEIVED_ROUTING_KEY = PREFIX + "receivedRoutingKey";

	public static final String REDELIVERED = PREFIX + "redelivered";

	public static final String REPLY_TO = PREFIX + "replyTo";

	public static final String TIMESTAMP = PREFIX + "timestamp";

	public static final String TYPE = PREFIX + "type";

	public static final String USER_ID = PREFIX + "userId";

	public static final String SPRING_REPLY_CORRELATION = PREFIX + "springReplyCorrelation";

	public static final String SPRING_REPLY_TO_STACK = PREFIX + "springReplyToStack";

}
