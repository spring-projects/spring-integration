/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.history;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.messaging.Message;

/**
 * @author Oleg Zhurakousky
 *
 */
@MessageEndpoint("outputAdapter")
public class AnnotatedAdapter implements ApplicationContextAware {

	private volatile ApplicationContext applicationContext;

	@SuppressWarnings("serial")
	public void handle(Message<?> message) {
		applicationContext.publishEvent(new ApplicationEvent(message) {

		});
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}

