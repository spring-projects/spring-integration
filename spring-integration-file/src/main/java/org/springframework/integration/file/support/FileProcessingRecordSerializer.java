/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.file.support;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serializer for serialization/deserialization of FileProcessingRecord.
 *
 * @author Bojan Vukasovic
 * @since 5.0
 *
 */
public final class FileProcessingRecordSerializer {
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Log logger = LogFactory.getLog(FileProcessingRecordSerializer.class);

	private FileProcessingRecordSerializer() {
		super();
	}

	public static String serialize(FileProcessingRecord record) {
		String value = "";
		try {
			value = mapper.writeValueAsString(record);
		}
		catch (JsonProcessingException e) {
			logger.warn("Exception during serialization of FileProcessingRecord. ", e);
		}
		return value;
	}

	public static FileProcessingRecord deserialize(String value) {
		FileProcessingRecord record = null;
		try {
			record = mapper.readValue(value, FileProcessingRecord.class);
		}
		catch (IOException e) {
			logger.warn("Exception during deserialization of FileProcessingRecord. ", e);
		}
		return record;
	}
}
