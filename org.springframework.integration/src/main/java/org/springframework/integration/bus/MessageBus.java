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

package org.springframework.integration.bus;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.factory.ChannelFactory;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.scheduling.Schedule;

/**
 * The message bus interface.
 * 
 * @author Mark Fisher
 */
public interface MessageBus extends ChannelRegistry, EndpointRegistry, Lifecycle, DisposableBean {

	MessageChannel getErrorChannel();

	ChannelFactory getChannelFactory();

	void registerHandler(String name, MessageHandler handler, Object input, Schedule schedule);

}
