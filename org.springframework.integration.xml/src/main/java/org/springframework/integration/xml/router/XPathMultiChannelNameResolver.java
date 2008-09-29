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

import java.util.List;

import org.springframework.integration.message.Message;
import org.springframework.integration.router.AbstractMultiChannelNameResolver;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.util.Assert;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathExpression;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * @author Jonas Partner
 */
public class XPathMultiChannelNameResolver extends AbstractMultiChannelNameResolver {

	private final XPathExpression xPathExpression;

	private volatile XmlPayloadConverter payloadConvertor = new DefaultXmlPayloadConverter();

	private volatile NodeMapper nodeMapper = new TextContentNodeMapper();

	public XPathMultiChannelNameResolver(XPathExpression xPathExpression) {
		Assert.notNull("XPathExpression must not be null");
		this.xPathExpression = xPathExpression;
	}

	public void setNodeMapper(NodeMapper nodeMapper) {
		Assert.notNull(nodeMapper, "NodeMapper must not be null");
		this.nodeMapper = nodeMapper;
	}

	@SuppressWarnings("unchecked")
	public String[] resolveChannelNames(Message<?> message) {
		Node node = payloadConvertor.convertToDocument(message.getPayload());
		List channelNamesList = this.xPathExpression.evaluate(node, this.nodeMapper);
		return (String[]) channelNamesList.toArray(new String[channelNamesList.size()]);
	}

	public void setPayloadConvertor(XmlPayloadConverter payloadConvertor) {
		this.payloadConvertor = payloadConvertor;
	}

	private static class TextContentNodeMapper implements NodeMapper {

		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent();
		}

	}

}
