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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.gemfire.inbound.cq.CqServiceActivator;

import com.gemstone.gemfire.cache.client.Pool;
import com.gemstone.gemfire.cache.client.PoolManager;

@Configuration
@SuppressWarnings("unused")
public class CqClientConfiguration {

	@Value("${region-name}")
	private String regionName;

	@Value("${host}")
	private String host;

	@Value("${region-query}")
	private String query;

	@Value("${port}")
	private int port;

	@Value("#{cqIn}")
	private MessageChannel messageChannel;


	@Bean
	public CqServiceActivator cqServiceActivator() {
		return new CqServiceActivator();
	}

	/* todo 
   	protected ClientCache buildCache() throws Throwable {
		return new ClientCacheFactory().create();
	}
	@Bean
	public ClientCache clientCache() throws Throwable {
		return buildCache();
	}

	@Bean
	public Region<?, ?> clientRegion() throws Throwable {
		ClientRegionFactory<?, ?> clientRegionFactory = clientCache().createClientRegionFactory(ClientRegionShortcut.PROXY);
		return clientRegionFactory.create(this.regionName);
	}
*/

	@Bean
	public Pool pool() throws Throwable {
		return this.buildPool(this.host, this.port);
	}

	/*@Bean
	public ContinuousQueryMessageProducer continuousQueryMessageProducer() throws Throwable {
		ContinuousQueryMessageProducer continuousQueryMessageProducer
				= new ContinuousQueryMessageProducer( this.clientRegion() , this.pool(), this.query);
		continuousQueryMessageProducer.setDurable(true);
		continuousQueryMessageProducer.setOutputChannel(this.messageChannel);
		continuousQueryMessageProducer.setQueryName("pplQuery");
		return continuousQueryMessageProducer;
	}
*/
/*	protected CqQuery registerContinuousQuery(QueryService queryService, String name, String query, boolean durable, CqListener cqListener) throws Throwable {
		CqAttributesFactory cqAttributesFactory = new CqAttributesFactory();
		cqAttributesFactory.addCqListener(cqListener);
		CqAttributes attrs = cqAttributesFactory.create();
		CqQuery cqQuery = queryService.newCq(name, query, attrs, durable);
		cqQuery.execute();
		return cqQuery;
	}*/

	protected Pool buildPool(String host, int port) throws Throwable {
		Pool pool = PoolManager.createFactory()
				.addServer(host, port)
				.setSubscriptionEnabled(true)
				.create(host + "Pool");
		return pool;
	}

	/**
	 * the continuous query listener attached to the
	 */
	/*class MyContinuousQueryListener implements CqListener {
		public void onEvent(CqEvent cqEvent) {
			System.out.println("Received event: " +
					new ToStringCreator(cqEvent));
		}

		public void onError(CqEvent cqEvent) {
		}

		public void close() {
		}
	}*/


	public static void main(String[] args) throws Exception {
		new ClassPathXmlApplicationContext("org/springframework/integration/gemfire/inbound/cq/CqClient-context.xml");
		while (true) {
			Thread.sleep(1000 * 10);
		}
	}

}
