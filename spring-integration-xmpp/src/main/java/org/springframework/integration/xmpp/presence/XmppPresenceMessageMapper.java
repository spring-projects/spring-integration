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

import org.jivesoftware.smack.packet.Presence;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.util.StringUtils;


/**
 * Implementation of the strategy interface {@link OutboundMessageMapper}
 * which maps {@link Presence} to {@link Message}
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class XmppPresenceMessageMapper implements OutboundMessageMapper<Presence> {

//	/**
//	 * Builds {@link Message} with payload of {@link Presence} while also 
//	 * setting Presence attributes as {@link MessageHeaders}
//	 *
//	 * @param presence the presence object
//	 * @return the Message
//	 * @throws Exception thrown if conversion should fail
//	 */
//	@SuppressWarnings("unchecked")
//	public Message<Presence> toMessage(Presence presence) throws Exception {
//		MessageBuilder<?> presenceMessageBuilder = MessageBuilder.withPayload(presence);
//		presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_PRIORITY, presence.getPriority());
//		presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_STATUS, presence.getStatus());
//		presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_MODE, presence.getMode());
//		presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_FROM, presence.getFrom());
//
//		return (Message<Presence>) presenceMessageBuilder.build();
//	}

	/**
	 * Builds a {@link Presence} object from the inbound Message headers, if possible.
	 *
	 * @param message the Message whose headers and payload willl b
	 * @return the presence object as constructed from the {@link org.springframework.integration.Message} object
	 * @throws Exception if there is a problem
	 */
	public Presence fromMessage(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		if (payload instanceof Presence) {
			return (Presence) payload;
		}
		else if (payload instanceof Presence.Type) {
			Presence.Type presenceType = (Presence.Type) payload;
			MessageHeaders messageHeaders = message.getHeaders();

			Integer priority = (Integer) messageHeaders.get(XmppHeaders.PRESENCE_PRIORITY);
			String status = (String) messageHeaders.get(XmppHeaders.PRESENCE_STATUS);
			String language = (String) messageHeaders.get(XmppHeaders.PRESENCE_LANGUAGE);
			String from = (String) messageHeaders.get(XmppHeaders.PRESENCE_FROM);

			Object modeObj = messageHeaders.get(XmppHeaders.PRESENCE_MODE);
			Presence.Mode mode = null;

			if (modeObj != null){
				if (modeObj instanceof String) {
					mode = Presence.Mode.valueOf((String) modeObj);
				} 
				else if (modeObj instanceof Presence.Mode) {
					mode = (Presence.Mode) modeObj;
				}
				else {
					throw new MessageMappingException("Unsupported type for Presence mode. Only" +
							" String or Presence.Mode is allowed, but was: " + modeObj.getClass().getName());
				}
			}
			return this.factoryPresence(from, status, priority, presenceType, mode, language);
		}
		else {
			throw new MessageMappingException("Unsupported Payload type. " +
					"The only supported payload type is org.jivesoftware.smack.packet.Presence: " + payload.getClass().getName());
		}
	}

	private Presence factoryPresence(String from, String status, Integer priority,
	                                 Presence.Type type, Presence.Mode mode, String language) {
		if (null == type) {
			type = Presence.Type.available;
		}

		Presence presence = new Presence(type);

		if (null != priority) {
			presence.setPriority(priority);
		}

		if (StringUtils.hasText(status)) {
			presence.setStatus(status);
		}

		if (StringUtils.hasText(from)) {
			presence.setFrom(from);
		}

		if (null != mode) {
			presence.setMode(mode);
		}

		if (StringUtils.hasText(language)) {
			presence.setLanguage(language);
		}

		return presence;
	}
}
