/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.ws.config;

import java.net.URI;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @since 1.0.3
 */
import org.springframework.util.Assert;
import org.springframework.ws.client.support.destination.DestinationProvider;


/**
 * A {@link DestinationProvider} implementation that returns a fixed URI.
 * This is used by the outbound gateway parser when no 'destination-provider'
 * reference has been configured.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 * @since 1.0.3
 */
class FixedUriDestinationProvider implements DestinationProvider {

	private final URI uri;


	public FixedUriDestinationProvider(String uri) {
		Assert.hasText(uri, "uri must not be null or empty");
		this.uri = URI.create(uri);
	}


	public URI getDestination() {
		return this.uri;
	}

}
