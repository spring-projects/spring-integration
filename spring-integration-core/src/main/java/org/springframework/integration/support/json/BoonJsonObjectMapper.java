package org.springframework.integration.support.json;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.boon.json.JsonFactory;
import org.boon.json.JsonParserAndMapper;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.JsonSlurper;
import org.boon.json.ObjectMapper;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.util.ClassUtils;

/**
 * The Boon (@link https://github.com/RichardHightower/boon) {@link JsonObjectMapper} implementation.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class BoonJsonObjectMapper extends JsonObjectMapperAdapter<Map<String, Object>, Object>
		implements BeanClassLoaderAware {

	private static final Log logger = LogFactory.getLog(BoonJsonObjectMapper.class);

	private static final Collection<Class<?>> supportedJsonTypes =
			Arrays.<Class<?>>asList(String.class, byte[].class, byte[].class, File.class, InputStream.class, Reader.class);


	private final ObjectMapper objectMapper;

	private final JsonSlurper slurper = new JsonSlurper();

	private volatile ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

	public BoonJsonObjectMapper() {
		this.objectMapper = JsonFactory.create();
	}

	public BoonJsonObjectMapper(JsonParserFactory parserFactory, JsonSerializerFactory serializerFactory) {
		this.objectMapper = JsonFactory.create(parserFactory, serializerFactory);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public String toJson(Object value) throws Exception {
		return this.objectMapper.writeValueAsString(value);
	}

	@Override
	public void toJson(Object value, Writer writer) {
		this.objectMapper.toJson(value, writer);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> toJsonNode(final Object value) throws Exception {
		PipedReader in = new PipedReader();
		final PipedWriter out = new PipedWriter(in);
		Executors.newSingleThreadExecutor()
				.execute(new Runnable() {
					@Override
					public void run() {
						toJson(value, out);
					}
				});
		return (Map<String, Object>) this.slurper.parse(in);
	}

	@Override
	public <T> T fromJson(Object json, Class<T> type) throws Exception {
		if (json instanceof String) {
			return this.objectMapper.readValue((String) json, type);
		}
		else if (json instanceof byte[]) {
			return this.objectMapper.readValue((byte[]) json, type);
		}
		else if (json instanceof char[]) {
			return this.objectMapper.readValue((char[]) json, type);
		}
		else if (json instanceof File) {
			return this.objectMapper.readValue((File) json, type);
		}
		else if (json instanceof InputStream) {
			return this.objectMapper.readValue((InputStream) json, type);
		}
		else if (json instanceof Reader) {
			return this.objectMapper.readValue((Reader) json, type);
		}
		else {
			throw new IllegalArgumentException("'json' argument must be an instance of: " + supportedJsonTypes
					+ " , but gotten: " + json.getClass());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T fromJson(Object json, Map<String, Object> javaTypes) throws Exception {
		JsonParserAndMapper parser = this.objectMapper.parser();

		Class<?> classType = this.createJavaType(javaTypes, JsonHeaders.TYPE_ID);

		Class<?> contentClassType = this.createJavaType(javaTypes, JsonHeaders.CONTENT_TYPE_ID);

		Class<?> keyClassType = this.createJavaType(javaTypes, JsonHeaders.KEY_TYPE_ID);

		if (keyClassType != null) {
			logger.warn("Boon doesn't support the Map 'key' conversion. Will be returned raw Map<String, Object>");
			if (json instanceof String) {
				return (T) parser.parseMap((String) json);
			}
			else if (json instanceof byte[]) {
				return (T) parser.parseMap((byte[]) json);
			}
			else if (json instanceof char[]) {
				return (T) parser.parseMap((char[]) json);
			}
			else if (json instanceof File) {
				return (T) parser.parseMap(new FileReader((File) json));
			}
			else if (json instanceof InputStream) {
				return (T) parser.parseMap((InputStream) json);
			}
			else if (json instanceof Reader) {
				return (T) parser.parseMap((Reader) json);
			}
			else {
				throw new IllegalArgumentException("'json' argument must be an instance of: " + supportedJsonTypes
						+ " , but gotten: " + json.getClass());
			}
		}

		if (contentClassType != null) {
			if (json instanceof String) {
				return (T) this.objectMapper.readValue((String) json, (Class<Collection>) classType, contentClassType);
			}
			else if (json instanceof byte[]) {
				return (T) this.objectMapper.readValue((byte[]) json, (Class<Collection>) classType, contentClassType);
			}
			else if (json instanceof char[]) {
				return (T) this.objectMapper.readValue((char[]) json, (Class<Collection>) classType, contentClassType);
			}
			else if (json instanceof File) {
				return (T) this.objectMapper.readValue((File) json, (Class<Collection>) classType, contentClassType);
			}
			else if (json instanceof InputStream) {
				return (T) this.objectMapper.readValue((InputStream) json, (Class<Collection>) classType,
						contentClassType);
			}
			else if (json instanceof Reader) {
				return (T) this.objectMapper.readValue((Reader) json, (Class<Collection>) classType, contentClassType);
			}
			else {
				throw new IllegalArgumentException("'json' argument must be an instance of: " + supportedJsonTypes
						+ " , but gotten: " + json.getClass());
			}
		}

		return (T) fromJson(json, classType);
	}

	protected Class<?> createJavaType(Map<String, Object> javaTypes, String javaTypeKey) throws Exception {
		Object classValue = javaTypes.get(javaTypeKey);
		if (classValue instanceof Class<?>) {
			return (Class<?>) classValue;
		}
		else if (classValue != null) {
			return ClassUtils.forName(classValue.toString(), this.classLoader);
		}
		else {
			return null;
		}
	}

	@Override
	public <T> T fromJson(Object parser, Type valueType) throws Exception {
		throw new UnsupportedOperationException("Boon doesn't support JSON reader parser abstraction");
	}

}
