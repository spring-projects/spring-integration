/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.Message;

import org.springframework.integration.mapping.HeaderMapper;

/**
 * Strategy interface for mapping integration Message headers to an outbound
 * JMS Message (e.g. to configure JMS properties) or extracting integration
 * header values from an inbound JMS Message.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class JmsHeaderMapper implements HeaderMapper<Message> {

	protected static final String CONTENT_TYPE_PROPERTY = "content_type";

}

