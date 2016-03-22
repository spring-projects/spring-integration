/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.jms;

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving JMS
 * attributes from/to integration Message Headers.
 *
 * @deprecated - use {@link org.springframework.jms.support.JmsHeaders.JmsHeaders}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
@Deprecated
public abstract class JmsHeaders {

	/**
	 * Prefix used for JMS API related headers in order to distinguish from
	 * user-defined headers and other internal headers (e.g. correlationId).
	 * @see DefaultJmsHeaderMapper
	 */
	public static final String PREFIX = org.springframework.jms.support.JmsHeaders.PREFIX;

	public static final String MESSAGE_ID = org.springframework.jms.support.JmsHeaders.MESSAGE_ID;

	public static final String CORRELATION_ID = org.springframework.jms.support.JmsHeaders.CORRELATION_ID;

	public static final String REPLY_TO = org.springframework.jms.support.JmsHeaders.REPLY_TO;

	public static final String REDELIVERED = org.springframework.jms.support.JmsHeaders.REDELIVERED;

	public static final String TYPE = org.springframework.jms.support.JmsHeaders.TYPE;

	public static final String TIMESTAMP = org.springframework.jms.support.JmsHeaders.TIMESTAMP;

}
