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

package org.springframework.integration.gemfire.inbound.cq.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.gemfire.GemfireTemplate;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.server.CacheServer;

@Configuration
@SuppressWarnings("unused")
public class CqServerConfiguration {

	@Value("#{c}")
	private Cache cache;

	@Value("#{r}")
	private Region<String, ?> region;

	@Value("${region-name}")
	private String regionName;

	@Value("${host}")
	private String host;

	@Value("${port}")
	private int port;


	@Bean
	public GemfireTemplate gemfireTemplate() {
		return new GemfireTemplate(this.region);
	}

	@Bean
	public CacheServer cacheServer() throws Throwable {
		CacheServer cacheServer = this.cache.addCacheServer();
		cacheServer.setBindAddress(this.host);
		cacheServer.setPort(this.port);
		cacheServer.start();
		return cacheServer;
	}

	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"org/springframework/integration/gemfire/inbound/cq/CqServer-context.xml");
		applicationContext.registerShutdownHook();
		applicationContext.start();
		GemfireTemplate gemfireTemplate = applicationContext.getBean(GemfireTemplate.class);
		String letters = "abcdefghijk";
		while (true) {
			Thread.sleep(1000 * 10);
			for (char c : letters.toCharArray()) {
				gemfireTemplate.put("" + c, "value-" + c);
			}
		}
	}

}
