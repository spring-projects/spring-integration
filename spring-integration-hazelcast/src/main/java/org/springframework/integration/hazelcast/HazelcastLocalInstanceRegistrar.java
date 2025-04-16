/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.hazelcast;

import java.net.SocketAddress;
import java.util.concurrent.locks.Lock;

import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.integration.hazelcast.listener.HazelcastMembershipListener;

/**
 * This class creates an internal configuration {@link MultiMap} to cache Hazelcast instances' socket
 * address information which used Hazelcast event-driven inbound channel adapter(s). It
 * also enables a Hazelcast {@link MembershipListener} to listen for
 * membership updates.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastLocalInstanceRegistrar implements SmartInitializingSingleton {

	private static final Log logger = LogFactory.getLog(HazelcastLocalInstanceRegistrar.class);

	/**
	 * The bean name for the {@link HazelcastLocalInstanceRegistrar} instance.
	 */
	public static final String BEAN_NAME = "hazelcastLocalInstanceRegistrar";

	/**
	 * The name for the Hazelcast MultiMap used for membership registration.
	 */
	public static final String SPRING_INTEGRATION_INTERNAL_CLUSTER_MULTIMAP =
			"SPRING_INTEGRATION_INTERNAL_CLUSTER_MULTIMAP";

	/**
	 * The name for the Hazelcast Lock used for membership registration.
	 */
	public static final String SPRING_INTEGRATION_INTERNAL_CLUSTER_LOCK = "SPRING_INTEGRATION_INTERNAL_CLUSTER_LOCK";

	private final HazelcastInstance hazelcastInstance;

	/**
	 * Construct {@link HazelcastLocalInstanceRegistrar} based on the local JVM {@link HazelcastInstance}s if any.
	 */
	public HazelcastLocalInstanceRegistrar() {
		this.hazelcastInstance = null;
	}

	/**
	 * Construct {@link HazelcastLocalInstanceRegistrar} based on the provided {@link HazelcastInstance}.
	 * @param hazelcastInstance the {@link HazelcastInstance} to use.
	 */
	public HazelcastLocalInstanceRegistrar(HazelcastInstance hazelcastInstance) {
		this.hazelcastInstance = hazelcastInstance;
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (this.hazelcastInstance == null) {
			if (!Hazelcast.getAllHazelcastInstances().isEmpty()) {
				HazelcastInstance anyHazelcastInstance = Hazelcast.getAllHazelcastInstances().iterator().next();
				anyHazelcastInstance.getCluster().addMembershipListener(new HazelcastMembershipListener());
				syncConfigurationMultiMap(anyHazelcastInstance);
			}
			else {
				logger.warn("No HazelcastInstances for MembershipListener registration");
			}
		}
		else {
			syncConfigurationMultiMap(this.hazelcastInstance);
			this.hazelcastInstance.getCluster().addMembershipListener(new HazelcastMembershipListener());
		}
	}

	private void syncConfigurationMultiMap(HazelcastInstance hazelcastInstance) {
		Lock lock = hazelcastInstance.getCPSubsystem().getLock(SPRING_INTEGRATION_INTERNAL_CLUSTER_LOCK);
		lock.lock();
		try {
			MultiMap<SocketAddress, SocketAddress> multiMap = hazelcastInstance
					.getMultiMap(SPRING_INTEGRATION_INTERNAL_CLUSTER_MULTIMAP);
			for (HazelcastInstance localInstance : Hazelcast.getAllHazelcastInstances()) {
				SocketAddress localInstanceSocketAddress = localInstance.getLocalEndpoint().getSocketAddress();
				if (multiMap.size() == 0) {
					multiMap.put(localInstanceSocketAddress, localInstanceSocketAddress);
				}
				else {
					multiMap.put(multiMap.keySet().iterator().next(), localInstanceSocketAddress);
				}
			}
		}
		finally {
			lock.unlock();
		}
	}

}
