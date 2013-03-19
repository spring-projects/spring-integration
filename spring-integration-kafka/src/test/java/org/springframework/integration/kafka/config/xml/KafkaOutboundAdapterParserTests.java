/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.config.xml;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaOutboundAdapterParserTests {

    @Autowired
    private ApplicationContext appContext;

    @Test
    public void testOutboundAdapterConfiguration(){
       final PollingConsumer pollingConsumer = appContext.getBean("kafkaOutboundChannelAdapter", PollingConsumer.class);
       final KafkaProducerMessageHandler messageHandler = appContext.getBean(KafkaProducerMessageHandler.class);
       Assert.assertNotNull(pollingConsumer);
       Assert.assertNotNull(messageHandler);
       final KafkaProducerContext producerContext = messageHandler.getKafkaProducerContext();
       Assert.assertNotNull(producerContext);
       Assert.assertEquals(producerContext.getTopicsConfiguration().size(), 2);
    }
}
