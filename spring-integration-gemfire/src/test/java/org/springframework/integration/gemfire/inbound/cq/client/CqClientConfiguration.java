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

package org.springframework.integration.gemfire.inbound.cq.client;

import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.*;
import com.gemstone.gemfire.cache.query.CqEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.gemfire.inbound.ContinuousQueryMessageProducer;
import org.springframework.integration.gemfire.inbound.cq.server.CqServerConfiguration;

import javax.annotation.PostConstruct;

/**
 * Simple example demonstrating the client side of a continuous query using Gemfire
 *
 * @author Josh Long
 *
 */
@ImportResource("/org/springframework/integration/gemfire/inbound/cq/CqClient-context.xml")
@Configuration
@PropertySource("/org/springframework/integration/gemfire/inbound/cq/common.properties")
@SuppressWarnings("unused")
public class CqClientConfiguration {

	static private Log log = LogFactory.getLog(CqClientConfiguration.class);

	private String regionName, host, query;

	private int port;

	@Autowired
	private Environment environment;

	@Autowired @Qualifier("cqIn")
	private MessageChannel messageChannel;

	public static void main(String[] args) throws Throwable {

		if (log.isInfoEnabled()) {
			log.info(String.format("Starting the %s client. Make sure to run the %s server, first.", CqServerConfiguration.class.getName(), CqClientConfiguration.class.getName()));
		}

		AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(CqClientConfiguration.class);

		long timeout = 10 * 1000;
		long counter = 0;

		while (((counter += 1) < timeout)) {
			Thread.sleep(1000);
		}
	}

	@PostConstruct
	public void setup() throws Throwable {
		this.regionName = environment.getProperty("region-name");
		this.port = Integer.parseInt(environment.getProperty("port"));
		this.host = environment.getProperty("host");
		this.query = environment.getProperty("region-query");
	}


	@Bean
	public Object cqServiceActivator() {
		return new Object() {
			@ServiceActivator
			public void handleMessage(Message<CqEvent> eventMessage) throws MessagingException {
				CqEvent cqEvent = eventMessage.getPayload();
				log.info("Received an event from the continuous query adapter: " + cqEvent);
			}
		};
	}

	@Bean
	public ClientCache clientCache() throws Throwable {
		return new ClientCacheFactory().create();
	}

	@Bean
	public Region<?, ?> clientRegion() throws Throwable {
		ClientRegionFactory<?, ?> clientRegionFactory = clientCache().createClientRegionFactory(ClientRegionShortcut.PROXY);
		return clientRegionFactory.create(this.regionName);
	}

	@Bean
	public Pool pool() throws Throwable {
		return this.buildPool(this.host, this.port);
	}

	@Bean
	public ContinuousQueryMessageProducer continuousQueryMessageProducer() throws Throwable {
		ContinuousQueryMessageProducer mp = new ContinuousQueryMessageProducer(this.clientRegion(), this.pool(), this.query);
		mp.setDurable(true);
		mp.setOutputChannel(this.messageChannel);
		mp.setQueryName("pplQuery");
		return mp;
	}


	protected Pool buildPool(String host, int port) throws Throwable {
		return PoolManager.createFactory()
				       .addServer(host, port)
				       .setSubscriptionEnabled(true)
				       .create(host + "Pool");
	}


}