/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.cassandra.test.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class BookSampler {

	public static List<Book> getBookList(int numBooks) {
		List<Book> books = new ArrayList<>();
		for (int i = 0; i < numBooks - 1; i++) {
			books.add(new Book(UUID.randomUUID().toString(), "Spring Data Cassandra Guide", "Cassandra Guru puppy",
					i * 10 + 5, LocalDate.now(), true));
		}
		books.add(getBook());
		return books;
	}

	public static Book getBook() {
		return new Book("123456-1", "Spring Integration Cassandra", "Cassandra Guru", 521, LocalDate.now(), true);
	}

	private BookSampler() {
	}

}
