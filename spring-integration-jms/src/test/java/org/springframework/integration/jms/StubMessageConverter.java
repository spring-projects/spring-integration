/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * @author Mark Fisher
 */
public class StubMessageConverter implements MessageConverter {

	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		return null;
	}

	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		return null;
	}

}
