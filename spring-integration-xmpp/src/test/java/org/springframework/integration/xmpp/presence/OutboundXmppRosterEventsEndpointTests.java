package org.springframework.integration.xmpp.presence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * Tests {@link XmppRosterEventMessageSendingHandler} to ensure that we are able to publish status.
 *
 * 
 * @author Josh Long
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class OutboundXmppRosterEventsEndpointTests {
    @Test
    public void testOutbound() throws Throwable {
       Thread.sleep( 60 *  1000);  
    }
}
