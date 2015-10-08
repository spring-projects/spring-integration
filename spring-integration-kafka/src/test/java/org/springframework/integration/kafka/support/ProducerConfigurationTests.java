/*
 * Copyright 2013-2014 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import kafka.producer.Partitioner;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.integration.kafka.serializer.avro.AvroReflectDatumBackedKafkaEncoder;
import org.springframework.integration.kafka.test.utils.NonSerializableTestKey;
import org.springframework.integration.kafka.test.utils.NonSerializableTestPayload;
import org.springframework.integration.kafka.test.utils.TestKey;
import org.springframework.integration.kafka.test.utils.TestPayload;
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import kafka.serializer.DefaultEncoder;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @since 0.5
 */
public class ProducerConfigurationTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testSendMessageWithNonDefaultKeyAndValueEncoders() throws Exception {
		final ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<String, String>("test", String.class, String.class, new StringSerializer(), new StringSerializer());
		final Producer<String, String> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<String, String> configuration =
				new ProducerConfiguration<String, String>(producerMetadata, producer);

		configuration.send("test", "key", "test message");

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<String, String>> argument =
				(ArgumentCaptor<ProducerRecord<String, String>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<String, String> capturedKeyMessage = argument.getValue();

		Assert.assertEquals(capturedKeyMessage.key(), "key");
		Assert.assertEquals(capturedKeyMessage.value(), "test message");
		Assert.assertEquals(capturedKeyMessage.topic(), "test");
	}

