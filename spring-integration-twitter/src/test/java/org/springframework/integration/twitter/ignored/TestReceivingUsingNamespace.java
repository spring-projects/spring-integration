/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.twitter.ignored;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class TestReceivingUsingNamespace {

	@Test
	@Ignore
	/*
	 * In order to run this test you need to provide oauth properties in sample.properties on the classpath.
	 */
	public void testUpdatesWithRealTwitter() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		ConfigurableApplicationContext ctx =
				new ClassPathXmlApplicationContext("TestReceivingUsingNamespace-context.xml", this.getClass());
		latch.await(10000, TimeUnit.SECONDS);
		ctx.close();
	}
}
