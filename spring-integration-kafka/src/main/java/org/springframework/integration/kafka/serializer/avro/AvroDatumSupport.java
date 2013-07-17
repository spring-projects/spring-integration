package org.springframework.integration.kafka.serializer.avro;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public abstract class AvroDatumSupport<T> {

	private static final Log LOG = LogFactory.getLog(AvroDatumSupport.class);

	private final AvroSerializer<T> avroSerializer;

	protected AvroDatumSupport() {
		this.avroSerializer = new AvroSerializer<T>();
	}

	@SuppressWarnings("unchecked")
	public byte[] toBytes(final T source, final DatumWriter<T> writer) {
		try {
			return avroSerializer.serialize(source, writer);
		} catch (IOException e) {
			LOG.error("Failed to encode source: " + e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public T fromBytes(final byte[] bytes, final DatumReader<T> reader) {
		try {
			return avroSerializer.deserialize(bytes, reader);
		} catch (IOException e) {
			LOG.error("Failed to decode byte array: " + e);
		}
		return null;
	}
}
