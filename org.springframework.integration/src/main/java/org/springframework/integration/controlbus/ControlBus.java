/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.controlbus;

import org.springframework.integration.channel.SubscribableChannel;

/**
 * Defines base interface for implementation of the Control Bus EIP pattern ({@linkplain http://www.eaipatterns.com/ControlBus.html})
 * Control Bus infrastructure is assembled with various components provided by the framework with the main entry point 
 * being {@link SubscribableChannel} making this interface more of a convenience wrapper over target {@link SubscribableChannel}
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public interface ControlBus extends SubscribableChannel {
	public String getBusName();
}
