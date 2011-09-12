/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.twitter.config;


/**
 * @author Oleg Zhurakousky
 */
public class TestReceivingMessageSourceParserTests {

	
// NO LONGER RELEVANT...
//	@Test
//	public void testRecievingAdapterConfigurationAutoStartup(){
//		ApplicationContext ac = new ClassPathXmlApplicationContext("TestReceivingMessageSourceParser-context.xml", this.getClass());
//		SourcePollingChannelAdapter spca = ac.getBean("mentionAdapter", SourcePollingChannelAdapter.class);
//		SmartLifecycle ms = TestUtils.getPropertyValue(spca, "source", SmartLifecycle.class);
//		assertFalse(ms.isAutoStartup());
//
//		spca = ac.getBean("dmAdapter", SourcePollingChannelAdapter.class);
//		ms = TestUtils.getPropertyValue(spca, "source", SmartLifecycle.class);
//		assertFalse(ms.isAutoStartup());
//		
//		spca = ac.getBean("updateAdapter", SourcePollingChannelAdapter.class);
//		ms = TestUtils.getPropertyValue(spca, "source", SmartLifecycle.class);
//		assertFalse(ms.isAutoStartup());
//	}


//	public static class TwitterTemplateFactoryBean implements FactoryBean<Twitter>{
//
//		public Twitter getObject() throws Exception {
//			Twitter twitter = mock(Twitter.class);
//			//when(oper.getProfileId()).thenReturn("kermit");
//			return oper;
//		}
//
//		public Class<?> getObjectType() {
//			return TwitterOperations.class;
//		}
//
//		public boolean isSingleton() {
//			return true;
//		}
//	}

}
