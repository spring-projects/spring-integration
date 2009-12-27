package org.springframework.integration.xml.xpath;

import org.springframework.xml.xpath.XPathExpression;
import org.w3c.dom.Node;


/**
 * Enumeration of different types o XPath evaluation used to indicate the type of evaluation that should be carried out
 * using a provided XPath expression
 */
public enum XPathEvaluationType {

    BOOLEAN_RESULT {public Object evaluateXPath(XPathExpression expression, Node node) {
        return expression.evaluateAsBoolean(node);
    }},

    STRING_RESULT {public Object evaluateXPath(XPathExpression expression, Node node) {
        return expression.evaluateAsString(node);
    }},
    NUMBER_RESULT {public Object evaluateXPath(XPathExpression expression, Node node) {
        return expression.evaluateAsNumber(node);
    }},

    NODE_RESULT {public Object evaluateXPath(XPathExpression expression, Node node) {
        return expression.evaluateAsNode(node);
    }},
    NODE_LIST_RESULT {public Object evaluateXPath(XPathExpression expression, Node node) {
        return expression.evaluateAsNodeList(node);
    }};

    public abstract Object evaluateXPath(XPathExpression expression, Node node);

}
