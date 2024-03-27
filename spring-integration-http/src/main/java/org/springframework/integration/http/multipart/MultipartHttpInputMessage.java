/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.multipart;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartRequest;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class MultipartHttpInputMessage extends ServletServerHttpRequest implements MultipartRequest {

	private final MultipartHttpServletRequest multipartServletRequest;

	public MultipartHttpInputMessage(MultipartHttpServletRequest multipartServletRequest) {
		super(multipartServletRequest);
		this.multipartServletRequest = multipartServletRequest;
	}

	public MultipartFile getFile(String name) {
		return this.multipartServletRequest.getFile(name);
	}

	public Map<String, MultipartFile> getFileMap() {
		return this.multipartServletRequest.getFileMap();
	}

	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		return this.multipartServletRequest.getMultiFileMap();
	}

	public Iterator<String> getFileNames() {
		return this.multipartServletRequest.getFileNames();
	}

	public List<MultipartFile> getFiles(String name) {
		return this.multipartServletRequest.getFiles(name);
	}

	public MultiValueMap<String, String> getParameterMap() {
		return this.multipartServletRequest.getParameterMap()
				.entrySet()
				.stream()
				.collect(LinkedMultiValueMap::new,
						(params, entry) -> params.addAll(entry.getKey(), Arrays.asList(entry.getValue())),
						LinkedMultiValueMap::addAll);
	}

	public String getMultipartContentType(String paramOrFileName) {
		return this.multipartServletRequest.getMultipartContentType(paramOrFileName);
	}

}
