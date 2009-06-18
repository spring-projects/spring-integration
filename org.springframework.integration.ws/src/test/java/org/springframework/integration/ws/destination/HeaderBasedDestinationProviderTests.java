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

import static org.junit.Assert.*;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;


/**
 * @author Jonas Partner
 */
public class HeaderBasedDestinationProviderTests {

    Message<String> messageNoHeader;

    Message<String> messageWithHeaderSet;

    URI uriInHeader = URI.create("uriInHeader");

    URI defaultURI = URI.create("testDefaultUri");

    String testHeaderName = "testUriHeaderName";

    @Before
    public void setUp() {
        messageNoHeader = MessageBuilder.withPayload("testPayload").build();
        messageWithHeaderSet = MessageBuilder.withPayload("otherTestPayload").setHeader(testHeaderName, uriInHeader.toString()).build();
    }

    @Test
    public void testDefaultUriNoHeaderNameSet() {

        HeaderBasedDestinationProvider provider = new HeaderBasedDestinationProvider(defaultURI, null);
        URI resolvedURI = provider.getDestination(messageNoHeader);
        assertEquals("Wrong URI", defaultURI, resolvedURI);
    }

    @Test
    public void testDefaultUriAndHeaderNameSetAndPresent() {
        HeaderBasedDestinationProvider provider = new HeaderBasedDestinationProvider(defaultURI, testHeaderName);
        URI resolvedURI = provider.getDestination(messageWithHeaderSet);
        assertEquals("Wrong URI", uriInHeader, resolvedURI);
    }

    @Test
    public void testDefaultUriAndHeaderNameSetAndNotPresent() {
        HeaderBasedDestinationProvider provider = new HeaderBasedDestinationProvider(defaultURI, testHeaderName);
        URI resolvedURI = provider.getDestination(messageNoHeader);
        assertEquals("Wrong URI", defaultURI, resolvedURI);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testNoDefaultUriAndNoHeaderName() {
        HeaderBasedDestinationProvider provider = new HeaderBasedDestinationProvider(null, null);
        URI resolvedURI = provider.getDestination(messageNoHeader);
        assertEquals("Wrong URI", defaultURI, resolvedURI);
    }


    @Test(expected = MessageHandlingException.class)
    public void testNoDefaultUriAndHeaderNameSetButNotPresent() {
        HeaderBasedDestinationProvider provider = new HeaderBasedDestinationProvider(null, testHeaderName);
        URI resolvedURI = provider.getDestination(messageNoHeader);
        assertEquals("Wrong URI", defaultURI, resolvedURI);
    }

}

