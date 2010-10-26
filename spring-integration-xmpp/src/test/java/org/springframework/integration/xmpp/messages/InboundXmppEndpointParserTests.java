/**
 * 
 */
package org.springframework.integration.xmpp.messages;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author ozhurakousky
 *
 */
public class InboundXmppEndpointParserTests {

	@Test
	public void testInboundAdapter(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("InboundXmppEndpointParserTests-context.xml", this.getClass());
		XmppMessageDrivenEndpoint xmde = context.getBean("xmppInboundAdapter", XmppMessageDrivenEndpoint.class);
		assertFalse(xmde.isAutoStartup());
		DirectChannel channel = (DirectChannel) TestUtils.getPropertyValue(xmde, "requestChannel");
		assertEquals("xmppInbound", channel.getComponentName());
		XMPPConnection connection = (XMPPConnection)TestUtils.getPropertyValue(xmde, "xmppConnection");
		assertEquals(connection, context.getBean("testConnection"));
	}
}
