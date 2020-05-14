/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.sftp.outbound;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@SpringJUnitConfig
public class BigMGetTests extends org.springframework.integration.file.BigMGetTests {

	@Test
	@Disabled("needs directories and server (FTP and SFTP)") // TODO: use SftpTestSupport
	public void doTest() throws Exception {
		assertThat(this.mgetManyFiles().getPayload().size()).isEqualTo(FILES);
	}

}
