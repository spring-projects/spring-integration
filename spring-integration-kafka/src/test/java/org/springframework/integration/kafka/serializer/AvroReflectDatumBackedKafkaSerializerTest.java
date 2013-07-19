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
package org.springframework.integration.kafka.serializer;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.kafka.serializer.avro.AvroReflectDatumBackedKafkaDecoder;
import org.springframework.integration.kafka.serializer.avro.AvroReflectDatumBackedKafkaEncoder;
import org.springframework.integration.kafka.test.utils.TestObject;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class AvroReflectDatumBackedKafkaSerializerTest {

	@Test
	public void testDecodePlainSchema() {
		final AvroReflectDatumBackedKafkaEncoder<TestObject> avroBackedKafkaEncoder = new AvroReflectDatumBackedKafkaEncoder<TestObject>(TestObject.class);

		final TestObject testObject = new TestObject();
		testObject.setTestData1("\"Test Data1\"");
		testObject.setTestData2(1);

		final byte[] data = avroBackedKafkaEncoder.toBytes(testObject);

		final AvroReflectDatumBackedKafkaDecoder<TestObject> avroReflectDatumBackedKafkaDecoder = new AvroReflectDatumBackedKafkaDecoder<TestObject>(TestObject.class);
		final TestObject decodedFbu = avroReflectDatumBackedKafkaDecoder.fromBytes(data);

		Assert.assertEquals(testObject.getTestData1(), decodedFbu.getTestData1());
		Assert.assertEquals(testObject.getTestData2(), decodedFbu.getTestData2());
	}

	@Test
	public void anotherTest() {
		final AvroReflectDatumBackedKafkaEncoder<String> avroBackedKafkaEncoder = new AvroReflectDatumBackedKafkaEncoder<String>(java.lang.String.class);
		final String testString = "Testing Avro";
		final byte[] data = avroBackedKafkaEncoder.toBytes(testString);

		final AvroReflectDatumBackedKafkaDecoder<String> avroReflectDatumBackedKafkaDecoder = new AvroReflectDatumBackedKafkaDecoder<String>(java.lang.String.class);
		final String decodedS = avroReflectDatumBackedKafkaDecoder.fromBytes(data);

		Assert.assertEquals(testString, decodedS);
	}
}
