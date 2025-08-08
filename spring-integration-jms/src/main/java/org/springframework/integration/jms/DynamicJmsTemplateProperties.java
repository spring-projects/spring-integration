/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0.2
 */
abstract class DynamicJmsTemplateProperties {

	private static final ThreadLocal<Integer> PRIORITY_HOLDER = new ThreadLocal<>();

	private static final ThreadLocal<Long> RECEIVE_TIMEOUT_HOLDER = new ThreadLocal<>();

	private static final ThreadLocal<Integer> DELIVER_MODE_HOLDER = new ThreadLocal<>();

	private static final ThreadLocal<Long> TIME_TO_LIVE_HOLDER = new ThreadLocal<>();

	private DynamicJmsTemplateProperties() {
	}

	public static Integer getPriority() {
		return PRIORITY_HOLDER.get();
	}

	public static void setPriority(Integer priority) {
		PRIORITY_HOLDER.set(priority);
	}

	public static void clearPriority() {
		PRIORITY_HOLDER.remove();
	}

	public static Long getReceiveTimeout() {
		return RECEIVE_TIMEOUT_HOLDER.get();
	}

	public static void setReceiveTimeout(Long receiveTimeout) {
		RECEIVE_TIMEOUT_HOLDER.set(receiveTimeout);
	}

	public static void clearReceiveTimeout() {
		RECEIVE_TIMEOUT_HOLDER.remove();
	}

	public static Integer getDeliveryMode() {
		return DELIVER_MODE_HOLDER.get();
	}

	public static void setDeliveryMode(Integer deliveryMode) {
		DELIVER_MODE_HOLDER.set(deliveryMode);
	}

	public static void clearDeliveryMode() {
		DELIVER_MODE_HOLDER.remove();
	}

	public static Long getTimeToLive() {
		return TIME_TO_LIVE_HOLDER.get();
	}

	public static void setTimeToLive(Long timeToLive) {
		TIME_TO_LIVE_HOLDER.set(timeToLive);
	}

	public static void clearTimeToLive() {
		TIME_TO_LIVE_HOLDER.remove();
	}

}
