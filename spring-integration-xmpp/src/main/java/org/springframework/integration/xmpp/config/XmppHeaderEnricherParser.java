/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
