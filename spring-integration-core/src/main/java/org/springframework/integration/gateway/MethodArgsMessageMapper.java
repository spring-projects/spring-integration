/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.springframework.integration.mapping.InboundMessageMapper;

/**
 * Implementations of this interface are {@link InboundMessageMapper}s
 * that map a {@link MethodArgsHolder} to a {@link org.springframework.messaging.Message}.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface MethodArgsMessageMapper extends InboundMessageMapper<MethodArgsHolder> {

}
