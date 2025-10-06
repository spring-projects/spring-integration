/*
 * Copyright 2002-present the original author or authors.
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

import javax.management.Notification;
import javax.management.NotificationListener;

/**
 * A JMX {@link NotificationListener} implementation that will send Messages
 * containing the JMX {@link Notification} instances as their payloads.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.NotificationListeningMessageProducer}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class NotificationListeningMessageProducer extends
		org.springframework.integration.jmx.inbound.NotificationListeningMessageProducer {

}
