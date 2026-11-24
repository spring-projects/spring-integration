/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.channel.config;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RendezvousChannelParserTests {

	@Test
	public void testRendezvous() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"rendezvousChannelParserTests.xml", RendezvousChannelParserTests.class);
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		assertThat(channel.getClass()).isEqualTo(RendezvousChannel.class);
		context.close();
	}

}
