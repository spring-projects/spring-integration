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
 * Keys to be used for setting and/or retrieving JMS attributes stored in the
 * integration message header.
 * 
 * @author Mark Fisher
 */
public abstract class JmsAttributeKeys {

	public static final String USER_DEFINED_ATTRIBUTE_PREFIX = "jms.";

	public static final String CORRELATION_ID = "_jms.JMSCorrelationID";

	public static final String REPLY_TO = "_jms.JMSReplyTo";

	public static final String REDELIVERED = "_jms.JMSRedelivered";

	public static final String TYPE = "_jms.JMSType";

}
