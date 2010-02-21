/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.ip.tcp;

/**
 * Definition of message formats supported by tcp channel adapters.
 * 
 * @author Gary Russell
 *
 */
public interface MessageFormats {

	/**
	 * Message has format '&lt;length&gt;&lt;message&gt;'.
	 */
	public static final int FORMAT_LENGTH_HEADER = 1;
	/**
	 * Message has format 'STX&lt;message&gt;ETX'.
	 */
	public static final int FORMAT_STX_ETX = 2;
	/**
	 * Message has format '&lt;message&gt;\r\n'.
	 */
	public static final int FORMAT_CRLF = 3;
	/**
	 * Message has custom format.
	 */
	public static final int FORMAT_CUSTOM = 99;
	
	public static final int STX = 0x02;
	
	public static final int ETX = 0x03;

}