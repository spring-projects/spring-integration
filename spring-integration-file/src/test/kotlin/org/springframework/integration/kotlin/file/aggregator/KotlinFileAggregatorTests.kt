/*
 * Copyright 2021 the original author or authors.
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

package org.springframework.integration.kotlin.file.aggregator

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskExecutor
import org.springframework.integration.IntegrationMessageHeaderAccessor
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.file.FileHeaders
import org.springframework.integration.file.aggregator.FileAggregator
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.file.splitter.FileSplitter.FileMarker
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.PollableChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.FileOutputStream

/**
 * @author Artem Bilan
 *
 * @since 5.5
 */
@SpringJUnitConfig
@DirtiesContext
class KotlinFileAggregatorTests {

	companion object {

		lateinit var file: File

		@BeforeAll
		@JvmStatic
		fun setup(@TempDir tmpDir: File) {
			file = File(tmpDir, "foo.txt")
			val content = """
			file header
			first line
			second line
			last line
			""".trimIndent()
			FileCopyUtils.copy(content.toByteArray(), FileOutputStream(file, false))
		}

	}

	@Autowired
	@Qualifier("fileSplitterAggregatorFlow.input")
	private lateinit var fileSplitterAggregatorFlow: MessageChannel

	@Autowired
	private lateinit var resultChannel: PollableChannel

	@Test
	fun testFileAggregator() {
		this.fileSplitterAggregatorFlow.send(GenericMessage(file))
		val receive = this.resultChannel.receive(10000)
		assertThat(receive).isNotNull()
		assertThat(receive.headers)
			.all {
				contains(FileHeaders.FILENAME, "foo.txt")
				contains(FileHeaders.LINE_COUNT, 3L)
				contains("firstLine", "file header")
				doesNotContain(IntegrationMessageHeaderAccessor.CORRELATION_ID, null)
			}

		assertThat(receive.payload)
			.isInstanceOf(MutableList::class.java)
			.containsAll("SECOND LINE", "LAST LINE", "FIRST LINE")
	}

	@Configuration
	@EnableIntegration
	class Config {

		@Bean
		fun fileSplitterAggregatorFlow(taskExecutor: TaskExecutor?) =
			integrationFlow {
				split(Files.splitter().markers().firstLineAsHeader("firstLine"))
				channel { executor(taskExecutor) }
				filter<Any>({ it !is FileMarker }) { discardChannel("aggregatorChannel") }
				transform(String::toUpperCase)
				channel("aggregatorChannel")
				aggregate(FileAggregator())
				channel { queue("resultChannel") }
			}

	}

}