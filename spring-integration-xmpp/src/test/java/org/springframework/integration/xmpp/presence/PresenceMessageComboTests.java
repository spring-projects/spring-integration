package org.springframework.integration.xmpp.presence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * This class will demonstrate using both inbound adapter types in a 1-2 punch of:
 * <UL>
 *  <LI> notifying the bus of a user's sudden online availability using &lt;xmpp:roster-event-inbound-channel-adapter&gt;</LI>
 *  <LI> sending that user a message using the &lt;xmpp:outbound-message-channel-adapter /&gt;</LI>
 * </UL>
 *
 * @author Josh Long
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PresenceMessageComboTests {

    @Test
    public void run () throws Throwable {
   	Thread.sleep( 60 *  1000);
    }

}
