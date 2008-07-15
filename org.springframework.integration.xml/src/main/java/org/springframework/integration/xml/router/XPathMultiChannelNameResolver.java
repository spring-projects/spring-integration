/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.List;

import org.springframework.integration.message.Message;
import org.springframework.integration.router.MultiChannelNameResolver;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

public class XPathMultiChannelNameResolver extends AbstractXPathChannelNameResolver implements MultiChannelNameResolver {

	private final XPathExpression xPathExpression;
	
	private NodeMapper nodeMapper = new TextContentNodeMapper();
	
	public XPathMultiChannelNameResolver(XPathExpression xPathExpression){
		Assert.notNull("XPAthExpression must be provided");
		this.xPathExpression = xPathExpression;
	}
	
	public void setNodeMapper(NodeMapper nodeMapper){
		Assert.notNull(nodeMapper,"NodeMapper can not be null");
		this.nodeMapper = nodeMapper;
	}
	
	@SuppressWarnings("unchecked")
	public String[] resolve(Message<?> message) {
		Node node =extractNode(message);
		List channelNamesList = xPathExpression.evaluate(node, nodeMapper);
		return (String[])channelNamesList.toArray(new String[channelNamesList.size()]);
	}
	
	private static class TextContentNodeMapper implements NodeMapper{

		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent();
		}
		
	}
	

}
