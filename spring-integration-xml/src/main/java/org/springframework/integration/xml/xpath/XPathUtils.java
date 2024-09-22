/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.xpath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.util.Assert;
import org.springframework.xml.DocumentBuilderFactoryUtils;
import org.springframework.xml.xpath.NodeMapper;
import org.springframework.xml.xpath.XPathException;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Utility class for 'xpath' support.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @since 3.0
 */
public final class XPathUtils {

	public static final String STRING = "string";

	public static final String BOOLEAN = "boolean";

	public static final String NUMBER = "number";

	public static final String NODE = "node";

	public static final String NODE_LIST = "node_list";

	public static final String DOCUMENT_LIST = "document_list";

	private static final List<String> RESULT_TYPES =
			Arrays.asList(STRING, BOOLEAN, NUMBER, NODE, NODE_LIST, DOCUMENT_LIST);

	private static final XmlPayloadConverter CONVERTER = new DefaultXmlPayloadConverter();

	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactoryUtils.newInstance();

	static {
		DOCUMENT_BUILDER_FACTORY.setNamespaceAware(true);
	}

	/**
	 * Utility method to evaluate an xpath on the provided object.
	 * Delegates evaluation to an {@link XPathExpression}.
	 * Note this method provides the {@code #xpath()} SpEL function.
	 * @param object the xml Object for evaluation.
	 * @param xpath an 'xpath' expression String.
	 * @param resultArg an optional parameter to represent the result type of the xpath evaluation.
	 * Only one argument is allowed, which can be an instance of
	 * {@link org.springframework.xml.xpath.NodeMapper} or
	 * one of these String constants: "string", "boolean", "number", "node" or "node_list".
	 * @param <T> The required return type.
	 * @return the result of the xpath expression evaluation.
	 * @throws IllegalArgumentException - if the provided arguments aren't appropriate types or values;
	 * @throws org.springframework.messaging.MessagingException - if the provided
	 * object can't be converted to a {@link Node};
	 * @throws XPathException - if the xpath expression can't be evaluated.
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> T evaluate(Object object, String xpath, Object... resultArg) {
		Object resultType = null;
		if (resultArg != null && resultArg.length > 0) {
			Assert.isTrue(resultArg.length == 1, "'resultArg' can contains only one element.");
			Assert.noNullElements(resultArg, "'resultArg' can't contains 'null' elements.");
			resultType = resultArg[0];
		}

		XPathExpression expression = XPathExpressionFactory.createXPathExpression(xpath);
		Node node = CONVERTER.convertToNode(object);

		if (resultType == null) {
			return (T) expression.evaluateAsString(node);
		}
		else if (resultType instanceof NodeMapper<?>) {
			return (T) expression.evaluateAsObject(node, (NodeMapper<?>) resultType);
		}
		else if (resultType instanceof String resType && RESULT_TYPES.contains(resultType)) {
			if (DOCUMENT_LIST.equals(resType)) {
				List<Node> nodeList = (List<Node>) XPathEvaluationType.NODE_LIST_RESULT.evaluateXPath(expression,
						node);
				try {
					DocumentBuilder documentBuilder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
					List<Node> documents = new ArrayList<>(nodeList.size());
					for (Node n : nodeList) {
						Document document = documentBuilder.newDocument();
						document.appendChild(document.importNode(n, true));
						documents.add(document);
					}
					return (T) documents;
				}
				catch (ParserConfigurationException e) {
					throw new XPathException("Unable to create 'documentBuilder'.", e);
				}
			}
			else {
				XPathEvaluationType evaluationType = XPathEvaluationType.valueOf(resType.toUpperCase() + "_RESULT");
				return (T) evaluationType.evaluateXPath(expression, node);
			}
		}
		else {
			throw new IllegalArgumentException("'resultArg[0]' can be an instance of 'NodeMapper<?>' " +
					"or one of supported String constants: " + RESULT_TYPES);
		}
	}

	private XPathUtils() {
	}

}
