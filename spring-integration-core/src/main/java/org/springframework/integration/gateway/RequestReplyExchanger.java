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

package org.springframework.integration.gateway;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * Interface for a request/reply Message exchange. This will be used as a default
 * by {@link GatewayProxyFactoryBean} if no 'service-interface' property has been provided.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@FunctionalInterface
public interface RequestReplyExchanger {

	Message<?> exchange(Message<?> request) throws MessagingException;

}
