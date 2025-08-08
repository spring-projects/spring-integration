/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.mqtt.core;

import org.springframework.beans.factory.BeanNameAware;

/**
 * A component that interfaces with MQTT.
 *
 * @param <T> The connection information type.
 *
 * @author Gary Russell
 * @since 2.5
 *
 */
public interface MqttComponent<T> extends BeanNameAware {

	/**
	 * Return this component's bean name.
	 * @return the bean name.
	 */
	String getBeanName();

	/**
	 * Return information about the connection.
	 * @return the information.
	 */
	T getConnectionInfo();

}
