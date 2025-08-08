/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.core;

import org.springframework.messaging.Message;

/**
 * Strategy interface for message selection.
 *
 * @author Mark Fisher
 */
@FunctionalInterface
public interface MessageSelector extends GenericSelector<Message<?>> {

	boolean accept(Message<?> message);

}
