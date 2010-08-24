/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.xmpp;

import org.springframework.integration.MessageHeaders;


/**
 * Used as keys for {@link org.springframework.integration.Message} objects
 * that handle XMPP events.
 *
 * @author Mario Gray
 * @author Josh Long
 * @since 2.0
 */
public class XmppHeaders {
	private static final String PREFIX = MessageHeaders.PREFIX + "xmpp_";
	public static final String CHAT = PREFIX + "chat_key";
	public static final String CHAT_TO_USER = PREFIX + "chat_to_user";
	public static final String CHAT_THREAD_ID = PREFIX + "thread_id";
	public static final String TYPE = PREFIX + "type";
//    public static final String ROSTER_CHANGE_TYPE = PREFIX + "roster_change_type";
	public static final String PRESENCE = PREFIX + "presence";
//    public static final String ROSTER = PREFIX + "roster";

	public static final String PRESENCE_LANGUAGE = PRESENCE + "language";
	public static final String PRESENCE_PRIORITY = PRESENCE + "priority";
	public static final String PRESENCE_MODE = PRESENCE + "mode";
	public static final String PRESENCE_TYPE = PRESENCE + "type";
	public static final String PRESENCE_STATUS = PRESENCE + "status";
	public static final String PRESENCE_FROM = PRESENCE + "from";


}
