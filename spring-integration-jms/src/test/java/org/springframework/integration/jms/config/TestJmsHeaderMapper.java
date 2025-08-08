/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import java.util.HashMap;
import java.util.Map;

import jakarta.jms.Message;

import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class TestJmsHeaderMapper extends JmsHeaderMapper {

	@Override
	public void fromHeaders(MessageHeaders headers, Message target) {
	}

	@Override
	public Map<String, Object> toHeaders(Message source) {
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("testProperty", "foo");
		headerMap.put("testAttribute", 123);
		return headerMap;
	}

}
