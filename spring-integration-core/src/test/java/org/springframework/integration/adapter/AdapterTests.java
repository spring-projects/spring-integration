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

package org.springframework.integration.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Mark Fisher
 */
public class AdapterTests {

	@Test
	public void testAdaptersWithBeanDefinitions() throws IOException, InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("adapterTests.xml", this.getClass());
		TestSink sink = (TestSink) context.getBean("sink");
		CountDownLatch latch = new CountDownLatch(1);
		sink.setLatch(latch);
		assertNull(sink.get());
		context.start();
		String result = null;
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		result = sink.get();
		assertNotNull(result);
		context.close();
	}

	@Test
	public void testAdaptersWithNamespace() throws IOException, InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("adapterTestsWithNamespace.xml", this.getClass());
		TestSink sink = (TestSink) context.getBean("sink");
		CountDownLatch latch = new CountDownLatch(1);
		sink.setLatch(latch);
		assertNull(sink.get());
		context.start();
		String result = null;
		latch.await(3000, TimeUnit.MILLISECONDS);
		assertEquals("latch should have counted down within allotted time", 0, latch.getCount());
		result = sink.get();
		assertNotNull(result);
		context.close();
	}

}
