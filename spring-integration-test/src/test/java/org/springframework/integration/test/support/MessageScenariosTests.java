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

package org.springframework.integration.test.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration
public class MessageScenariosTests extends AbstractRequestResponseScenarioTests {

    @Override
    protected List<RequestResponseScenario> defineRequestResponseScenarios() {
        List<RequestResponseScenario> scenarios= new ArrayList<RequestResponseScenario>();
        RequestResponseScenario scenario1 = new RequestResponseScenario(
                "inputChannel","outputChannel")
            .setPayload("hello")
            .setResponseValidator(new PayloadValidator<String>() {
                @Override
                protected void validateResponse(String response) {
                    assertEquals("HELLO",response);
                }
            });

        scenarios.add(scenario1);

        RequestResponseScenario scenario2 = new RequestResponseScenario(
                "inputChannel","outputChannel")
        .setMessage(MessageBuilder.withPayload("hello").setHeader("foo", "bar").build())
        .setResponseValidator(new MessageValidator() {
            @Override
            protected void validateMessage(Message<?> message) {
               assertThat(message,hasPayload("HELLO"));
               assertThat(message,hasHeader("foo","bar"));
            }
        });

        scenarios.add(scenario2);

        RequestResponseScenario scenario3 = new RequestResponseScenario(
                "inputChannel2","outputChannel2")
        .setMessage(MessageBuilder.withPayload("hello").setHeader("foo", "bar").build())
        .setResponseValidator(new MessageValidator() {
            @Override
            protected void validateMessage(Message<?> message) {
                assertThat(message,hasPayload("HELLO"));
                assertThat(message,hasHeader("foo","bar"));
            }
        });

        scenarios.add(scenario3);

        return scenarios;
    }
}
