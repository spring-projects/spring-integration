/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.springframework.integration.handler.MessageProcessor;

/**
 * @param <T> the payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 3.0
 */
public interface HeaderValueMessageProcessor<T> extends MessageProcessor<T> {

	Boolean isOverwrite();

}
