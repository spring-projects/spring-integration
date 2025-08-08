/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.integration.core.GenericTransformer;
import org.springframework.messaging.Message;

/**
 * Strategy interface for transforming a {@link Message}.
 *
 * @author Mark Fisher
 */
@FunctionalInterface
public interface Transformer extends GenericTransformer<Message<?>, Message<?>> {

	Message<?> transform(Message<?> message);

}
