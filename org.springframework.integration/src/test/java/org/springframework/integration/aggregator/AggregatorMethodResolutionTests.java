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

package org.springframework.integration.aggregator;

import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.MessageBuilder;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mark Fisher
 */
/*
 * TODO This class needs to be removed with INT-1017. We need to ensure that the tests herein are superseded by
 * MethodInvokingMessageGroupProcessorTests before deleting it entirely.
 */
public class AggregatorMethodResolutionTests {

    @Test
    public void singleAnnotation() throws Exception {
        SingleAnnotationTestBean bean = new SingleAnnotationTestBean();
        MethodInvokingAggregator aggregator = new MethodInvokingAggregator(bean);
        Method method = this.getMethod(aggregator);
        Method expected = SingleAnnotationTestBean.class.getMethod("method1", new Class[]{List.class});
        assertEquals(expected, method);
    }

    @Test(expected = IllegalArgumentException.class)
    public void multipleAnnotations() {
        MultipleAnnotationTestBean bean = new MultipleAnnotationTestBean();
        new MethodInvokingAggregator(bean);
    }

    @Test
    public void noAnnotations() throws Exception {
        NoAnnotationTestBean bean = new NoAnnotationTestBean();
        MethodInvokingAggregator aggregator = new MethodInvokingAggregator(bean);
        Method method = this.getMethod(aggregator);
        Method expected = NoAnnotationTestBean.class.getMethod("method1", new Class[]{List.class});
        assertEquals(expected, method);
    }

    @Test(expected = IllegalArgumentException.class)
    public void multiplePublicMethods() {
        MultiplePublicMethodTestBean bean = new MultiplePublicMethodTestBean();
        new MethodInvokingAggregator(bean);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noPublicMethods() {
        NoPublicMethodTestBean bean = new NoPublicMethodTestBean();
        new MethodInvokingAggregator(bean);
    }

    @Test
    public void jdkProxy() {
        DirectChannel input = new DirectChannel();
        QueueChannel output = new QueueChannel();
        GreetingService testBean = new GreetingBean();
        ProxyFactory proxyFactory = new ProxyFactory(testBean);
        proxyFactory.setProxyTargetClass(false);
        testBean = (GreetingService) proxyFactory.getProxy();
        MethodInvokingAggregator aggregator = new MethodInvokingAggregator(testBean);
        aggregator.setAutoStartup(false);
        aggregator.setOutputChannel(output);
        EventDrivenConsumer endpoint = new EventDrivenConsumer(input, aggregator);
        endpoint.start();
        Message<?> message = MessageBuilder.withPayload("proxy")
                .setCorrelationId("abc")
                .build();
        input.send(message);
        assertEquals("hello proxy", output.receive(0).getPayload());
    }

    @Test
    public void cglibProxy() {
        DirectChannel input = new DirectChannel();
        QueueChannel output = new QueueChannel();
        GreetingService testBean = new GreetingBean();
        ProxyFactory proxyFactory = new ProxyFactory(testBean);
        proxyFactory.setProxyTargetClass(true);
        testBean = (GreetingService) proxyFactory.getProxy();
        MethodInvokingAggregator aggregator = new MethodInvokingAggregator(testBean);
        aggregator.setAutoStartup(false);
        aggregator.setOutputChannel(output);
        EventDrivenConsumer endpoint = new EventDrivenConsumer(input, aggregator);
        endpoint.start();
        Message<?> message = MessageBuilder.withPayload("proxy")
                .setCorrelationId("abc")
                .build();
        input.send(message);
        assertEquals("hello proxy", output.receive(0).getPayload());
    }


    private Method getMethod(MethodInvokingAggregator aggregator) {
        Object invoker = new DirectFieldAccessor(aggregator).getPropertyValue("methodInvoker");
        return (Method) new DirectFieldAccessor(invoker).getPropertyValue("method");
    }


    private static class SingleAnnotationTestBean {

        @Aggregator
        public String method1(List<String> input) {
            return input.get(0);
        }

        public String method2(List<String> input) {
            return input.get(0);
        }
    }


    private static class MultipleAnnotationTestBean {

        @Aggregator
        public String method1(List<String> input) {
            return input.get(0);
        }

        @Aggregator
        public String method2(List<String> input) {
            return input.get(0);
        }
    }


    private static class NoAnnotationTestBean {

        public String method1(List<String> input) {
            return input.get(0);
        }

        String method2(List<String> input) {
            return input.get(0);
        }
    }


    private static class MultiplePublicMethodTestBean {

        public String upperCase(String s) {
            return s.toUpperCase();
        }

        public String lowerCase(String s) {
            return s.toLowerCase();
        }
    }


    private static class NoPublicMethodTestBean {

        String lowerCase(String s) {
            return s.toLowerCase();
        }
    }


    public interface GreetingService {

        String sayHello(List<String> names);

    }


    public static class GreetingBean implements GreetingService {

        private String greeting = "hello";

        public void setGreeting(String greeting) {
            this.greeting = greeting;
        }

        @Aggregator
        public String sayHello(List<String> names) {
            return greeting + " " + names.get(0);
        }

    }

}
