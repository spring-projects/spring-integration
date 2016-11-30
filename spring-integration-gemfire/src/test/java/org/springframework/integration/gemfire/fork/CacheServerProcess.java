/*
 * Copyright 2013-2016 the original author or authors.
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
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.server.CacheServer;

/**
 * @author Costin Leau
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Soby Chacko
 * @author Gary Russell
 *
 * Runs as a standalone Java app.
 * Modified from SGF implementation for testing client/server CQ features
 */
public class CacheServerProcess {

	private static final Log logger = LogFactory.getLog(CacheServerProcess.class);

	private CacheServerProcess() {
		super();
	}

	public static void main(String[] args) throws Exception {

		Properties props = new Properties();
		props.setProperty("name", "CacheServer at " + new Date());
		props.setProperty("log-level", "info");

		logger.info("Connecting to the distributed system and creating the cache.");

		Cache cache = new CacheFactory(props).create();

		// Create region.
		Region<?, ?> region = cache.createRegionFactory(RegionShortcut.REPLICATE)
				.setScope(Scope.DISTRIBUTED_ACK)
				.create("test");

		logger.info("Test region, " + region.getFullPath() + ", created in cache.");

		// Start Cache Server.
		CacheServer server = cache.addCacheServer();
		server.setPort(40404);
		logger.info("Starting server");
		server.start();
		ForkUtil.createControlFile(CacheServerProcess.class.getName());
		logger.info("Waiting for shutdown");

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		bufferedReader.readLine();
	}

}
