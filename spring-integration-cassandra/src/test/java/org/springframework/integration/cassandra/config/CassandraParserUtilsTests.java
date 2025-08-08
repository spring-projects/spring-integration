/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.cassandra.config.xml.CassandraParserUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
class CassandraParserUtilsTests {

	@Test
	void mutuallyExclusiveCase1() {
		String query = "";
		BeanDefinition statementExpressionDef = null;
		String ingestQuery = "";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isTrue();
	}

	@Test
	void mutuallyExclusiveCase2() {
		String query = "";
		BeanDefinition statementExpressionDef = null;
		String ingestQuery =
				"insert into book (isbn, title, author, pages, saleDate, isInStock) values (?, ?, ?, ?, ?, ?)";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isTrue();
	}

	@Test
	void mutuallyExclusiveCase3() {
		String query = "";
		BeanDefinition statementExpressionDef = new RootBeanDefinition();
		String ingestQuery = "";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isTrue();
	}

	@Test
	void mutuallyExclusiveCase4() {
		String query = "";
		BeanDefinition statementExpressionDef = new RootBeanDefinition();
		String ingestQuery =
				"insert into book (isbn, title, author, pages, saleDate, isInStock) values (?, ?, ?, ?, ?, ?)";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isFalse();
	}

	@Test
	void mutuallyExclusiveCase5() {
		String query = "SELECT * FROM book limit :size";
		BeanDefinition statementExpressionDef = new RootBeanDefinition();
		String ingestQuery = "";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isFalse();
	}

	@Test
	void mutuallyExclusiveCase6() {
		String query = "SELECT * FROM book limit :size";
		BeanDefinition statementExpressionDef = new RootBeanDefinition();
		String ingestQuery =
				"insert into book (isbn, title, author, pages, saleDate, isInStock) values (?, ?, ?, ?, ?, ?)";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isFalse();
	}

	@Test
	void mutuallyExclusiveCase7() {
		String query = "SELECT * FROM book limit :size";
		BeanDefinition statementExpressionDef = new RootBeanDefinition();
		String ingestQuery = "";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isFalse();
	}

	@Test
	void mutuallyExclusiveCase8() {
		String query = "SELECT * FROM book limit :size";
		BeanDefinition statementExpressionDef = new RootBeanDefinition();
		String ingestQuery =
				"insert into book (isbn, title, author, pages, saleDate, isInStock) values (?, ?, ?, ?, ?, ?)";
		assertThat(CassandraParserUtils.areMutuallyExclusive(query, statementExpressionDef, ingestQuery)).isFalse();
	}

}
