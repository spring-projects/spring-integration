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

package org.springframework.integration.hazelcast.listener;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.hazelcast.cluster.MembershipAdapter;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.multimap.MultiMap;

import org.springframework.integration.hazelcast.HazelcastLocalInstanceRegistrar;

/**
 * Hazelcast {@link MembershipAdapter} in order to listen for membership updates in the cluster.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastMembershipListener extends MembershipAdapter {

	@Override
	public void memberRemoved(MembershipEvent membershipEvent) {
		SocketAddress removedMemberSocketAddress =
				membershipEvent.getMember().getSocketAddress(EndpointQualifier.MEMBER);
		Set<HazelcastInstance> hazelcastLocalInstanceSet = Hazelcast.getAllHazelcastInstances();
		if (!hazelcastLocalInstanceSet.isEmpty()) {
			HazelcastInstance hazelcastInstance = hazelcastLocalInstanceSet.iterator().next();
			Lock lock =
					hazelcastInstance.getCPSubsystem()
							.getLock(HazelcastLocalInstanceRegistrar.SPRING_INTEGRATION_INTERNAL_CLUSTER_LOCK);
			lock.lock();
			try {
				MultiMap<SocketAddress, SocketAddress> configMultiMap = hazelcastInstance
						.getMultiMap(HazelcastLocalInstanceRegistrar.SPRING_INTEGRATION_INTERNAL_CLUSTER_MULTIMAP);

				if (configMultiMap.containsKey(removedMemberSocketAddress)) {
					SocketAddress newAdminSocketAddress = getNewAdminInstanceSocketAddress(
							configMultiMap, removedMemberSocketAddress);
					for (SocketAddress socketAddress : configMultiMap.values()) {
						if (!socketAddress.equals(removedMemberSocketAddress)) {
							configMultiMap.put(newAdminSocketAddress, socketAddress);
						}
					}
					configMultiMap.remove(removedMemberSocketAddress);
				}
				else {
					configMultiMap.remove(configMultiMap.keySet().iterator().next(), removedMemberSocketAddress);
				}
			}
			finally {
				lock.unlock();
			}
		}
	}

	private SocketAddress getNewAdminInstanceSocketAddress(
			MultiMap<SocketAddress, SocketAddress> configMultiMap, SocketAddress removedMemberSocketAddress) {
		for (SocketAddress socketAddress : configMultiMap.values()) {
			if (!socketAddress.equals(removedMemberSocketAddress)) {
				return socketAddress;
			}
		}

		throw new IllegalStateException("No Active Hazelcast Instance Found.");
	}

}
