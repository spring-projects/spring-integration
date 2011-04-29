package org.springframework.integration.channel.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 * 
 * @author David Turanski
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GlobalWireTapTests {
 
  
  @Autowired
  @Qualifier("channel")
  MessageChannel channel;
  
  @Autowired
  @Qualifier("random-channel")
  MessageChannel anotherChannel;
  
  
  
  @Autowired
  @Qualifier("wiretap-single")
  PollableChannel wiretapSingle;
  
  @Autowired
  @Qualifier("wiretap-all")
  PollableChannel wiretapAll;
  
  
  
  @Test
  public void testWireTapsOnTargetedChannel(){
	 Message<?> message = new GenericMessage<String>("hello");  
	 channel.send(message);
	 Message <?> wireTapMessage = wiretapSingle.receive(100);
	 assertNotNull(wireTapMessage);
 
	 //There shoud be two messages on this channel. One for 'channel' and one for 'output'
	 wireTapMessage = wiretapAll.receive(100);
	 int msgCount=0;
	 while (wireTapMessage != null){
       msgCount++;
	   assertEquals(wireTapMessage.getPayload(),message.getPayload());
	   wireTapMessage = wiretapAll.receive(100);
	 }	 
	 
	assertEquals(2,msgCount);
  }
  
  @Test
  public void testWireTapsOnRandomChannel(){
	 Message<?> message = new GenericMessage<String>("hello");  
	 anotherChannel.send(message);
	 
	 //This time no message on wiretapSingle
	 Message <?> wireTapMessage = wiretapSingle.receive(100);
	 assertNull(wireTapMessage);
	 
	 //There shoud be two messages on this channel. One for 'channel' and one for 'output'
	 wireTapMessage = wiretapAll.receive(100);
	 int msgCount=0;
	 while (wireTapMessage != null){
       msgCount++;
	   assertEquals(wireTapMessage.getPayload(),message.getPayload());
	   wireTapMessage = wiretapAll.receive(100);
	 }	 
	 
	 assertEquals(2,msgCount);
  }
  
}
