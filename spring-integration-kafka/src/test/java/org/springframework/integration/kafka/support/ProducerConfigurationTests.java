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

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.serializer.DefaultEncoder;
import kafka.serializer.StringEncoder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.integration.Message;
import org.springframework.integration.kafka.serializer.avro.AvroBackedKafkaEncoder;
import org.springframework.integration.kafka.test.utils.NonSerializableTestKey;
import org.springframework.integration.kafka.test.utils.NonSerializableTestPayload;
import org.springframework.integration.kafka.test.utils.TestKey;
import org.springframework.integration.kafka.test.utils.TestPayload;
import org.springframework.integration.support.MessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;

/**
 * @author Soby Chacko
 */
public class ProducerConfigurationTests {
    @Test
    @SuppressWarnings("unchecked")
    public void testSendMessageWithNonDefaultKeyAndValueEncoders() throws Exception {
        final ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<String, String>("test");
        producerMetadata.setValueEncoder(new StringEncoder(null));
        producerMetadata.setKeyEncoder(new StringEncoder(null));
        producerMetadata.setKeyClassType(String.class);
        producerMetadata.setValueClassType(String.class);
        final Producer<String, String> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<String, String> configuration = new ProducerConfiguration<String, String>(producerMetadata, producer);

        final Message<String> message = MessageBuilder.withPayload("test message").
                                           setHeader("messageKey", "key")
                                           .setHeader("topic", "test").build();

        configuration.send(message);

        Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(KeyedMessage.class));

        final ArgumentCaptor<KeyedMessage> argument = ArgumentCaptor.forClass(KeyedMessage.class);
        Mockito.verify(producer).send(argument.capture());

        final KeyedMessage capturedKeyMessage = argument.getValue();

