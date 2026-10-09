/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.config;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FileSplitterParserTests {

	@Autowired
	private EventDrivenConsumer fullBoat;

	@Autowired
	private FileSplitter splitter;

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel out;

	@Test
	public void testComplete() {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.splitter, "returnIterator")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.splitter, "markers")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.splitter, "markersJson")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.splitter, "requiresReply")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.splitter, "applySequence")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(this.splitter, "charset"))
				.isEqualTo(StandardCharsets.UTF_8);
		assertThat(TestUtils.<Long>getPropertyValue(this.splitter, "messagingTemplate.sendTimeout"))
				.isEqualTo(5L);
		assertThat(TestUtils.<String>getPropertyValue(this.splitter, "firstLineHeaderName"))
				.isEqualTo("foo");
		assertThat(TestUtils.<String>getPropertyValue(this.splitter, "discardChannelName"))
				.isEqualTo("nullChannel");
		assertThat(this.splitter.getOutputChannel()).isSameAs(this.out);
		assertThat(this.splitter.getOrder()).isEqualTo(2);
		assertThat(this.fullBoat.getInputChannel()).isSameAs(this.in);
		assertThat(this.fullBoat.isAutoStartup()).isFalse();
		assertThat(this.fullBoat.getPhase()).isEqualTo(1);
	}

}
