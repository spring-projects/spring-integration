/*
 * Copyright 2002-2008 the original author or authors.
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
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.StringUtils;
import org.springframework.util.Assert;

import java.net.URI;


/**
 * Determines URI based
 */
public class HeaderBasedDestinationProvider implements MessageAwareDestinationProvider {

    private final URI defaultUri;

    private final String headerName;

    public HeaderBasedDestinationProvider(URI defaultUri, String headerName) {
        Assert.isTrue(!(defaultUri == null && headerName ==null), "At least one of defaultURI or headerName must be provided");
        this.defaultUri = defaultUri;
        this.headerName = headerName;
    }

    public HeaderBasedDestinationProvider(URI defaultUri) {
        this.defaultUri = defaultUri;
        this.headerName = null;
    }

      public HeaderBasedDestinationProvider( String headerName) {
        this.defaultUri = null;
        this.headerName = headerName;
    }


    public URI getDestination(Message<?> message) {
        URI uri = null;
        if(StringUtils.hasText(headerName)) {
            String headerValue = message.getHeaders().get(headerName, String.class);
            if(StringUtils.hasText(headerValue)){
                uri = URI.create(headerValue);
            }
        }
        if(uri == null){
            uri = defaultUri;
        }

        if(uri == null){
            throw new MessageHandlingException(message,"Could not determine URI for message and no default set");
        }
        return uri;        
    }
}
