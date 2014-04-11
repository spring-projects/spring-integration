/*
 * Copyright 2014 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.social.twitter.api.SearchParameters;

/**
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public class TestSearchOutboundGateway {

	@Test
	@Ignore
	/*
	 * In order to run this test you need to provide oauth properties in sample.properties on the classpath.
	 */
	public void testSearch() throws Exception{
		ConfigurableApplicationContext ctx =
				new ClassPathXmlApplicationContext("TestSearchOutboundGateway-context.xml", this.getClass());
		MessageChannel search = ctx.getBean("search", MessageChannel.class);
		search.send(new GenericMessage<String>("#springintegration"));
		Thread.sleep(10000);
		search.send(new GenericMessage<SearchParameters>(new SearchParameters("#springintegration").count(5)));
		Thread.sleep(10000);
		search.send(new GenericMessage<SearchParameters>(new SearchParameters("#jjjjunk").count(5)));
		Thread.sleep(10000);
		ctx.close();
	}
}
