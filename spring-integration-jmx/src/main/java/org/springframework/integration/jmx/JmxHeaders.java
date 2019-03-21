/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jmx;

/**
 * Constants for JMX related Message Header keys.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class JmxHeaders {

	public static final String PREFIX = "jmx_";

	public static final String OBJECT_NAME = PREFIX + "objectName";

	public static final String OPERATION_NAME = PREFIX + "operationName";

	public static final String NOTIFICATION_TYPE = PREFIX + "notificationType";

	public static final String NOTIFICATION_HANDBACK = PREFIX + "notificationHandback";

}
