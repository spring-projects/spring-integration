/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.messaging.Message;

/**
 * The {@link MessageGroupFactory} implementation to produce {@link SimpleMessageGroup} instances.
 * The {@link GroupType} modificator specifies the internal collection for the {@link SimpleMessageGroup}.
 * The {@link GroupType#HASH_SET} is the default type.
 *
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class SimpleMessageGroupFactory implements MessageGroupFactory {

	private final GroupType type;

	public SimpleMessageGroupFactory() {
		this(GroupType.HASH_SET);
	}

	public SimpleMessageGroupFactory(GroupType type) {
		this.type = type;
	}

	@Override
	public MessageGroup create(Object groupId) {
		return create(Collections.emptyList(), groupId);
	}

	@Override
	public MessageGroup create(Collection<? extends Message<?>> messages, Object groupId) {
		return create(messages, groupId, System.currentTimeMillis(), false);
	}

	@Override
	public MessageGroup create(Collection<? extends Message<?>> messages, Object groupId, long timestamp,
			boolean complete) {

		return new SimpleMessageGroup(this.type.get(), messages, groupId, timestamp, complete, false);
	}

	@Override
	public MessageGroup create(MessageGroupStore messageGroupStore, Object groupId) {
		if (GroupType.PERSISTENT.equals(this.type)) {
			return new PersistentMessageGroup(messageGroupStore, new SimpleMessageGroup(groupId));
		}
		else {
			return create(messageGroupStore.getMessagesForGroup(groupId), groupId);
		}
	}

	@Override
	public MessageGroup create(MessageGroupStore messageGroupStore, Object groupId, long timestamp, boolean complete) {
		if (GroupType.PERSISTENT.equals(this.type)) {
			SimpleMessageGroup original = new SimpleMessageGroup(Collections.emptyList(), groupId,
					timestamp, complete);
			return new PersistentMessageGroup(messageGroupStore, original);
		}
		else {
			return create(messageGroupStore.getMessagesForGroup(groupId), groupId, timestamp, complete);
		}
	}

	public enum GroupType {

		LIST {
			@Override
			Collection<Message<?>> get() {
				return new ArrayList<>();
			}

		},

		BLOCKING_QUEUE {
			@Override
			Collection<Message<?>> get() {
				return new LinkedBlockingQueue<>();
			}

		},

		HASH_SET {
			@Override
			Collection<Message<?>> get() {
				return new LinkedHashSet<>();
			}

		},

		SYNCHRONISED_SET {
			@Override
			Collection<Message<?>> get() {
				return Collections.synchronizedSet(new LinkedHashSet<>());
			}

		},

		PERSISTENT {
			@Override
			Collection<Message<?>> get() {
				return HASH_SET.get();
			}

		};

		abstract Collection<Message<?>> get();

	}

}
