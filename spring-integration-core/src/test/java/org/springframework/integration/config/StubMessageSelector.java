/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class StubMessageSelector implements MessageSelector, BeanNameAware {

	private String beanName;

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public boolean accept(Message<?> message) {
		return true;
	}

	public String toString() {
		return this.beanName;
	}

}
