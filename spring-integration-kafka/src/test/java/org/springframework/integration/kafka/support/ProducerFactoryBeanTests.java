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
package org.springframework.integration.kafka.support;

import junit.framework.Assert;
import kafka.javaapi.producer.Producer;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Soby Chacko
 */
public class ProducerFactoryBeanTests {

    @Test
    public void createProducerWithDefaultMetadata() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        final ProducerMetadata<byte[], byte[]> tm = Mockito.spy(producerMetadata);
        final ProducerFactoryBean<byte[], byte[]> producerFactoryBean = new ProducerFactoryBean<byte[], byte[]>(tm, "localhost:9092");
        final Producer producer = producerFactoryBean.getObject();

        Assert.assertTrue(producer != null);

        Mockito.verify(tm, Mockito.times(1)).getPartitioner();
        Mockito.verify(tm, Mockito.times(1)).getCompressionCodec();
        Mockito.verify(tm, Mockito.times(1)).getValueEncoder();
        Mockito.verify(tm, Mockito.times(1)).getKeyEncoder();
        Mockito.verify(tm, Mockito.times(1)).isAsync();
        Mockito.verify(tm, Mockito.times(0)).getBatchNumMessages();
    }

    @Test
    public void createProducerWithAsyncFeatures() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        producerMetadata.setAsync(true);
        producerMetadata.setBatchNumMessages("300");
        final ProducerMetadata<byte[], byte[]> tm = Mockito.spy(producerMetadata);
        final ProducerFactoryBean<byte[], byte[]> producerFactoryBean = new ProducerFactoryBean<byte[], byte[]>(tm, "localhost:9092");
        final Producer producer = producerFactoryBean.getObject();

        Assert.assertTrue(producer != null);

        Mockito.verify(tm, Mockito.times(1)).getPartitioner();
        Mockito.verify(tm, Mockito.times(1)).getCompressionCodec();
        Mockito.verify(tm, Mockito.times(1)).getValueEncoder();
        Mockito.verify(tm, Mockito.times(1)).getKeyEncoder();
        Mockito.verify(tm, Mockito.times(1)).isAsync();
        Mockito.verify(tm, Mockito.times(2)).getBatchNumMessages();
    }
}
