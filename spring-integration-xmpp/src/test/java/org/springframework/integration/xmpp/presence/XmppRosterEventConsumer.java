package org.springframework.integration.xmpp.presence;

import org.apache.commons.lang.StringUtils;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.xmpp.XmppHeaders;

import java.util.Collection;

/**
 *
 * This class reacts to changes in {@link org.jivesoftware.smack.packet.Presence} objects for a given account.
 *
 * @author Josh Long
 * @since 2.0
 */
public class XmppRosterEventConsumer {

    @ServiceActivator
	public void presenceChanged ( Message<?> presenceEventMsg ) throws Exception {
        System.out.println(StringUtils.repeat( "-" , 100));
        String whosePresence  = (String)presenceEventMsg.getHeaders().get( XmppHeaders.PRESENCE_FROM);
        System.out.println( "entries affected:  " +  whosePresence);
        MessageHeaders messageHeaders = presenceEventMsg.getHeaders();
        for( String h : messageHeaders.keySet()  )
            System.out.println( String.format( "%s = %s", h, messageHeaders.get(h)));
	}
}
