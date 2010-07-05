package org.springframework.integration.xmpp.presence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * this class demonstrates that when I launch this and then manipulate the status of the
 * user using Pidgin, the updated state is immediately delivered to the Spring Integration bus.
 * 
 * @author Josh Long
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class InboundXmppRosterEventsEndpointTests {

	@Test
	public void run() throws Exception {
		Thread.sleep( 60 *  1000);
	}

}