	/**
	 * User does not set an explicit key/value encoder, but send a serializable object for both key/value
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testSendMessageWithDefaultKeyAndValueEncodersAndCustomSerializableKeyAndPayloadObject()
			throws Exception {
		final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test",
				byte[].class,byte[].class, new ByteArraySerializer(), new ByteArraySerializer());
		final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<byte[], byte[]> configuration =
				new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

		configuration.convertAndSend("test", new TestKey("compositePart1", "compositePart2"), new TestPayload("part1", "part2"));

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<byte[], byte[]>> argument =
				(ArgumentCaptor<ProducerRecord<byte[], byte[]>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<byte[], byte[]> capturedKeyMessage = argument.getValue();

		final byte[] keyBytes = capturedKeyMessage.key();

		final ByteArrayInputStream keyInputStream = new ByteArrayInputStream(keyBytes);
		final ObjectInputStream keyObjectInputStream = new ObjectInputStream(keyInputStream);
		final Object keyObj = keyObjectInputStream.readObject();

		final TestKey tk = (TestKey) keyObj;

		Assert.assertEquals(tk.getKeyPart1(), "compositePart1");
		Assert.assertEquals(tk.getKeyPart2(), "compositePart2");

		final byte[] messageBytes = capturedKeyMessage.value();

		final ByteArrayInputStream messageInputStream = new ByteArrayInputStream(messageBytes);
		final ObjectInputStream messageObjectInputStream = new ObjectInputStream(messageInputStream);
		final Object messageObj = messageObjectInputStream.readObject();

		final TestPayload tp = (TestPayload) messageObj;

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
		final AvroReflectDatumBackedKafkaEncoder<TestPayload> encoder =
				new AvroReflectDatumBackedKafkaEncoder<TestPayload>(TestPayload.class);
		final ProducerMetadata<byte[], TestPayload> producerMetadata = new ProducerMetadata<byte[], TestPayload>("test", byte[].class,TestPayload.class, new ByteArraySerializer(), new EncoderAdaptingSerializer<TestPayload>(encoder));

		final Producer<byte[], TestPayload> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<byte[], TestPayload> configuration =
				new ProducerConfiguration<>(producerMetadata, producer);

		TestPayload tp = new TestPayload("part1", "part2");
		configuration.convertAndSend("test", "key", tp);

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<byte[], TestPayload>> argument =
				(ArgumentCaptor<ProducerRecord<byte[], TestPayload>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<byte[], TestPayload> capturedKeyMessage = argument.getValue();

		final byte[] keyBytes = capturedKeyMessage.key();

		final ByteArrayInputStream keyInputStream = new ByteArrayInputStream(keyBytes);
		final ObjectInputStream keyObjectInputStream = new ObjectInputStream(keyInputStream);
		final Object keyObj = keyObjectInputStream.readObject();

		Assert.assertEquals("key", keyObj);
		Assert.assertEquals(capturedKeyMessage.value(), tp);

		Assert.assertEquals(capturedKeyMessage.topic(), "test");
	}

	/**
	 * User does set an explicit key encoder, but not a value encoder, and sends the corresponding data
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testSendMessageWithNonDefaultKeyEncoderAndDefaultValueEncoderAndCorrespondingData() throws Exception {
		final AvroReflectDatumBackedKafkaEncoder<TestKey> encoder = new AvroReflectDatumBackedKafkaEncoder<TestKey>(TestKey.class);
		final ProducerMetadata<TestKey, byte[]> producerMetadata = new ProducerMetadata<TestKey, byte[]>("test", TestKey.class, byte[].class, new EncoderAdaptingSerializer<TestKey>(encoder), new ByteArraySerializer());

		final Producer<TestKey, byte[]> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<TestKey, byte[]> configuration =
				new ProducerConfiguration<TestKey, byte[]>(producerMetadata, producer);

		final TestKey tk = new TestKey("part1", "part2");

		configuration.convertAndSend("test", tk, "test message");

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<TestKey, byte[]>> argument =
				(ArgumentCaptor<ProducerRecord<TestKey, byte[]>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<TestKey, byte[]> capturedKeyMessage = argument.getValue();

		Assert.assertEquals(capturedKeyMessage.key(), tk);

		final byte[] payloadBytes = capturedKeyMessage.value();

		final ByteArrayInputStream payloadBis = new ByteArrayInputStream(payloadBytes);
		final ObjectInputStream payloadOis = new ObjectInputStream(payloadBis);
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
		final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test", byte[].class, byte[].class, new ByteArraySerializer(), new ByteArraySerializer());
		final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<byte[], byte[]> configuration =
				new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

		configuration.convertAndSend("test", "key", "test message");

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<byte[], byte[]>> argument =
				(ArgumentCaptor<ProducerRecord<byte[], byte[]>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<byte[], byte[]> capturedKeyMessage = argument.getValue();
		final byte[] keyBytes = capturedKeyMessage.key();

		final ByteArrayInputStream keyBis = new ByteArrayInputStream(keyBytes);
		final ObjectInputStream keyOis = new ObjectInputStream(keyBis);
		final Object keyObj = keyOis.readObject();

		Assert.assertEquals("key", keyObj);

		final byte[] payloadBytes = capturedKeyMessage.value();

		final ByteArrayInputStream payloadBis = new ByteArrayInputStream(payloadBytes);
		final ObjectInputStream payloadOis = new ObjectInputStream(payloadBis);
		final Object payloadObj = payloadOis.readObject();

		Assert.assertEquals("test message", payloadObj);
		Assert.assertEquals(capturedKeyMessage.topic(), "test");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSendMessageWithNonDefaultKeyAndValueEncodersSpecifyingPartition() throws Exception {
		final ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<String, String>("test", String.class, String.class, new StringSerializer(), new StringSerializer());
		final Producer<String, String> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<String, String> configuration =
				new ProducerConfiguration<String, String>(producerMetadata, producer);

		configuration.send("test", 1, "key", "test message");

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<String, String>> argument =
				(ArgumentCaptor<ProducerRecord<String, String>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<String, String> capturedKeyMessage = argument.getValue();

		Assert.assertEquals(capturedKeyMessage.key(), "key");
		Assert.assertEquals(capturedKeyMessage.value(), "test message");
		Assert.assertEquals(capturedKeyMessage.topic(), "test");
		Assert.assertEquals(capturedKeyMessage.partition().intValue(), 1);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSendMessageWithNonDefaultKeyAndValueEncodersCustomPartitioner() throws Exception {
		Partitioner customPartitioner = Mockito.mock(Partitioner.class);
		final ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<String, String>("test", String.class, String.class, new StringSerializer(), new StringSerializer());
		producerMetadata.setPartitioner(customPartitioner);
		final Producer<String, String> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<String, String> configuration =
				new ProducerConfiguration<String, String>(producerMetadata, producer);

		ArrayList<PartitionInfo> partitionInfos = new ArrayList<>();
		partitionInfos.add(Mockito.mock(PartitionInfo.class));
		Mockito.when(producer.partitionsFor("test")).thenReturn(partitionInfos);
		Mockito.when(customPartitioner.partition("key", 1)).thenReturn(4);

		configuration.send("test", null, "key", "test message");

		Mockito.verify(producer, Mockito.times(1)).send(Mockito.any(ProducerRecord.class));

		final ArgumentCaptor<ProducerRecord<String, String>> argument =
				(ArgumentCaptor<ProducerRecord<String, String>>) (Object)
						ArgumentCaptor.forClass(ProducerRecord.class);
		Mockito.verify(producer).send(argument.capture());

		final ProducerRecord<String, String> capturedKeyMessage = argument.getValue();

		Assert.assertEquals(capturedKeyMessage.key(), "key");
		Assert.assertEquals(capturedKeyMessage.value(), "test message");
		Assert.assertEquals(capturedKeyMessage.topic(), "test");
		Assert.assertEquals(capturedKeyMessage.partition().intValue(), 4);
	}

	/**
	 * User does not set an explicit key/value encoder, but send non-serializable object for both key/value
	 */
	@Test(expected = ConversionFailedException.class)
	@SuppressWarnings("unchecked")
	public void testSendMessageWithDefaultKeyAndValueEncodersButNonSerializableKeyAndValue() throws Exception {
		final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test", byte[].class, byte[].class, new ByteArraySerializer(), new ByteArraySerializer());
		final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<byte[], byte[]> configuration =
				new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

		Message<NonSerializableTestPayload> message =
				new GenericMessage<NonSerializableTestPayload>(new NonSerializableTestPayload("part1", "part2"));

		configuration.convertAndSend("test", new NonSerializableTestKey("compositePart1", "compositePart2"), message);
	}

