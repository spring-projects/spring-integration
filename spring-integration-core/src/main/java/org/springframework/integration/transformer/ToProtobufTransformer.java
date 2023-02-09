/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.transformer;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.transformer.support.ProtoHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An Protocol Buffer transformer for generated {@link com.google.protobuf.Message} objects.
 * 
 * @author Christian Tzolov
 * @since 6.1
 */
public class ToProtobufTransformer extends AbstractTransformer {

    private Expression typeIdExpression = new FunctionExpression<Message<?>>(
            (message) -> message.getPayload().getClass());

    private EvaluationContext evaluationContext;

    /**
     * Set the expression to evaluate against the message to determine the value for the {@link ProtoHeaders#TYPE}
     * header.
     * @param expression the expression.
     * @return the transformer
     */
    public ToProtobufTransformer typeExpression(Expression expression) {
        assertExpressionNotNull(expression);
        this.typeIdExpression = expression;
        return this;
    }

    /**
     * Set the expression to evaluate against the message to determine the value for the {@link ProtoHeaders#TYPE}
     * header.
     * @param expression the expression.
     * @return the transformer
     */
    public ToProtobufTransformer typeExpression(String expression) {
        assertExpressionNotNull(expression);
        this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
        return this;
    }

    /**
     * Set the expression to evaluate against the message to determine the value for the {@link ProtoHeaders#TYPE}
     * header.
     * @param expression the expression.
     */
    public void setTypeExpression(Expression expression) {
        assertExpressionNotNull(expression);
        this.typeIdExpression = expression;
    }

    /**
     * Set the expression to evaluate against the message to determine the value for the {@link ProtoHeaders#TYPE}
     * header.
     * @param expression the expression.
     */
    public void setTypeExpressionString(String expression) {
        assertExpressionNotNull(expression);
        this.typeIdExpression = EXPRESSION_PARSER.parseExpression(expression);
    }

    private void assertExpressionNotNull(Object expression) {
        Assert.notNull(expression, "'expression' must not be null");
    }

    @Override
    protected void onInit() {
        this.evaluationContext = IntegrationContextUtils.getEvaluationContext(getBeanFactory());
    }

    @Override
    protected Object doTransform(Message<?> message) {
        Assert.state(message.getPayload() instanceof com.google.protobuf.Message,
                "Payload must be an implementation of 'com.google.protobuf.Message'");
        com.google.protobuf.Message protobufMessage = (com.google.protobuf.Message) message.getPayload();

        return getMessageBuilderFactory().withPayload(protobufMessage.toByteArray())
                .copyHeaders(message.getHeaders())
                .setHeader(ProtoHeaders.TYPE, this.typeIdExpression.getValue(this.evaluationContext, message))
                .build();
    }
}
