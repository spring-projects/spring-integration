/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
