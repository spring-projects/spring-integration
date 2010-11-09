/**
 * 
 */
package org.springframework.integration.xmpp.messages;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xmpp.XmppContextUtils;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * @author Oleg Zhurakousky
 *
 */
public class XmppMessageSendingMessageHandlerTests {
	

	@Test
	public void validateMessagePost() throws Exception{
		XMPPConnection connection = mock(XMPPConnection.class);
		ChatManager chantManager = mock(ChatManager.class);
		when(connection.getChatManager()).thenReturn(chantManager);
		Chat chat = mock(Chat.class);
		when(chantManager.createChat(Mockito.any(String.class), Mockito.any(MessageListener.class))).thenReturn(chat);
		
		XmppMessageSendingMessageHandler handler = new XmppMessageSendingMessageHandler(connection);
		handler.afterPropertiesSet();
		Message<?> message = MessageBuilder.withPayload("Test Message").
					setHeader(XmppHeaders.CHAT_TO_USER, "kermit@frog.com").
					build();
		// first Message
		handler.handleMessage(message);
		
		verify(chantManager, times(1)).createChat(Mockito.any(String.class), Mockito.any(MessageListener.class));
		verify(chat, times(1)).sendMessage("Test Message");
		
		// assuming we know thread ID although currently we do not provide this capability
		message = MessageBuilder.withPayload("Hello Kitty").
			setHeader(XmppHeaders.CHAT_TO_USER, "kermit@frog.com").
			setHeader(XmppHeaders.CHAT_THREAD_ID, "123").
			build();
		reset(chat, chantManager);
		when(chantManager.getThreadChat("123")).thenReturn(chat);
		
		handler.handleMessage(message);
		// in threaded conversation we need to look for existing chat
		verify(chantManager, times(0)).createChat(Mockito.any(String.class), Mockito.any(MessageListener.class));
		verify(chantManager, times(1)).getThreadChat("123");
		verify(chat, times(1)).sendMessage("Hello Kitty");
	}
	
	@Test(expected=MessageHandlingException.class)
	public void validateFailureNoChatToUser() throws Exception{	
		XmppMessageSendingMessageHandler handler = new XmppMessageSendingMessageHandler(mock(XMPPConnection.class));
		handler.handleMessage(new GenericMessage<String>("hello"));
	}
	
	@Test(expected=MessageHandlingException.class)
	public void validateMessageWithUnsupportedPayload() throws Exception{	
		XmppMessageSendingMessageHandler handler = new XmppMessageSendingMessageHandler(mock(XMPPConnection.class));
		handler.handleMessage(new GenericMessage<Integer>(123));
	}
	@Test
	public void testWithImplicitXmppConnection(){
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerSingleton(XmppContextUtils.XMPP_CONNECTION_BEAN_NAME, mock(XMPPConnection.class));
		XmppMessageSendingMessageHandler handler = new XmppMessageSendingMessageHandler();
		handler.setBeanFactory(bf);
		handler.afterPropertiesSet();
		assertNotNull(TestUtils.getPropertyValue(handler,"xmppConnection"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNoXmppConnection(){
		XmppMessageSendingMessageHandler handler = new XmppMessageSendingMessageHandler();
		handler.afterPropertiesSet();
	}
}
