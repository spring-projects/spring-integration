/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
