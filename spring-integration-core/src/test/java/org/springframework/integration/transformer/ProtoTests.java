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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.test.condition.LogLevels;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.proto.TestClass1;
import org.springframework.integration.transformer.proto.TestClass2;
import org.springframework.integration.transformer.support.ProtoHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 *
 * @author Christian Tzolov
 * @since 6.1
 */
@SpringJUnitConfig
@LogLevels(categories = "foo", level = "DEBUG")
public class ProtoTests {

        @Test
        @LogLevels(classes = DirectChannel.class, categories = "bar", level = "DEBUG")
        void testTransformers(@Autowired ProtoConfig config) {
                TestClass1 test = TestClass1.newBuilder()
                                .setBar("foo")
                                .setQux(678)
                                .build();
                LogAccessor spied = spy(TestUtils.getPropertyValue(config.in1(), "logger", LogAccessor.class));
                new DirectFieldAccessor(config.in1()).setPropertyValue("logger", spied);
                config.in1().send(new GenericMessage<>(test));
                assertThat(config.tapped().receive(0))
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isInstanceOf(byte[].class);
                Message<?> received = config.out().receive(0);
                assertThat(received)
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isEqualTo(test)
                                .isNotSameAs(test);
                assertThat(received.getHeaders().get("flow")).isEqualTo("flow1");
                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                verify(spied, atLeastOnce()).debug(captor.capture());
                assertThat(captor.getAllValues()).anyMatch(s -> s.contains("preSend on channel"));
                assertThat(captor.getAllValues()).anyMatch(s -> s.contains("postSend (sent=true) on channel"));
        }

        @Test
        @LogLevels(classes = DirectChannel.class, categories = "bar", level = "DEBUG")
        void testTransformersJson(@Autowired ProtoConfig config) {
                TestClass1 test = TestClass1.newBuilder()
                                .setBar("foo")
                                .setQux(678)
                                .build();
                LogAccessor spied = spy(TestUtils.getPropertyValue(config.in1(), "logger", LogAccessor.class));
                new DirectFieldAccessor(config.in1()).setPropertyValue("logger", spied);

                config.in1().send(new GenericMessage<>(test,
                                Collections.singletonMap(MessageHeaders.CONTENT_TYPE, "application/json")));
                assertThat(config.tapped().receive(0))
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isInstanceOf(String.class)
                                .isEqualTo("{\n  \"bar\": \"foo\",\n  \"qux\": 678\n}");
                Message<?> received = config.out().receive(0);
                assertThat(received)
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isEqualTo(test)
                                .isNotSameAs(test);
                assertThat(received.getHeaders().get("flow")).isEqualTo("flow1");
                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                verify(spied, atLeastOnce()).debug(captor.capture());
                assertThat(captor.getAllValues()).anyMatch(s -> s.contains("preSend on channel"));
                assertThat(captor.getAllValues()).anyMatch(s -> s.contains("postSend (sent=true) on channel"));
        }

        @Test
        void testMultiTypeTransformers(@Autowired ProtoConfig config) {
                TestClass1 test = TestClass1.newBuilder()
                                .setBar("foo")
                                .setQux(678)
                                .build();
                config.in2().send(new GenericMessage<>(test));
                assertThat(config.tapped().receive(0))
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isInstanceOf(byte[].class);
                Message<?> received = config.out().receive(0);
                assertThat(received)
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isNotEqualTo(test)
                                .isInstanceOf(TestClass2.class);
                assertThat(received.getHeaders().get("flow")).isEqualTo("flow2");
        }

        @Test
        void testMultiTypeTransformersClassName(@Autowired ProtoConfig config) {
                TestClass1 test = TestClass1.newBuilder()
                                .setBar("foo")
                                .setQux(678)
                                .build();
                config.in3().send(new GenericMessage<>(test));
                assertThat(config.tapped().receive(0))
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isInstanceOf(byte[].class);
                Message<?> received = config.out().receive(0);
                assertThat(received)
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isNotEqualTo(test)
                                .isInstanceOf(TestClass2.class);
                assertThat(received.getHeaders().get("flow")).isEqualTo("flow3");
        }

        @Test
        void testTransformersNoHeaderPresent(@Autowired ProtoConfig config) {
                TestClass1 test = TestClass1.newBuilder()
                                .setBar("foo")
                                .setQux(678)
                                .build();
                config.in4().send(new GenericMessage<>(test));
                assertThat(config.tapped().receive(0))
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isInstanceOf(byte[].class);
                Message<?> received = config.out().receive(0);
                assertThat(received)
                                .isNotNull()
                                .extracting(msg -> msg.getPayload())
                                .isEqualTo(test)
                                .isNotSameAs(test);
                assertThat(received.getHeaders().get("flow")).isEqualTo("flow4");
        }

        @Configuration
        @EnableIntegration
        public static class ProtoConfig {

                @Bean
                public IntegrationFlow flow1() {
                        return IntegrationFlow.from(in1())
                                        .transform(new ToProtobufTransformer())
                                        .wireTap(tapped())
                                        .transform(fromTransformer())
                                        .enrichHeaders(h -> h.header("flow", "flow1"))
                                        .channel(out())
                                        .get();
                }

                @Bean
                public IntegrationFlow flow2() {
                        return IntegrationFlow.from(in2())
                                        .transform(new ToProtobufTransformer())
                                        .wireTap(tapped())
                                        .enrichHeaders(h -> h.header(ProtoHeaders.TYPE, TestClass2.class, true))
                                        .transform(fromTransformer())
                                        .enrichHeaders(h -> h.header("flow", "flow2"))
                                        .channel(out())
                                        .get();
                }

                @Bean
                public IntegrationFlow flow3() {
                        return IntegrationFlow.from(in3())
                                        .transform(new ToProtobufTransformer())
                                        .wireTap(tapped())
                                        .enrichHeaders(h -> h.header(ProtoHeaders.TYPE, TestClass2.class.getName(),
                                                        true))
                                        .transform(fromTransformer())
                                        .enrichHeaders(h -> h.header("flow", "flow3"))
                                        .channel(out())
                                        .get();
                }

                @Bean
                public IntegrationFlow flow4() {
                        return IntegrationFlow.from(in4())
                                        .transform(new ToProtobufTransformer())
                                        .wireTap(tapped())
                                        .enrichHeaders(h -> h.header(ProtoHeaders.TYPE, null, true)
                                                        .shouldSkipNulls(false))
                                        .transform(fromTransformer())
                                        .enrichHeaders(h -> h.header("flow", "flow4"))
                                        .channel(out())
                                        .get();
                }

                @Bean
                public FromProtobufTransformer fromTransformer() {
                        return new FromProtobufTransformer(TestClass1.class);
                }

                @Bean
                public DirectChannel in1() {
                        return new DirectChannel();
                }

                @Bean
                public DirectChannel in2() {
                        return new DirectChannel();
                }

                @Bean
                public DirectChannel in3() {
                        return new DirectChannel();
                }

                @Bean
                public DirectChannel in4() {
                        return new DirectChannel();
                }

                @Bean
                public PollableChannel tapped() {
                        return new QueueChannel();
                }

                @Bean
                public PollableChannel out() {
                        return new QueueChannel();
                }

        }
}
