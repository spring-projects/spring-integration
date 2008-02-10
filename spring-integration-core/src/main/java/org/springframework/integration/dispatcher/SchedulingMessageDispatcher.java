/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import org.springframework.context.Lifecycle;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.scheduling.Schedule;

/**
 * An extension to the {@link MessageDispatcher} strategy for handlers that may
 * be scheduled.
 * 
 * @author Mark Fisher
 */
public interface SchedulingMessageDispatcher extends MessageDispatcher, Lifecycle {

	void setDefaultSchedule(Schedule defaultSchedule);

	void addHandler(MessageHandler handler, Schedule schedule);

}
