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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jivesoftware.smack.packet.Presence;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.integration.xmpp.XmppHeaders;

import org.springframework.util.StringUtils;


/**
 * Implementation of the strategy interface {@link org.springframework.integration.message.OutboundMessageMapper}. This is the hook that lets the adapter receive various payloads from
 * components inside Spring Integration and forward them correctly as {@link org.jivesoftware.smack.packet.Presence} instances.
 *
 * @author Josh Long
 * @since 2.0
 */
public class XmppPresenceMessageMapper implements OutboundMessageMapper<Presence>,
    InboundMessageMapper<Presence> {
    
    private static final Log logger = LogFactory.getLog(XmppPresenceMessageMapper.class);

    /**
     * Returns a {@link org.springframework.integration.core.Message} with payload {@link org.jivesoftware.smack.packet.Presence}
     *
     * @param presence the presence object that can be used to present the priority, status, mode, and type of a given roster entry. This will be decomposed into a series of headers, as well as a payload
     * @return the Message
     * @throws Exception thrown if conversion should fail
     */
    public Message<?> toMessage(Presence presence) throws Exception {
        MessageBuilder<?> presenceMessageBuilder = MessageBuilder.withPayload(presence);
        presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_PRIORITY, presence.getPriority());
        presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_STATUS, presence.getStatus());
        presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_MODE, presence.getMode());
        presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_TYPE, presence.getType());
        presenceMessageBuilder.setHeader(XmppHeaders.PRESENCE_FROM, presence.getFrom());

        return presenceMessageBuilder.build();
    }

    /**
     * Builds a {@link org.jivesoftware.smack.packet.Presence} object from the inbound Message headers, if possible.
     *
     * @param message the Message whose headers and payload willl b
     * @return    the presence object as constructed from the {@link org.springframework.integration.core.Message} object
     * @throws Exception if there is a problem
     */
    public Presence fromMessage(Message<?> message) throws Exception {
        MessageHeaders messageHeaders = message.getHeaders();

        Integer priority = (Integer) messageHeaders.get(XmppHeaders.PRESENCE_PRIORITY);
        String status = (String) messageHeaders.get(XmppHeaders.PRESENCE_STATUS);
        String language = (String) messageHeaders.get(XmppHeaders.PRESENCE_LANGUAGE);
        String from = (String) messageHeaders.get(XmppHeaders.PRESENCE_FROM);

        // trickery afoot 
        Object modeObj = messageHeaders.get(XmppHeaders.PRESENCE_MODE);
        Presence.Mode mode  = null;

        Object typeObj = messageHeaders.get(XmppHeaders.PRESENCE_TYPE);
        Presence.Type type = null;

        if (typeObj instanceof String) {
            try {
                type = Presence.Type.valueOf((String) typeObj);
            } catch (Throwable th) {
                logger.debug("couldn't convert type header into an object of type Presence.Type");
            }
        } else if (modeObj instanceof Presence.Type) {
            type = (Presence.Type) typeObj;
        }

        if (modeObj instanceof String) {
            try {
                mode = Presence.Mode.valueOf((String) modeObj);
            } catch (Throwable th) {
                logger.debug("couldn't convert mode header into an object of type Presence.Mode ");
            }
        } else if (modeObj instanceof Presence.Mode) {
            mode = (Presence.Mode) modeObj;
        }

        Object payload = message.getPayload();

        if (null != payload) {
            if (payload instanceof Presence) {
                return (Presence) payload;
            }

            if (payload instanceof Presence.Type) {
                type = (Presence.Type) payload;
            }
        }

        return this.factoryPresence(from, status, priority, type, mode, language);
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
