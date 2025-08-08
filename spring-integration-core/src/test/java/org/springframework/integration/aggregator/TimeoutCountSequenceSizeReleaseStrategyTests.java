/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.junit.Test;

import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artme Bilan
 * @author Peter Uhlenbruck
 */
public class TimeoutCountSequenceSizeReleaseStrategyTests {

	@Test
	public void testIncompleteList() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy = new TimeoutCountSequenceSizeReleaseStrategy();
		assertThat(releaseStrategy.canRelease(messages)).isFalse();
	}

	@Test
	public void testIncompleteListWithTimeout() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy =
				new TimeoutCountSequenceSizeReleaseStrategy(TimeoutCountSequenceSizeReleaseStrategy.DEFAULT_THRESHOLD,
						-100);
		assertThat(releaseStrategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testIncompleteListWithTimeoutForMultipleMessages() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceSize(3).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceSize(3).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message1);
		messages.add(message2);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy =
				new TimeoutCountSequenceSizeReleaseStrategy(TimeoutCountSequenceSizeReleaseStrategy.DEFAULT_THRESHOLD,
						-100);
		assertThat(releaseStrategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testIncompleteListWithCount() {
		Message<String> message = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message);
		TimeoutCountSequenceSizeReleaseStrategy releaseStrategy =
				new TimeoutCountSequenceSizeReleaseStrategy(1,
						TimeoutCountSequenceSizeReleaseStrategy.DEFAULT_TIMEOUT);
		assertThat(releaseStrategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testCompleteList() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceSize(2).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceSize(2).build();
		SimpleMessageGroup messages = new SimpleMessageGroup("FOO");
		messages.add(message1);
		messages.add(message2);
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		assertThat(releaseStrategy.canRelease(messages)).isTrue();
	}

	@Test
	public void testEmptyList() {
		SequenceSizeReleaseStrategy releaseStrategy = new SequenceSizeReleaseStrategy();
		assertThat(releaseStrategy.canRelease(new SimpleMessageGroup("FOO"))).isTrue();
	}

}
