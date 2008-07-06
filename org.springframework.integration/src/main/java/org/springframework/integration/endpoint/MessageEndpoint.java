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

package org.springframework.integration.endpoint;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.scheduling.Schedule;

/**
 * Base interface for message endpoints.
 * 
 * @author Mark Fisher
 */
public interface MessageEndpoint extends MessageTarget, ChannelRegistryAware, InitializingBean {

	void setName(String name);

	String getName();

	Schedule getSchedule();

	EndpointTrigger getTrigger();

	String getInputChannelName();

	MessageChannel getInputChannel();

	String getOutputChannelName();

	MessageChannel getOutputChannel();

	boolean poll();

}
