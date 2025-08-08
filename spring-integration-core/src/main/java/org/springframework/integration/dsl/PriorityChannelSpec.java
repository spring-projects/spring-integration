/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.Comparator;

import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.integration.store.PriorityCapableChannelMessageStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class PriorityChannelSpec extends MessageChannelSpec<PriorityChannelSpec, PriorityChannel> {

	private int capacity;

	private Comparator<Message<?>> comparator;

	private MessageGroupQueue messageGroupQueue;

	protected PriorityChannelSpec() {
	}

	public PriorityChannelSpec capacity(int capacity) {
		this.capacity = capacity;
		return this;
	}

	public PriorityChannelSpec comparator(Comparator<Message<?>> comparator) {
		this.comparator = comparator;
		return this;
	}

	public PriorityChannelSpec messageStore(PriorityCapableChannelMessageStore messageGroupStore, Object groupId) {
		this.messageGroupQueue = new MessageGroupQueue(messageGroupStore, groupId);
		this.messageGroupQueue.setPriority(true);
		return this;
	}

	@Override
	protected PriorityChannel doGet() {
		Assert.state(!(this.comparator != null && this.messageGroupQueue != null),
				"Only one of 'comparator' or 'messageGroupStore' can be specified.");

		if (this.messageGroupQueue != null) {
			this.channel = new PriorityChannel(this.messageGroupQueue);
		}
		else {
			this.channel = new PriorityChannel(this.capacity, this.comparator);
		}

		return super.doGet();
	}

}
