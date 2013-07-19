package org.springframework.integration.kafka.serializer.avro;

import kafka.serializer.Decoder;

import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class AvroSpecificDatumBackedKafkaDecoder<T> extends AvroDatumSupport<T> implements Decoder<T> {

	private final DatumReader<T> reader;

	public AvroSpecificDatumBackedKafkaDecoder(final Class<T> specificRecordBase) {
		this.reader = new SpecificDatumReader<T>(specificRecordBase);
	}

	@Override
	public T fromBytes(final byte[] bytes) {
		return fromBytes(bytes, reader);
	}
}
