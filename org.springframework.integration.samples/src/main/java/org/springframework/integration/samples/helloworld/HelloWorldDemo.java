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

package org.springframework.integration.samples.helloworld;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.StringMessage;

/**
 * Demonstrates a basic message endpoint. 
 * 
 * @author Mark Fisher
 */
public class HelloWorldDemo {

	public static void main(String[] args) {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("helloWorldDemo.xml", HelloWorldDemo.class);
		ChannelResolver channelResolver = new BeanFactoryChannelResolver(context);
		MessageChannel inputChannel = channelResolver.resolveChannelName("inputChannel");
		PollableChannel outputChannel = (PollableChannel) channelResolver.resolveChannelName("outputChannel");
		inputChannel.send(new StringMessage("World"));
		System.out.println(outputChannel.receive(0).getPayload());
		context.stop();
	}

}
