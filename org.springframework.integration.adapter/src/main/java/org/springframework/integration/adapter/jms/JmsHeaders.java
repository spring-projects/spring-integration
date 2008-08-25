/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.jms;

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving JMS attributes
 * from/to integration Message Headers.
 * 
 * @author Mark Fisher
 */
public abstract class JmsHeaders {

	/**
	 * Prefix for any message header that should be passed for usage by the JMS transport.
	 */
	public static final String TRANSPORT_PREFIX = "spring.integration.transport.jms.";

	/**
	 * Prefix for any user-defined message header that should be passed within JMS properties.
	 */
	public static final String USER_PREFIX = "spring.integration.user.jms.";


	public static final String CORRELATION_ID = TRANSPORT_PREFIX + "JMSCorrelationID";

	public static final String REPLY_TO = TRANSPORT_PREFIX + "JMSReplyTo";

	public static final String REDELIVERED = TRANSPORT_PREFIX + "JMSRedelivered";

	public static final String TYPE = TRANSPORT_PREFIX + "JMSType";

}
