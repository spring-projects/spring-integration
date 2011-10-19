/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DefaultConfiguringBeanFactoryPostProcessorTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void errorChannelRegistered() {
		Object errorChannel = context.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
		assertNotNull(errorChannel);
		assertEquals(PublishSubscribeChannel.class, errorChannel.getClass());
	}

	@Test
	public void nullChannelRegistered() {
		Object nullChannel = context.getBean(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME);
		assertNotNull(nullChannel);
		assertEquals(NullChannel.class, nullChannel.getClass());
	}

	@Test
	public void taskSchedulerRegistered() {
		Object taskScheduler = context.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME);
		assertEquals(ThreadPoolTaskScheduler.class, taskScheduler.getClass());
		Object errorHandler = new DirectFieldAccessor(taskScheduler).getPropertyValue("errorHandler");
		assertEquals(MessagePublishingErrorHandler.class, errorHandler.getClass());
		Object defaultErrorChannel = new DirectFieldAccessor(errorHandler).getPropertyValue("defaultErrorChannel");
		assertEquals(context.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME), defaultErrorChannel);
	}
	
	@Test
	public void taskSchedulerNotRegisteredMoreThanOnce() {
		ClassPathXmlApplicationContext superParentApplicationContext = new ClassPathXmlApplicationContext("superParentApplicationContext.xml", this.getClass());
		ClassPathXmlApplicationContext parentApplicationContext = 
				new ClassPathXmlApplicationContext(new String[]{"org/springframework/integration/config/xml/parentApplicationContext.xml"}, superParentApplicationContext);
		ClassPathXmlApplicationContext childApplicationContext = 
				new ClassPathXmlApplicationContext(new String[]{"org/springframework/integration/config/xml/childApplicationContext.xml"}, parentApplicationContext);
		System.out.println(Arrays.asList(childApplicationContext.getBeanDefinitionNames()));
		TaskScheduler parentScheduler = childApplicationContext.getParent().getBean("taskScheduler", TaskScheduler.class);
        TaskScheduler childScheduler = childApplicationContext.getBean("taskScheduler", TaskScheduler.class);

        assertNotNull("Child task scheduler was null", childScheduler);
        assertNotNull("Parent task scheduler was null", parentScheduler);
        assertEquals("Different schedulers in parent and child", parentScheduler, childScheduler );
	}

}
