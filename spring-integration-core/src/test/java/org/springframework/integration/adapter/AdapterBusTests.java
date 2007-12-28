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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Mark Fisher
 */
public class AdapterBusTests {

	@Test
	public void testAdapters() throws IOException, InterruptedException {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("adapterBusTests.xml", this.getClass());
		TestSink sink = (TestSink) context.getBean("sink");
		assertNull(sink.get());
		context.start();
		String result = null;
		int attempts = 0;
		while (result == null && attempts++ < 10) {
			Thread.sleep(5);
			result = sink.get();
		}
		assertNotNull(result);
	}

}
