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

package org.springframework.integration.jpa.support;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.util.Assert;

/**
 * This Utility contains a sub-set of utility methods from the Spring Data JPA Project.
 * As the Spring Integration JPA adapter uses only these utility methods, they
 * were copied into this class in order to prevent having to declare a dependency
 * on Spring Data JPA.
 *
 *
 * @author Oliver Gierke
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public final class JpaUtils {

	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";

	private static final Pattern ALIAS_MATCH;

	private static final String IDENTIFIER = "[\\p{Alnum}._$]+";

	private static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	static {

		StringBuilder builder = new StringBuilder();
		builder.append("(?<=from)"); // from as starting delimiter
		builder.append("(?: )+"); // at least one space separating
		builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
		builder.append("(?: as)*"); // exclude possible "as" keyword
		builder.append("(?: )+"); // at least one space separating
		builder.append("(\\w*)"); // the actual alias

		ALIAS_MATCH = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("(select\\s+((distinct )?.+?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s+)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private JpaUtils() {

	}

	/**
	 * Resolves the alias for the entity to be retrieved from the given JPA query.
	 *
	 * @param query The query.
	 * @return The alias, or null.
	 *
	 */
	public static String detectAlias(String query) {

		Matcher matcher = ALIAS_MATCH.matcher(query);

		return matcher.find() ? matcher.group(2) : null;
	}

	/**
	 * Creates a where-clause referencing the given entities and appends it to the given query string. Binds the given
	 * entities to the query.
	 *
	 * @param queryString The query string.
	 * @param entities The entities.
	 * @param entityManager The entity manager.
	 * @param <T> The entity type.
	 * @return The query.
	 *
	 */
	public static <T> Query applyAndBind(String queryString, Iterable<T> entities, EntityManager entityManager) {
		Assert.hasText(queryString, "'queryString' must not be empty");
		Assert.notNull(entities, "'entities' must not be null");
		Assert.notNull(entityManager, "'entityManager' must not be null");

		Iterator<T> iterator = entities.iterator();

		if (!iterator.hasNext()) {
			return entityManager.createQuery(queryString);
		}

		String alias = detectAlias(queryString);
		StringBuilder builder = new StringBuilder(queryString);
		builder.append(" where");

		int i = 0;

		while (iterator.hasNext()) {

			iterator.next();

			builder.append(String.format(" %s = ?%d", alias, ++i));

			if (iterator.hasNext()) {
				builder.append(" or");
			}
		}

		Query query = entityManager.createQuery(builder.toString());

		iterator = entities.iterator();
		i = 0;

		while (iterator.hasNext()) {
			query.setParameter(++i, iterator.next());
		}

		return query;
	}

	/**
	 * Returns the query string for the given class name.
	 *
	 * @param template The template.
	 * @param entityName The entity name.
	 * @return The query string.
	 */
	public static String getQueryString(String template, String entityName) {

		Assert.hasText(entityName, "Entity name must not be null or empty!");

		return String.format(template, entityName);
	}

	public static String getEntityName(EntityManager em, Class<?> entityClass) {

		return em.getMetamodel().entity(entityClass).getName();

	}

}
