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

import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.integration.core.Message;
import org.springframework.util.Assert;

import java.net.URI;


/**
 * Simple wrapper for Spring WS DestinationProvider instances
 * @author Jonas Partner
 */
public class SpringWsDestinationProviderWrapper implements MessageAwareDestinationProvider {

    private final DestinationProvider destinationProvider;

    public SpringWsDestinationProviderWrapper(DestinationProvider destinationProvider){
        Assert.notNull(destinationProvider, "DestinationProvider can not be null");
        this.destinationProvider = destinationProvider;
    }

    public URI getDestination(Message<?> message) {
        return destinationProvider.getDestination();
    }
}
