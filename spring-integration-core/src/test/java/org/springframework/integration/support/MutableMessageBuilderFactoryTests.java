package org.springframework.integration.support;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;


/**
 * @author swilliams
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MutableMessageBuilderFactoryTests {

    @Autowired
    ContextConfiguration.TestGateway gateway;

    @Autowired
    CountDownLatch latch;

    @Test
    public void test() throws InterruptedException {

        gateway.input("hello!");

        boolean result = latch.await(2L, TimeUnit.SECONDS);

        assertTrue("A failure means that that MMBF wasn't used", result);
    }

    @Configuration
    @EnableIntegration
    @IntegrationComponentScan
    static class ContextConfiguration {

        @Bean
        public MutableMessageBuilderFactory messageBuilderFactory() {
            return new MutableMessageBuilderFactory();
        }

        @Bean
        public DirectChannel input() {
            return new DirectChannel();
        }

        @Bean
        public DirectChannel output() {
            return new DirectChannel();
        }

        @Bean
        public CountDownLatch latch() {
            return new CountDownLatch(1);
        }

        @MessagingGateway
        static interface TestGateway {

            @Gateway(requestChannel = "input")
            void input(String payload);

        }

        @MessageEndpoint
        static class TestFilter {

            @Filter(inputChannel = "input", outputChannel = "output")
            public boolean filter(MessageHeaders headers) {
                // headers are immutable, so if this passes without exception,
                // the MutableMessageBuilderFactory *was* used...
                try {
                    headers.put("foo", "bar");
                    return true;
                }
                catch (UnsupportedOperationException e) {
                    return false;
                }
            }
        }

        @MessageEndpoint
        static class Counter {

            @Autowired
            CountDownLatch latch;

            @ServiceActivator(inputChannel = "output")
            public void count(@Payload String message) {
                latch.countDown();
            }
        }
    }

}
