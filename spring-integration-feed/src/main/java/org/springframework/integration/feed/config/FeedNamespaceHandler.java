package org.springframework.integration.feed.config;
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



import org.springframework.beans.factory.xml.NamespaceHandlerSupport;


/**
 * This is a rather tricky one. I've decided it's best to not get cute about it and to expose *one*
 * <em>inbound-channel-adapter</em>. The adapter will let the user pick which type of updated object they'd like to
 * return. By default it'll return new {@link com.sun.syndication.feed.synd.SyndEntry} objects (which represent
 * individual, new entries in a given feed). One adapter will return updated {@link
 * com.sun.syndication.feed.synd.SyndFeed} objects, or it can return updated {@link
 * com.sun.syndication.feed.synd.SyndEntry} objects.
 *
 * @author Josh Long
 */
public class FeedNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("inbound-channel-adapter", new FeedMessageSourceBeanDefinitionParser());
    }


}