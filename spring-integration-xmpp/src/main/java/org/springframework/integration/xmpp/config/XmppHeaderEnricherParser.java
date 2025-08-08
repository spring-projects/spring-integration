/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.springframework.integration.config.xml.HeaderEnricherParserSupport;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * Parser for 'xmpp:header-enricher' element
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class XmppHeaderEnricherParser extends HeaderEnricherParserSupport {

	public XmppHeaderEnricherParser() {
		this.addElementToHeaderMapping("chat-to", XmppHeaders.TO);
		this.addElementToHeaderMapping("chat-thread-id", XmppHeaders.THREAD);
	}

}
