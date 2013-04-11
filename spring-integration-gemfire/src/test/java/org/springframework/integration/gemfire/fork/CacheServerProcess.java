/*
 * Copyright 2013 the original author or authors.
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

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.RegionShortcut;
import com.gemstone.gemfire.cache.Scope;
import com.gemstone.gemfire.cache.server.CacheServer;

/**
 * @author Costin Leau
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Soby Chacko
 *
 * Runs as a standalone Java app.
 * Modified from SGF implementation for testing client/server CQ features
 */
public class CacheServerProcess {

	public static void main(String[] args) throws Exception {

		Properties props = new Properties();
		props.setProperty("name", "CacheServer");
		props.setProperty("log-level", "info");

		System.out.println("\nConnecting to the distributed system and creating the cache.");

		Cache cache = new CacheFactory(props).create();

		// Create region.
		Region<?,?> region = cache.createRegionFactory(RegionShortcut.REPLICATE)
				.setScope(Scope.DISTRIBUTED_ACK)
				.create("test");

		System.out.println("Test region, " + region.getFullPath() + ", created in cache.");

		// Start Cache Server.
		CacheServer server = cache.addCacheServer();
		server.setPort(40404);
		System.out.println("Starting server");
		server.start();
		ForkUtil.createControlFile(CacheServerProcess.class.getName());
		System.out.println("Waiting for shutdown");

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		bufferedReader.readLine();

	}
}
