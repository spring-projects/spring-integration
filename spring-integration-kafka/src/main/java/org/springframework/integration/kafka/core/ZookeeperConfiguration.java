/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.core;

import java.util.List;

import com.gs.collections.api.block.function.Function;
import com.gs.collections.impl.list.mutable.FastList;
import kafka.cluster.Broker;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import scala.collection.JavaConversions;
import scala.collection.Seq;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.integration.kafka.support.ZookeeperConnect;

/**
 * Kafka {@link Configuration} that uses a ZooKeeper connection for retrieving the list of seed brokers.
 *
 * @author Marius Bogoevici
 */
public class ZookeeperConfiguration extends AbstractConfiguration {

	private final static Log log = LogFactory.getLog(ZookeeperConfiguration.class);

	public static final BrokerToBrokerAddressFunction brokerToBrokerAddressFunction = new BrokerToBrokerAddressFunction();

	private String zookeeperServers;

	private int sessionTimeout;

	private int connectionTimeout;

	public ZookeeperConfiguration(ZookeeperConnect zookeeperConnect) {
		this.zookeeperServers = zookeeperConnect.getZkConnect();
		try {
			this.sessionTimeout = Integer.parseInt(zookeeperConnect.getZkSessionTimeout());
		}
		catch (NumberFormatException e) {
			throw new BeanInitializationException("Cannot parse session timeout:", e);
		}
		try {
			this.connectionTimeout = Integer.parseInt(zookeeperConnect.getZkConnectionTimeout());
		}
		catch (NumberFormatException e) {
			throw new BeanInitializationException("Cannot parse connection timeout:", e);
		}
	}

	@Override
	protected List<BrokerAddress> doGetBrokerAddresses() {
		ZkClient zkClient = null;
		try {
			zkClient = new ZkClient(zookeeperServers, sessionTimeout, connectionTimeout, ZKStringSerializer$.MODULE$);
			Seq<Broker> allBrokersInCluster = ZkUtils$.MODULE$.getAllBrokersInCluster(zkClient);
			FastList<Broker> brokers = FastList.newList(JavaConversions.asJavaCollection(allBrokersInCluster));
			return brokers.collect(brokerToBrokerAddressFunction);
		}
		finally {
			if (zkClient != null) {
				try {
					zkClient.close();
				}
				catch (Exception e) {
					log.error("Cannot close Zookeeper client: ", e);
				}
			}
		}
	}

	@SuppressWarnings("serial")
	private static class BrokerToBrokerAddressFunction implements Function<Broker, BrokerAddress> {

		@Override
		public BrokerAddress valueOf(Broker broker) {
			return new BrokerAddress(broker.host(), broker.port());
		}

	}

}