	/**
	 * User does not set an explicit key/value encoder, but send non-serializable key and serializable value
	 */
	@Test(expected = ConversionFailedException.class)
	@SuppressWarnings("unchecked")
	public void testSendMessageWithDefaultKeyAndValueEncodersButNonSerializableKeyAndSerializableValue()
			throws Exception {
		final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test", byte[].class, byte[].class, new ByteArraySerializer(), new ByteArraySerializer());
		final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<byte[], byte[]> configuration =
				new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

		configuration.convertAndSend("test", new NonSerializableTestKey("compositePart1", "compositePart2"), new TestPayload("part1", "part2"));
	}

	/**
	 * User does not set an explicit key/value encoder, but send serializable key and non-serializable value
	 */
	@Test(expected = ConversionFailedException.class)
	@SuppressWarnings("unchecked")
	public void testSendMessageWithDefaultKeyAndValueEncodersButSerializableKeyAndNonSerializableValue()
			throws Exception {
		final ProducerMetadata<byte[], byte[]> producerMetadata = new ProducerMetadata<byte[], byte[]>("test", byte[].class, byte[].class, new EncoderAdaptingSerializer<byte[]>(new DefaultEncoder(null)), new EncoderAdaptingSerializer<byte[]>(new DefaultEncoder(null)));
		final Producer<byte[], byte[]> producer = Mockito.mock(Producer.class);

		final ProducerConfiguration<byte[], byte[]> configuration =
				new ProducerConfiguration<byte[], byte[]>(producerMetadata, producer);

		configuration.convertAndSend("test", new TestKey("compositePart1", "compositePart2"), new NonSerializableTestPayload("part1", "part2"));
	}

}
