/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.xml.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Jonas Partner
 * @author David Turanski
 * @author Gary Russell
 * @since 2.0
 */
public class XPathHeaderEnricherTests {

	@Test
	public void simpleStringEvaluation() {
		Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap =
				new HashMap<String, XPathExpressionEvaluatingHeaderValueMessageProcessor>();
		expressionMap.put("one", new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementOne"));
		expressionMap.put("two", new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementTwo"));
		String docAsString = "<root><elementOne>1</elementOne><elementTwo>2</elementTwo></root>";
		XPathHeaderEnricher enricher = new XPathHeaderEnricher(expressionMap);
		Message<?> result = enricher.transform(MessageBuilder.withPayload(docAsString).build());
		MessageHeaders headers = result.getHeaders();
		assertEquals("Wrong value for element one expression", "1", headers.get("one"));
		assertEquals("Wrong value for element two expression", "2", headers.get("two"));
	}

	@Test
	public void convertedEvaluation() {
		Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap =
				new HashMap<String, XPathExpressionEvaluatingHeaderValueMessageProcessor>();
		XPathExpressionEvaluatingHeaderValueMessageProcessor processor = new XPathExpressionEvaluatingHeaderValueMessageProcessor(
				"/root/elementOne");
		processor.setHeaderType(TimeZone.class);
		expressionMap.put("one", processor);
		String docAsString = "<root><elementOne>America/New_York</elementOne></root>";
		XPathHeaderEnricher enricher = new XPathHeaderEnricher(expressionMap);
		Message<?> result = enricher.transform(MessageBuilder.withPayload(docAsString).build());
		MessageHeaders headers = result.getHeaders();
		assertEquals("Wrong value for element one expression", TimeZone.getTimeZone("America/New_York"), headers.get("one"));
	}

	@Test
	public void nullValuesSkippedByDefault() {
		Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap
				= new HashMap<String, XPathExpressionEvaluatingHeaderValueMessageProcessor>();
		expressionMap.put("two", new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementTwo"));
		String docAsString = "<root><elementOne>1</elementOne></root>";
		XPathHeaderEnricher enricher = new XPathHeaderEnricher(expressionMap);
		Message<?> result = enricher.transform(MessageBuilder.withPayload(docAsString).build());
		MessageHeaders headers = result.getHeaders();
		assertNull("value set for two when result was null", headers.get("two"));
	}

	@Test
	public void notSkippingNullValues() {
		Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap =
				new HashMap<String, XPathExpressionEvaluatingHeaderValueMessageProcessor>();
		expressionMap.put("two", new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementTwo"));
		String docAsString = "<root><elementOne>1</elementOne></root>";
		XPathHeaderEnricher enricher = new XPathHeaderEnricher(expressionMap);
		enricher.setShouldSkipNulls(false);
		enricher.setDefaultOverwrite(true);
		Message<?> result = enricher.transform(MessageBuilder.withPayload(docAsString).setHeader("two", "x").build());
		MessageHeaders headers = result.getHeaders();
		assertNull(headers.get("two"));
		assertFalse(headers.containsKey("two"));
	}

	@Test
	public void numberEvaluationResult() {
		Map<String, XPathExpressionEvaluatingHeaderValueMessageProcessor> expressionMap =
				new HashMap<String, XPathExpressionEvaluatingHeaderValueMessageProcessor>();
		XPathExpressionEvaluatingHeaderValueMessageProcessor expression1 =
				new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementOne");
		XPathExpressionEvaluatingHeaderValueMessageProcessor expression2 =
				new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementTwo");
		expression2.setEvaluationType(XPathEvaluationType.NUMBER_RESULT);
		expressionMap.put("one", expression1);
		expressionMap.put("two", expression2);
		Map<String, XPathEvaluationType> evalTypeMap = new HashMap<String, XPathEvaluationType>();
		evalTypeMap.put("two", XPathEvaluationType.NUMBER_RESULT);
		String docAsString = "<root><elementOne>1</elementOne><elementTwo>2</elementTwo></root>";
		XPathHeaderEnricher enricher = new XPathHeaderEnricher(expressionMap);
		Message<?> result = enricher.transform(MessageBuilder.withPayload(docAsString).build());
		MessageHeaders headers = result.getHeaders();
		assertEquals("Wrong value for element one expression", "1", headers.get("one"));
		assertEquals("Wrong value for element two expression", 2.0, headers.get("two"));
	}

}
