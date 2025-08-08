/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.test.domain;

import java.time.LocalDate;

import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Test POJO
 *
 * @author David Webb
 * @author Artem Bilan
 *
 * @since 6.0
 */
@Table("book")
public record Book(
		@PrimaryKey String isbn,
		String title,
		@Indexed String author,
		Integer pages,
		LocalDate saleDate,
		Boolean isInStock) {

}
