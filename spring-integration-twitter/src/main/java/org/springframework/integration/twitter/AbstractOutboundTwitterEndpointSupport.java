/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter;

import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;
import org.springframework.util.Assert;
import twitter4j.Twitter;


/**
 * The adapters that support 'sending' / 'updating status' messages will do so on top of this implementation for convenience, only.
 *
 * @author Josh Long
 */
public abstract class AbstractOutboundTwitterEndpointSupport extends AbstractEndpoint implements MessageHandler {
    protected volatile OAuthConfiguration configuration;
    protected volatile Twitter twitter;
    protected volatile StatusUpdateSupport statusUpdateSupport = new StatusUpdateSupport();

    @SuppressWarnings("unused")
    public void setConfiguration(OAuthConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void onInit() throws Exception {
        Assert.notNull(this.configuration, "'configuration' can't be null");
        this.twitter = this.configuration.getTwitter();
        Assert.notNull(this.twitter, "'twitter' can't be null");
    }


    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

}
