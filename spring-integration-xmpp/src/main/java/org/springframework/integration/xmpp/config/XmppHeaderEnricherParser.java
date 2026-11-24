/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	@SuppressWarnings("this-escape")
	public XmppHeaderEnricherParser() {
		this.addElementToHeaderMapping("chat-to", XmppHeaders.TO);
		this.addElementToHeaderMapping("chat-thread-id", XmppHeaders.THREAD);
	}

}
