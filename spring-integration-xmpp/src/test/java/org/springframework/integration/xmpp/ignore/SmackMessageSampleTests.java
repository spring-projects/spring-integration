/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.ignore;

import org.jivesoftware.smack.packet.StanzaBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Florian Schmaus
 */
@Disabled
public class SmackMessageSampleTests {

	@Test
	public void validateSmackMessageSent() throws XmppStringprepException {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("SmackMessageSampleTest-context.xml",
				this.getClass());

		MessageChannel xmppInput = ac.getBean("xmppInput", MessageChannel.class);

		org.jivesoftware.smack.packet.Message smackMessage = StanzaBuilder.buildMessage()
				.to("springintegration@gmail.com")
				.setBody("Message sent as Smack Message")
				.build();

		Message<org.jivesoftware.smack.packet.Message> message = new GenericMessage<>(smackMessage);

		xmppInput.send(message);
		ac.close();
	}

}
