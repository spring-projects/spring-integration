/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.channel;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Executor;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class PublishSubscribeChannelTests {

	@Test
	public void testEarlySubscribe() {
		PublishSubscribeChannel channel = new PublishSubscribeChannel(mock(Executor.class));
		try {
			channel.subscribe(m -> { });
			channel.setBeanFactory(mock(BeanFactory.class));
			channel.afterPropertiesSet();
			fail("expected Exception");
		}
		catch (IllegalStateException e) {
			assertThat(e.getMessage(), equalTo("When providing an Executor, you cannot subscribe() until the channel "
					+ "bean is fully initialized by the framework. Do not subscribe in a @Bean definition"));
		}
	}

}
