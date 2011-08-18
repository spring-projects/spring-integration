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

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.server.CacheServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;

/**
 * Demonstrates the server side for a continuous query example. This must be run before
 * {@link org.springframework.integration.gemfire.inbound.cq.client.CqClientConfiguration}.
 *
 * This must also be run in a separate VM as the {@link org.springframework.integration.gemfire.inbound.cq.client.CqClientConfiguration}.
 *
 * @author Josh Long
 *
 */
@PropertySource("org/springframework/integration/gemfire/inbound/cq/common.properties")
@ImportResource("/org/springframework/integration/gemfire/inbound/cq/CqServer-context.xml")
@Configuration
@SuppressWarnings("unused")
public class CqServerConfiguration {


	private static Log log = LogFactory.getLog(CqServerConfiguration.class);

	@Autowired private Environment environment;

	@Value("#{c}") private Cache cache;

	@Value("#{r}") private Region<String, ?> region;

	private String regionName, host;
	private int port;

	public static void main(String[] args) throws Exception {
		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(CqServerConfiguration.class);
		GemfireTemplate gemfireTemplate = annotationConfigApplicationContext.getBean(GemfireTemplate.class);
		TaskScheduler scheduler = annotationConfigApplicationContext.getBean(TaskScheduler.class);
		BusyWorkRunnable busyWorkRunnable = new BusyWorkRunnable(gemfireTemplate);
		scheduler.scheduleAtFixedRate(busyWorkRunnable, 10 * 1000);
	}

	@PostConstruct
	public void setup() throws Throwable {
		host = this.environment.getProperty("host");
		regionName = this.environment.getProperty("region-name");
		port = Integer.parseInt(this.environment.getProperty("port"));
	}

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

	@Bean
	public TaskScheduler scheduler() {
		return new ThreadPoolTaskScheduler();
	}

	private static class BusyWorkRunnable implements Runnable {

		private GemfireTemplate gemfireTemplate;

		private String letters = "abcdefghijk";

		public BusyWorkRunnable(GemfireTemplate gemfireTemplate) {
			this.gemfireTemplate = gemfireTemplate;
		}

		public void run() {
			for (char c : letters.toCharArray()) {
				if (log.isDebugEnabled()) {
					log.debug("Adding '" + c + "'");
				}
				gemfireTemplate.put("" + c, "value-" + c);
			}
		}
	}


}