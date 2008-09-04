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

package org.springframework.integration.xml.router;

import org.w3c.dom.Node;

import org.springframework.integration.message.Message;
import org.springframework.integration.router.AbstractSingleChannelNameResolver;
import org.springframework.integration.xml.util.XPathUtils;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.XPathExpression;

/**
 * @author Jonas Partner
 */
public class XPathSingleChannelNameResolver extends AbstractSingleChannelNameResolver {

	private final XPathExpression xPathExpression;


	public XPathSingleChannelNameResolver(XPathExpression xPathExpression) {
		Assert.notNull("XPathExpression must be provided");
		this.xPathExpression = xPathExpression;
	}

	public String resolveChannelName(Message<?> message) {
		Node node = XPathUtils.extractPayloadAsNode(message);
		return xPathExpression.evaluateAsString(node);
	}

}
