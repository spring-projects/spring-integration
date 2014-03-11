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

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileToStringTransformerParserTests {

    @Autowired
    @Qualifier("transformer")
    EventDrivenConsumer endpoint;

    @Autowired
    MessageBuilderFactory messageBuilderFactory;

    @Test
    public void checkDeleteFilesValue() {
        DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(endpoint);
        MessageTransformingHandler handler = (MessageTransformingHandler)
                endpointAccessor.getPropertyValue("handler");
        DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
        FileToStringTransformer transformer = (FileToStringTransformer)
                handlerAccessor.getPropertyValue("transformer");
        DirectFieldAccessor transformerAccessor = new DirectFieldAccessor(transformer);
        assertEquals(Boolean.TRUE, transformerAccessor.getPropertyValue("deleteFiles"));
        assertSame(this.messageBuilderFactory, transformerAccessor.getPropertyValue("messageBuilderFactory"));
    }

}
