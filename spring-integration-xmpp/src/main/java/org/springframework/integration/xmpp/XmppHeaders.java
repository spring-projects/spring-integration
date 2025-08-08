/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp;

/**
 * Used as keys for {@link org.springframework.messaging.Message} objects
 * that handle XMPP events.
 *
 * @author Mario Gray
 * @author Josh Long
 * @author Oleg Zhurakousky
 *
 * @since 2.0
 */
public final class XmppHeaders {

	private XmppHeaders() {
	}

	public static final String PREFIX = "xmpp_";

	public static final String CHAT = PREFIX + "chat";

	public static final String TO = PREFIX + "to";

	public static final String FROM = PREFIX + "from";

	public static final String THREAD = PREFIX + "thread";

	public static final String SUBJECT = PREFIX + "subject";

	public static final String TYPE = PREFIX + "type";

}
