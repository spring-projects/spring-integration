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

package org.springframework.integration.xml.enricher;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.integration.xml.xpath.XPathEvaluationType;
import org.springframework.util.StringUtils;
import org.springframework.xml.xpath.XPathExpression;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * Transformer implementation which evaluates XPath expressions against the message payload and inserts the
 * result of the evaluation into the messsage header
 *
 * @author Jonas Partner
 */
public class XPathHeaderEnricher implements Transformer {

    private final Map<String, XPathExpression> expressionMap;

    private Map<String, XPathEvaluationType> evaluationTypes;

    private XPathEvaluationType defaultEvaluationType = XPathEvaluationType.STRING_RESULT;

    private volatile boolean skipSettingNullResults = true;

    private volatile XmlPayloadConverter converter = new DefaultXmlPayloadConverter();


    /**
     * Create an instance of XPathHeaderEnricher using a map of the header name to the XPathExpression to evaluate
     * All XpathExpressions are currently evaluated as returning Strings
     *
     * @param expressionMap
     */
    public XPathHeaderEnricher(Map<String, XPathExpression> expressionMap) {
        this.expressionMap = Collections.unmodifiableMap(expressionMap);
    }

    public void setConverter(XmlPayloadConverter converter) {
        this.converter = converter;
    }

    public void setSkipSettingNullResults(boolean skipSettingNullResults) {
        this.skipSettingNullResults = skipSettingNullResults;
    }

    public void setEvaluationTypes(Map<String, XPathEvaluationType> evaluationTypes) {
        this.evaluationTypes = evaluationTypes;
    }

    public void setDefaultEvaluationType(XPathEvaluationType defaultEvaluationType) {
        this.defaultEvaluationType = defaultEvaluationType;
    }

    public Message<?> transform(Message<?> message) {
        MessageBuilder builder = MessageBuilder.fromMessage(message);
        Node node = this.converter.convertToNode(message.getPayload());

        Set<String> keys = this.expressionMap.keySet();
        for (String key : keys) {
            XPathExpression expression = this.expressionMap.get(key);
            XPathEvaluationType evalType = this.defaultEvaluationType;

            if (this.evaluationTypes != null && this.evaluationTypes.containsKey(key)) {
                evalType = this.evaluationTypes.get(key);
            }

            setHeader(node, key, expression, evalType, builder);
        }
        return builder.build();
    }

    protected void setHeader(Node node, String headerName, XPathExpression expression, XPathEvaluationType evaluationType, MessageBuilder builder) {
        Object result = evaluationType.evaluateXPath(expression, node);

        boolean  nullOrEmptyString = (result == null ||
                (result instanceof String && !StringUtils.hasLength((String)result)));

        if (!nullOrEmptyString || !this.skipSettingNullResults) {
            builder.setHeader(headerName, result);
        }
    }

}