        Assert.assertEquals(capturedKeyMessage.key(), "key");
        Assert.assertEquals(capturedKeyMessage.message(), "test message");
        Assert.assertEquals(capturedKeyMessage.topic(), "test");
    }

    /**
     * User does not set an explicit key/value encoder, but send a serializable object for both key/value
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSendMessageWithDefaultKeyAndValueEncodersAndCustomSerializableKeyAndPayloadObject() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        producerMetadata.setValueEncoder(new DefaultEncoder(null));
        producerMetadata.setKeyEncoder(new DefaultEncoder(null));
        final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<byte[], byte[]> configuration = new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

        final Message<TestPayload> message = MessageBuilder.withPayload(new TestPayload("part1", "part2")).
                                           setHeader("messageKey", new TestKey("compositePart1", "compositePart2"))
                                           .setHeader("topic", "test").build();

        configuration.send(message);

        Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(KeyedMessage.class));

        final ArgumentCaptor<KeyedMessage> argument = ArgumentCaptor.forClass(KeyedMessage.class);
        Mockito.verify(producer).send(argument.capture());

        final KeyedMessage capturedKeyMessage = argument.getValue();

        final byte[] keyBytes = (byte[])capturedKeyMessage.key();

        final ByteArrayInputStream keyInputStream = new ByteArrayInputStream (keyBytes);
        final ObjectInputStream keyObjectInputStream = new ObjectInputStream (keyInputStream);
        final Object keyObj = keyObjectInputStream.readObject();

        final TestKey tk = (TestKey)keyObj;

        Assert.assertEquals(tk.getKeyPart1(), "compositePart1");
        Assert.assertEquals(tk.getKeyPart2(), "compositePart2");

        final byte[] messageBytes = (byte[])capturedKeyMessage.message();

        final ByteArrayInputStream messageInputStream = new ByteArrayInputStream (messageBytes);
        final ObjectInputStream messageObjectInputStream = new ObjectInputStream (messageInputStream);
        final Object messageObj = messageObjectInputStream.readObject();

        final TestPayload tp = (TestPayload)messageObj;

        Assert.assertEquals(tp.getPart1(), "part1");
        Assert.assertEquals(tp.getPart2(), "part2");

        Assert.assertEquals(capturedKeyMessage.topic(), "test");
    }

    /**
     * User does not set an explicit key encoder, but a value encoder, and sends the corresponding data
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSendMessageWithDefaultKeyEncoderAndNonDefaultValueEncoderAndCorrespondingData() throws Exception {
        final ProducerMetadata<byte[], TestPayload> producerMetadata = new ProducerMetadata<byte[], TestPayload>("test");
        final AvroBackedKafkaEncoder<TestPayload> encoder = new AvroBackedKafkaEncoder<TestPayload>(TestPayload.class);
        producerMetadata.setValueEncoder(encoder);
        producerMetadata.setKeyEncoder(new DefaultEncoder(null));
        producerMetadata.setValueClassType(TestPayload.class);
        final Producer<byte[], TestPayload> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<byte[], TestPayload> configuration = new ProducerConfiguration<byte[], TestPayload>(producerMetadata, producer);
        final TestPayload tp = new TestPayload("part1", "part2");
        final Message<TestPayload> message = MessageBuilder.withPayload(tp).
                                           setHeader("messageKey", "key")
                                           .setHeader("topic", "test").build();

        configuration.send(message);

        Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(KeyedMessage.class));

        final ArgumentCaptor<KeyedMessage> argument = ArgumentCaptor.forClass(KeyedMessage.class);
        Mockito.verify(producer).send(argument.capture());

        final KeyedMessage capturedKeyMessage = argument.getValue();

        final byte[] keyBytes = (byte[])capturedKeyMessage.key();

        final ByteArrayInputStream keyInputStream = new ByteArrayInputStream (keyBytes);
        final ObjectInputStream keyObjectInputStream = new ObjectInputStream (keyInputStream);
        final Object keyObj = keyObjectInputStream.readObject();

        Assert.assertEquals("key", keyObj);
        Assert.assertEquals(capturedKeyMessage.message(), tp);

        Assert.assertEquals(capturedKeyMessage.topic(), "test");
    }

    /**
     * User does set an explicit key encoder, but not a value encoder, and sends the corresponding data
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSendMessageWithNonDefaultKeyEncoderAndDefaultValueEncoderAndCorrespondingData() throws Exception {
        final ProducerMetadata<TestKey, byte[]> producerMetadata = new ProducerMetadata<TestKey, byte[]>("test");
        final AvroBackedKafkaEncoder<TestKey> encoder = new AvroBackedKafkaEncoder<TestKey>(TestKey.class);
        producerMetadata.setKeyEncoder(encoder);
        producerMetadata.setValueEncoder(new DefaultEncoder(null));
        producerMetadata.setKeyClassType(TestKey.class);
        final Producer<TestKey, byte[]> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<TestKey, byte[]> configuration = new ProducerConfiguration<TestKey, byte[]>(producerMetadata, producer);
        final TestKey tk = new TestKey("part1", "part2");
        final Message<String> message = MessageBuilder.withPayload("test message").
                                           setHeader("messageKey", tk)
                                           .setHeader("topic", "test").build();

        configuration.send(message);

        Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(KeyedMessage.class));

        final ArgumentCaptor<KeyedMessage> argument = ArgumentCaptor.forClass(KeyedMessage.class);
        Mockito.verify(producer).send(argument.capture());

        final KeyedMessage capturedKeyMessage = argument.getValue();

        Assert.assertEquals(capturedKeyMessage.key(), tk);

        final byte[] payloadBytes = (byte[])capturedKeyMessage.message();

        final ByteArrayInputStream payloadBis = new ByteArrayInputStream (payloadBytes);
        final ObjectInputStream payloadOis = new ObjectInputStream (payloadBis);
        final Object payloadObj = payloadOis.readObject();

        Assert.assertEquals("test message", payloadObj);

        Assert.assertEquals(capturedKeyMessage.topic(), "test");
    }

    /**
     * User does not set an explicit key/value encoder, but send a serializable String key/value pair
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSendMessageWithDefaultKeyAndValueEncodersAndStringKeyAndValue() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        producerMetadata.setValueEncoder(new DefaultEncoder(null));
        producerMetadata.setKeyEncoder(new DefaultEncoder(null));
        final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<byte[], byte[]> configuration = new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

        final Message<String> message = MessageBuilder.withPayload("test message").
                                           setHeader("messageKey", "key")
                                           .setHeader("topic", "test").build();

        configuration.send(message);

        Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(KeyedMessage.class));

        final ArgumentCaptor<KeyedMessage> argument = ArgumentCaptor.forClass(KeyedMessage.class);
        Mockito.verify(producer).send(argument.capture());

        final KeyedMessage capturedKeyMessage = argument.getValue();
        final byte[] keyBytes = (byte[])capturedKeyMessage.key();

        final ByteArrayInputStream keyBis = new ByteArrayInputStream (keyBytes);
        final ObjectInputStream keyOis = new ObjectInputStream (keyBis);
        final Object keyObj = keyOis.readObject();

        Assert.assertEquals("key", keyObj);

        final byte[] payloadBytes = (byte[])capturedKeyMessage.message();

        final ByteArrayInputStream payloadBis = new ByteArrayInputStream (payloadBytes);
        final ObjectInputStream payloadOis = new ObjectInputStream (payloadBis);
        final Object payloadObj = payloadOis.readObject();

        Assert.assertEquals("test message", payloadObj);
        Assert.assertEquals(capturedKeyMessage.topic(), "test");
    }

    /**
     * User does not set an explicit key/value encoder, but send non-serializable object for both key/value
     */
    @Test(expected = NotSerializableException.class)
    @SuppressWarnings("unchecked")
    public void testSendMessageWithDefaultKeyAndValueEncodersButNonSerializableKeyAndValue() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        producerMetadata.setValueEncoder(new DefaultEncoder(null));
        producerMetadata.setKeyEncoder(new DefaultEncoder(null));
        final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<byte[], byte[]> configuration = new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

        final Message<NonSerializableTestPayload> message = MessageBuilder.withPayload(new NonSerializableTestPayload("part1", "part2")).
                                               setHeader("messageKey", new NonSerializableTestKey("compositePart1", "compositePart2"))
                                               .setHeader("topic", "test").build();
        configuration.send(message);
    }

    /**
     * User does not set an explicit key/value encoder, but send non-serializable key and serializable value
     */
    @Test(expected = NotSerializableException.class)
    @SuppressWarnings("unchecked")
    public void testSendMessageWithDefaultKeyAndValueEncodersButNonSerializableKeyAndSerializableValue() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        producerMetadata.setValueEncoder(new DefaultEncoder(null));
        producerMetadata.setKeyEncoder(new DefaultEncoder(null));
        final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<byte[], byte[]> configuration = new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

        final Message<TestPayload> message = MessageBuilder.withPayload(new TestPayload("part1", "part2")).
                                               setHeader("messageKey", new NonSerializableTestKey("compositePart1", "compositePart2"))
                                               .setHeader("topic", "test").build();
        configuration.send(message);
    }

    /**
     * User does not set an explicit key/value encoder, but send serializable key and non-serializable value
     */
    @Test(expected = NotSerializableException.class)
    @SuppressWarnings("unchecked")
    public void testSendMessageWithDefaultKeyAndValueEncodersButSerializableKeyAndNonSerializableValue() throws Exception {
        final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test");
        producerMetadata.setValueEncoder(new DefaultEncoder(null));
        producerMetadata.setKeyEncoder(new DefaultEncoder(null));
        final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

        final ProducerConfiguration<byte[], byte[]> configuration = new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

        final Message<NonSerializableTestPayload> message = MessageBuilder.withPayload(new NonSerializableTestPayload("part1", "part2")).
                                               setHeader("messageKey", new TestKey("compositePart1", "compositePart2"))
                                               .setHeader("topic", "test").build();
        configuration.send(message);
    }
}
