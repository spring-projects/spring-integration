/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import org.junit.rules.ExternalResource;

import org.springframework.lang.NonNull;

/**
 * Base class to set up the external resource NATS server locally before starting the Manual testing
 *
 * <p>Download NATS server zip and set property
 * nats_server_path=PATH_TO_LOCAL_EXTRACTED_DIR/nats-server before running the tests eg.
 * -Dnats_server_path=C:\Software\nats-server-v2.4.0-windows-386\nats-server
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
public class NatsExternalResource extends ExternalResource {

	// Setup Nats server to be up and running
	private NatsLocalTestServer ts;

	private String tlsConfPath = "src/test/resources/tls.conf";

	public NatsExternalResource(@NonNull final String pTlsConfPath) {
		this.tlsConfPath = pTlsConfPath;
	}

	public NatsExternalResource() {
	}

	@Override
	protected void before() throws Throwable {
		this.ts = new NatsLocalTestServer(this.tlsConfPath, 4222, true);
		Thread.sleep(5000);
	}

	@Override
	protected void after() {
		// code to tear down the external resource
		this.ts.close();
	}
}
