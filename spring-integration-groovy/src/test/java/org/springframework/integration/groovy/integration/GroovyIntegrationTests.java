/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.groovy.integration;

import groovy.lang.GroovyObject;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.scripting.support.StaticScriptSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author Artem Bilan
 * @since 2.1
 */
public class GroovyIntegrationTests {
    @Test
    public void testGroovyPojoToMapTransformer() throws IOException {
        GroovyScriptFactory groovyScriptFactory = new GroovyScriptFactory(this.getClass().getSimpleName());
        String scriptText = "class TestBean { String foo }; return new TestBean (foo: 'bar')";
        ScriptSource scriptSource = new StaticScriptSource(scriptText, "testScript");
        GroovyObject pojo = (GroovyObject) groovyScriptFactory.getScriptedObject(scriptSource, null);
        Message<?> message = MessageBuilder.withPayload(pojo).build();
        ObjectToMapTransformer transformer = new ObjectToMapTransformer();
        Message<?> transformedMessage = transformer.transform(message);
        Map<String, Object> transformedMap = (Map<String, Object>) transformedMessage.getPayload();
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("foo", "bar");
        assertEquals(testMap, transformedMap);

    }
}
