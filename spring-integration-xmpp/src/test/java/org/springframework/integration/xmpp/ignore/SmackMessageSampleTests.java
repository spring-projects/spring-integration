/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xmpp.ignore;

import org.junit.Ignore;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class SmackMessageSampleTests {

	@Test
	@Ignore
	public void validateSmackMessageSent() throws XmppStringprepException {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("SmackMessageSampleTest-context.xml",
				this.getClass());

		MessageChannel xmppInput = ac.getBean("xmppInput", MessageChannel.class);

		org.jivesoftware.smack.packet.Message smackMessage =
				new org.jivesoftware.smack.packet.Message(JidCreate.from("springintegration@gmail.com"));
		smackMessage.setBody("Message sent as Smack Message");

		Message<org.jivesoftware.smack.packet.Message> message = new GenericMessage<>(smackMessage);

		xmppInput.send(message);
		ac.close();
	}

}
