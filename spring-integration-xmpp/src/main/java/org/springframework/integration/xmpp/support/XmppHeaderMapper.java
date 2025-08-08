/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.support;

import org.jivesoftware.smack.packet.MessageBuilder;

import org.springframework.integration.mapping.RequestReplyHeaderMapper;

/**
 * A convenience interface that extends {@link RequestReplyHeaderMapper}
 * but parameterized with the Smack API {@link MessageBuilder}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Florian Schmaus
 *
 * @since 2.1
 */
public interface XmppHeaderMapper extends RequestReplyHeaderMapper<MessageBuilder> {

}
