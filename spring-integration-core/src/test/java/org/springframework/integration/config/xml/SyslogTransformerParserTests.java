/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.SyslogToMapTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Karol Dowbecki
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
public class SyslogTransformerParserTests {

	@Autowired
	private MessageChannel toMapChannel;

	@Autowired
	private PollableChannel out;

	@Test
	public void testMap() {
		toMapChannel.send(new GenericMessage<>("<157>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE"));
		Map<?, ?> map = (Map<?, ?>) out.receive(1000).getPayload();
		assertThat(map).isNotNull();
		assertThat(map.size()).isEqualTo(6);
		assertThat(map.get(SyslogToMapTransformer.FACILITY)).isEqualTo(19);
		assertThat(map.get(SyslogToMapTransformer.SEVERITY)).isEqualTo(5);
		Object date = map.get(SyslogToMapTransformer.TIMESTAMP);
		assertThat(date instanceof Date || date instanceof String).isTrue();
		assertThat(map.get(SyslogToMapTransformer.HOST)).isEqualTo("WEBERN");
		assertThat(map.get(SyslogToMapTransformer.TAG)).isEqualTo("TESTING");
		assertThat(map.get(SyslogToMapTransformer.MESSAGE)).isEqualTo("[70729]: TEST SYSLOG MESSAGE");
	}

}
