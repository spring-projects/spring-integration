package org.springframework.integration.xmpp.presence;

import org.apache.commons.lang.StringUtils;

import org.jivesoftware.smack.packet.Presence;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.*;
import org.springframework.integration.xmpp.XmppHeaders;


/**
 * This is used in {@link org.springframework.integration.xmpp.presence.OutboundXmppRosterEventsEndpointTests} to produce fake status / presence updates.
 *
 * @author Josh Long
 * @since 2.0
 */
public class XmppRosterEventProducer implements MessageSource<String> {
    public Message<String> receive() {
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            // eat it
        }
        return (Math.random() > .5) ?
               MessageBuilder.withPayload(StringUtils.EMPTY).setHeader(XmppHeaders.PRESENCE_MODE, Presence.Mode.chat).setHeader(XmppHeaders.PRESENCE_TYPE, Presence.Type.available)
                        .setHeader(XmppHeaders.PRESENCE_STATUS, "She Loves me").build()  :
               MessageBuilder.withPayload(StringUtils.EMPTY).setHeader(XmppHeaders.PRESENCE_MODE, Presence.Mode.dnd).setHeader(XmppHeaders.PRESENCE_TYPE, Presence.Type.available)
                        .setHeader(XmppHeaders.PRESENCE_STATUS, "She Loves me not").build();
    }
}
