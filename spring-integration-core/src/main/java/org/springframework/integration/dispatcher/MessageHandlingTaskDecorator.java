/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.integration.dispatcher;

import org.springframework.messaging.support.MessageHandlingRunnable;

/**
 * The strategy to decorate {@link MessageHandlingRunnable} tasks
 * to be used with the {@link java.util.concurrent.Executor}.
 *
 * @author Artem Bilan
 * @since 4.2
 * @see UnicastingDispatcher
 * @see BroadcastingDispatcher
 */
@FunctionalInterface
public interface MessageHandlingTaskDecorator {

	Runnable decorate(MessageHandlingRunnable task);

}
