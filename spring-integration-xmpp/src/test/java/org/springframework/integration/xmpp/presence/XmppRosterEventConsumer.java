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

package org.springframework.integration.xmpp.presence;

import org.apache.commons.lang.StringUtils;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * This class reacts to changes in
 * {@link org.jivesoftware.smack.packet.Presence} objects for a given account.
 * 
 * @author Josh Long
 * @since 2.0
 */
public class XmppRosterEventConsumer {

	@ServiceActivator
	public void presenceChanged(Message<?> presenceEventMsg) throws Exception {
		System.out.println(StringUtils.repeat("-", 100));
		String whosePresence = (String) presenceEventMsg.getHeaders().get(XmppHeaders.PRESENCE_FROM);
		System.out.println("entries affected:  " + whosePresence);
		MessageHeaders messageHeaders = presenceEventMsg.getHeaders();
		for (String h : messageHeaders.keySet()) {
			System.out.println(String.format("%s = %s", h, messageHeaders.get(h)));
		}
	}

}
