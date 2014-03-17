/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.channel.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 *
 * @author David Turanski
 * @author Gary Russell
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

	 //There should be three messages on this channel. One for 'channel', one for 'output', and one for 'wiretapSingle'.
	 wireTapMessage = wiretapAll.receive(100);
	 int msgCount=0;
	 while (wireTapMessage != null){
       msgCount++;
	   assertEquals(wireTapMessage.getPayload(),message.getPayload());
	   wireTapMessage = wiretapAll.receive(100);
	 }

	assertEquals(3,msgCount);
  }

  @Test
  public void testWireTapsOnRandomChannel(){
	 Message<?> message = new GenericMessage<String>("hello");
	 anotherChannel.send(message);

	 //This time no message on wiretapSingle
	 Message <?> wireTapMessage = wiretapSingle.receive(100);
	 assertNull(wireTapMessage);

	 //There should be two messages on this channel. One for 'channel' and one for 'output'
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
