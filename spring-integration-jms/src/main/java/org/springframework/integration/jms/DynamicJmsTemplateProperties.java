/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0.2
 */
abstract class DynamicJmsTemplateProperties {

	private static final ThreadLocal<Integer> priorityHolder = new ThreadLocal<>();

	private static final ThreadLocal<Long> receiveTimeoutHolder = new ThreadLocal<>();

	private static final ThreadLocal<Integer> deliverModeHolder = new ThreadLocal<>();

	private static final ThreadLocal<Long> timeToLiveHolder = new ThreadLocal<>();


	private DynamicJmsTemplateProperties() {
	}

	public static Integer getPriority() {
		return priorityHolder.get();
	}

	public static void setPriority(Integer priority) {
		priorityHolder.set(priority);
	}

	public static void clearPriority() {
		priorityHolder.remove();
	}

	public static Long getReceiveTimeout() {
		return receiveTimeoutHolder.get();
	}

	public static void setReceiveTimeout(Long receiveTimeout) {
		receiveTimeoutHolder.set(receiveTimeout);
	}

	public static void clearReceiveTimeout() {
		receiveTimeoutHolder.remove();
	}

	public static Integer getDeliveryMode() {
		return deliverModeHolder.get();
	}

	public static void setDeliveryMode(Integer deliveryMode) {
		deliverModeHolder.set(deliveryMode);
	}

	public static void clearDeliveryMode() {
		deliverModeHolder.remove();
	}

	public static Long getTimeToLive() {
		return timeToLiveHolder.get();
	}

	public static void setTimeToLive(Long timeToLive) {
		timeToLiveHolder.set(timeToLive);
	}

	public static void clearTimeToLive() {
		timeToLiveHolder.remove();
	}

}
