/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.integration.mapping.RequestReplyHeaderMapper;

/**
 * A convenience interface that extends {@link RequestReplyHeaderMapper},
 * parameterized with {@link MessageProperties}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public interface AmqpHeaderMapper extends RequestReplyHeaderMapper<MessageProperties> {

}
