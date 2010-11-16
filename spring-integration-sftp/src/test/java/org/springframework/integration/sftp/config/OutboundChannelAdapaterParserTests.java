/**
 * 
 */
package org.springframework.integration.sftp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.sftp.outbound.SftpSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author ozhurakousky
 *
 */
public class OutboundChannelAdapaterParserTests {

	@Test
	public void testOutboundChannelAdapaterWithId(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("OutboundChannelAdapaterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapter");
		assertTrue(consumer instanceof EventDrivenConsumer);
		assertEquals(context.getBean("inputChannel"), TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("sftpOutboundAdapter", ((EventDrivenConsumer)consumer).getComponentName());
		SftpSendingMessageHandler handler = (SftpSendingMessageHandler) TestUtils.getPropertyValue(consumer, "handler");
		assertEquals(context.getBean("fileNameGenerator"), TestUtils.getPropertyValue(handler, "fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "charset"));
//		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryBufferFolder"));
//		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryBufferFolderFile"));
//		FtpClientPool clientPoll = (FtpClientPool) TestUtils.getPropertyValue(handler, "ftpClientPool");
//		FtpClientFactory<?> clientFactory = (FtpClientFactory<?>) TestUtils.getPropertyValue(clientPoll, "factory");
//		assertEquals("localhost", TestUtils.getPropertyValue(clientFactory, "host"));
//		assertEquals(22, TestUtils.getPropertyValue(clientFactory, "port"));
	}
	
	@Test
	public void testOutboundChannelAdapaterWithNoId(){
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("OutboundChannelAdapaterParserTests-context.xml", this.getClass());
	}
}
