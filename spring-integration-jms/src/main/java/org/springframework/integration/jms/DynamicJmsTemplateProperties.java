/*
 * Copyright 2002-2020 the original author or authors.
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
