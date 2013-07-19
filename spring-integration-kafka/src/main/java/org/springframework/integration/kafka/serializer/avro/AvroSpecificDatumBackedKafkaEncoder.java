package org.springframework.integration.kafka.serializer.avro;

import kafka.serializer.Encoder;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class AvroSpecificDatumBackedKafkaEncoder<T> extends AvroDatumSupport<T> implements Encoder<T> {

	private final DatumWriter<T> writer;

	public AvroSpecificDatumBackedKafkaEncoder(final Class<T> specificRecordClazz) {
		this.writer = new SpecificDatumWriter<T>(specificRecordClazz);
	}

	@Override
	public byte[] toBytes(final T source) {
		return toBytes(source, writer);
	}
}
