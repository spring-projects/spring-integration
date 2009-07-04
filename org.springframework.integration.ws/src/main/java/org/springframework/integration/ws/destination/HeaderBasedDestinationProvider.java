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

package org.springframework.integration.ws.destination;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;

import java.net.URI;

/**
 * Determines a Web Service destination based on a URI provided in a header
 * value. The header value's type may be either a {@link URI} or a String.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 * @since 1.0.3
 */
public class HeaderBasedDestinationProvider implements MessageAwareDestinationProvider {

	private final URI defaultUri;

	private final String headerName;


	public HeaderBasedDestinationProvider(URI defaultUri, String headerName) {
		Assert.isTrue(!(defaultUri == null && headerName == null),
				"At least one of headerName or defaultUri must be provided");
		this.defaultUri = defaultUri;
		this.headerName = headerName;
	}

	public HeaderBasedDestinationProvider(URI defaultUri) {
		this(defaultUri, null);
	}

	public HeaderBasedDestinationProvider(String headerName) {
		this(null, headerName);
	}


	public URI getDestination(Message<?> message) {
		URI uri = null;
		if (StringUtils.hasText(this.headerName)) {
			Object headerValue = message.getHeaders().get(this.headerName);
			if (headerValue instanceof URI) {
				uri = (URI) headerValue;
			}
			else if (headerValue instanceof String && StringUtils.hasText((String) headerValue)) {
				uri = URI.create((String) headerValue);
			}
			else if (headerValue != null) {
				throw new MessageHandlingException(
						message,
						"Invalid type for Web Service destination URI header '"
								+ this.headerName
								+ "', expected java.net.URI or java.lang.String, but received ["
								+ headerValue.getClass() + "].");
			}
		}
		if (uri == null) {
			uri = defaultUri;
		}
		if (uri == null) {
			throw new MessageHandlingException(message,
					"Unable to determine URI for message and no default has been set.");
		}
		return uri;
	}

}
