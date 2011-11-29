/*
 * Copyright 2011 the original author or authors.
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

package org.springframework.integration.gemfire.fork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

import com.gemstone.gemfire.cache.AttributesFactory;
import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.DataPolicy;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.server.CacheServer;
import com.gemstone.gemfire.distributed.DistributedSystem;

/**
 * @author Costin Leau
 * @author David Turanski
 * 
 * Runs as a standalone Java app.
 * Modified from SGF implementation for testing client/server CQ features
 */
public class CacheServerProcess {

	@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
	public static void main(String[] args) throws Exception {

		Properties props = new Properties();
		props.setProperty("name", "CacheServer");
		props.setProperty("log-level", "info");

		System.out.println("\nConnecting to the distributed system and creating the cache.");
		
		DistributedSystem ds = DistributedSystem.connect(props);
		Cache cache = CacheFactory.create(ds);

		// Create region.
		AttributesFactory factory = new AttributesFactory();
		factory.setDataPolicy(DataPolicy.REPLICATE);
		factory.setScope(Scope.DISTRIBUTED_ACK);
		Region testRegion = cache.createRegion("test", factory.create());
		System.out.println("Test region, " + testRegion.getFullPath() + ", created in cache.");

		// Start Cache Server.
		CacheServer server = cache.addCacheServer();
		server.setPort(40404);
		server.setNotifyBySubscription(true);
		System.out.println("Starting server");
		server.start();
		ForkUtil.createControlFile(CacheServerProcess.class.getName());
		System.out.println("Waiting for shutdown");
	 
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		bufferedReader.readLine();

	}
}
