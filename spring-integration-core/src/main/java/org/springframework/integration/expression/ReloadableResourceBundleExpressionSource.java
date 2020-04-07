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

package org.springframework.integration.expression;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * {@link ExpressionSource} implementation that accesses resource bundles using specified basenames.
 * This class uses {@link java.util.Properties} instances as its custom data structure for expressions,
 * loading them via a {@link org.springframework.util.PropertiesPersister} strategy: The default
 * strategy is capable of loading properties files with a specific character encoding, if desired.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @see #setCacheSeconds
 * @see #setBasenames
 * @see #setDefaultEncoding
 * @see #setFileEncodings
 * @see #setPropertiesPersister
 * @see #setResourceLoader
 * @see org.springframework.util.DefaultPropertiesPersister
 * @see org.springframework.core.io.DefaultResourceLoader
 * @see java.util.ResourceBundle
 */
public class ReloadableResourceBundleExpressionSource implements ExpressionSource, ResourceLoaderAware {

	private static final Log LOGGER = LogFactory.getLog(ReloadableResourceBundleExpressionSource.class);

	private static final String PROPERTIES_SUFFIX = ".properties";

	private static final String XML_SUFFIX = ".xml";


	/**
	 * Cache to hold filename lists per Locale
	 */
	private final Map<String, Map<Locale, List<String>>> cachedFilenames = new HashMap<>();

	/**
	 * Cache to hold already loaded properties per filename
	 */
	private final Map<String, PropertiesHolder> cachedProperties = new HashMap<>();

	/**
	 * Cache to hold merged loaded properties per locale
	 */
	private final Map<Locale, PropertiesHolder> cachedMergedProperties = new HashMap<>();

	private final ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private String[] basenames = { };

	private String defaultEncoding;

	private Properties fileEncodings;

	private boolean fallbackToSystemLocale = true;

	private long cacheMillis = -1;

	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

	private ResourceLoader resourceLoader = new DefaultResourceLoader();


	/**
	 * Set a single basename, following the basic ResourceBundle convention of
	 * not specifying file extension or language codes, but referring to a Spring
	 * resource location: e.g. "META-INF/expressions" for "META-INF/expressions.properties",
	 * "META-INF/expressions_en.properties", etc.
	 * <p>XML properties files are also supported: .g. "META-INF/expressions" will find
	 * and load "META-INF/expressions.xml", "META-INF/expressions_en.xml", etc as well.
	 * @param basename the single basename
	 * @see #setBasenames
	 * @see org.springframework.core.io.ResourceEditor
	 * @see java.util.ResourceBundle
	 */
	public void setBasename(String basename) {
		setBasenames(new String[] { basename });
	}

	/**
	 * Set an array of basenames, each following the basic ResourceBundle convention
	 * of not specifying file extension or language codes, but referring to a Spring
	 * resource location: e.g. "META-INF/expressions" for "META-INF/expressions.properties",
	 * "META-INF/expressions_en.properties", etc.
	 * <p>XML properties files are also supported: .g. "META-INF/expressions" will find
	 * and load "META-INF/expressions.xml", "META-INF/expressions_en.xml", etc as well.
	 * <p>The associated resource bundles will be checked sequentially when resolving
	 * an expression key. Note that expression definitions in a <i>previous</i> resource
	 * bundle will override ones in a later bundle, due to the sequential lookup.
	 * @param basenames an array of basenames
	 * @see #setBasename
	 * @see java.util.ResourceBundle
	 */
	public void setBasenames(@Nullable String[] basenames) {
		if (basenames != null) {
			this.basenames = new String[basenames.length];
			for (int i = 0; i < basenames.length; i++) {
				String basename = basenames[i];
				Assert.hasText(basename, "Basename must not be empty");
				this.basenames[i] = basename.trim();
			}
		}
		else {
			this.basenames = new String[0];
		}
	}

	/**
	 * Set the default charset to use for parsing properties files.
	 * Used if no file-specific charset is specified for a file.
	 * <p>Default is none, using the <code>java.util.Properties</code>
	 * default encoding.
	 * <p>Only applies to classic properties files, not to XML files.
	 * @param defaultEncoding the default charset
	 * @see #setFileEncodings
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Set per-file charsets to use for parsing properties files.
	 * <p>Only applies to classic properties files, not to XML files.
	 * @param fileEncodings Properties with filenames as keys and charset
	 * names as values. Filenames have to match the basename syntax,
	 * with optional locale-specific appendices: e.g. "META-INF/expressions"
	 * or "META-INF/expressions_en".
	 * @see #setBasenames
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setFileEncodings(Properties fileEncodings) {
		this.fileEncodings = fileEncodings;
	}

	/**
	 * Set whether to fall back to the system Locale if no files for a specific
	 * Locale have been found. Default is "true"; if this is turned off, the only
	 * fallback will be the default file (e.g. "expressions.properties" for
	 * basename "expressions").
	 * <p>Falling back to the system Locale is the default behavior of
	 * <code>java.util.ResourceBundle</code>. However, this is often not
	 * desirable in an application server environment, where the system Locale
	 * is not relevant to the application at all: Set this flag to "false"
	 * in such a scenario.
	 * @param fallbackToSystemLocale true to fall back.
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	/**
	 * Set the number of seconds to cache loaded properties files.
	 * <ul>
	 * <li>Default is "-1", indicating to cache forever (just like
	 * <code>java.util.ResourceBundle</code>).
	 * <li>A positive number will cache loaded properties files for the given
	 * number of seconds. This is essentially the interval between refresh checks.
	 * Note that a refresh attempt will first check the last-modified timestamp
	 * of the file before actually reloading it; so if files don't change, this
	 * interval can be set rather low, as refresh attempts will not actually reload.
	 * <li>A value of "0" will check the last-modified timestamp of the file on
	 * every expression access. <b>Do not use this in a production environment!</b>
	 * </ul>
	 * @param cacheSeconds The cache seconds.
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheMillis = (cacheSeconds * 1000); // NOSONAR
	}

	/**
	 * Set the PropertiesPersister to use for parsing properties files.
	 * <p>The default is a DefaultPropertiesPersister.
	 * @param propertiesPersister The properties persister.
	 * @see org.springframework.util.DefaultPropertiesPersister
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
	}

	/**
	 * Set the ResourceLoader to use for loading bundle properties files.
	 * <p>The default is a DefaultResourceLoader. Will get overridden by the
	 * ApplicationContext if running in a context, as it implements the
	 * ResourceLoaderAware interface. Can be manually overridden when
	 * running outside of an ApplicationContext.
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.context.ResourceLoaderAware
	 */
	@Override
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}


	/**
	 * Resolves the given key in the retrieved bundle files to an Expression.
	 */
	@Override
	public Expression getExpression(String key, Locale locale) {
		String expressionString = getExpressionString(key, locale);
		if (expressionString != null) {
			return this.parser.parseExpression(expressionString);
		}
		return null;
	}

	@Nullable
	private String getExpressionString(String key, Locale locale) {
		if (this.cacheMillis < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			return propHolder.getProperty(key);
		}
		else {
			for (String basename : this.basenames) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					String result = propHolder.getProperty(key);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Get a PropertiesHolder that contains the actually visible properties
	 * for a Locale, after merging all specified resource bundles.
	 * Either fetches the holder from the cache or freshly loads it.
	 * <p> Only used when caching resource bundle contents forever, i.e.
	 * with cacheSeconds < 0. Therefore, merged properties are always
	 * cached forever.
	 */
	private PropertiesHolder getMergedProperties(Locale locale) {
		synchronized (this.cachedMergedProperties) {
			PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
			if (mergedHolder != null) {
				return mergedHolder;
			}
			Properties mergedProps = new Properties();
			mergedHolder = new PropertiesHolder(mergedProps, -1);
			for (int i = this.basenames.length - 1; i >= 0; i--) {
				List<String> filenames = calculateAllFilenames(this.basenames[i], locale);
				for (int j = filenames.size() - 1; j >= 0; j--) {
					String filename = filenames.get(j);
					PropertiesHolder propHolder = getProperties(filename);
					Properties propHolderProperties = propHolder.getProperties();
					if (propHolderProperties != null) {
						mergedProps.putAll(propHolderProperties);
					}
				}
			}
			this.cachedMergedProperties.put(locale, mergedHolder);
			return mergedHolder;
		}
	}

	/**
	 * Calculate all filenames for the given bundle basename and Locale.
	 * Will calculate filenames for the given Locale, the system Locale
	 * (if applicable), and the default file.
	 * @param basename the basename of the bundle
	 * @param locale the locale
	 * @return the List of filenames to check
	 * @see #setFallbackToSystemLocale
	 * @see #calculateFilenamesForLocale
	 */
	private List<String> calculateAllFilenames(String basename, Locale locale) {
		synchronized (this.cachedFilenames) {
			Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
			if (localeMap != null) {
				List<String> filenames = localeMap.get(locale);
				if (filenames != null) {
					return filenames;
				}
			}
			List<String> filenames = new ArrayList<>(calculateFilenamesForLocale(basename, locale));
			if (this.fallbackToSystemLocale && !locale.equals(Locale.getDefault())) {
				List<String> fallbackFilenames = calculateFilenamesForLocale(basename, Locale.getDefault());
				for (String fallbackFilename : fallbackFilenames) {
					if (!filenames.contains(fallbackFilename)) {
						// Entry for fallback locale that isn't already in filenames list.
						filenames.add(fallbackFilename);
					}
				}
			}
			filenames.add(basename);
			if (localeMap != null) {
				localeMap.put(locale, filenames);
			}
			else {
				localeMap = new HashMap<>();
				localeMap.put(locale, filenames);
				this.cachedFilenames.put(basename, localeMap);
			}
			return filenames;
		}
	}

	/**
	 * Calculate the filenames for the given bundle basename and Locale,
	 * appending language code, country code, and variant code.
	 * E.g.: basename "expressions", Locale "de_AT_oo" -> "expressions_de_AT_OO",
	 * "expressions_de_AT", "expressions_de".
	 * <p>Follows the rules defined by {@link java.util.Locale#toString()}.
	 * @param basename the basename of the bundle
	 * @param locale the locale
	 * @return the List of filenames to check
	 */
	private List<String> calculateFilenamesForLocale(String basename, Locale locale) {
		List<String> result = new ArrayList<>();
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		StringBuilder temp = new StringBuilder(basename);

		temp.append('_');
		if (language.length() > 0) {
			temp.append(language);
			result.add(0, temp.toString());
		}

		temp.append('_');
		if (country.length() > 0) {
			temp.append(country);
			result.add(0, temp.toString());
		}

		if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
			temp.append('_').append(variant);
			result.add(0, temp.toString());
		}

		return result;
	}


	/**
	 * Get a PropertiesHolder for the given filename, either from the
	 * cache or freshly loaded.
	 * @param filename the bundle filename (basename + Locale)
	 * @return the current PropertiesHolder for the bundle
	 */
	private PropertiesHolder getProperties(String filename) {
		synchronized (this.cachedProperties) {
			PropertiesHolder propHolder = this.cachedProperties.get(filename);
			if (propHolder != null &&
					(propHolder.getRefreshTimestamp() < 0 ||
							propHolder.getRefreshTimestamp() > System.currentTimeMillis() - this.cacheMillis)) {
				return propHolder;
			}
			return refreshProperties(filename, propHolder);
		}
	}

	/**
	 * Refresh the PropertiesHolder for the given bundle filename.
	 * The holder can be <code>null</code> if not cached before, or a timed-out cache entry
	 * (potentially getting re-validated against the current last-modified timestamp).
	 * @param filename the bundle filename (basename + Locale)
	 * @param propHolderArg the current PropertiesHolder for the bundle
	 */
	private PropertiesHolder refreshProperties(String filename, @Nullable PropertiesHolder propHolderArg) {
		PropertiesHolder propHolder = propHolderArg;
		long refreshTimestamp = (this.cacheMillis < 0) ? -1 : System.currentTimeMillis();

		Resource resource = getResource(filename);

		if (resource.exists()) {
			long fileTimestamp = -1;
			if (this.cacheMillis >= 0) {
				// Last-modified timestamp of file will just be read if caching with timeout.
				try {
					fileTimestamp = resource.lastModified();
					if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Re-caching properties for filename [" + filename
									+ "] - file hasn't been modified");
						}
						propHolder.setRefreshTimestamp(refreshTimestamp);
						return propHolder;
					}
				}
				catch (IOException ex) {
					// Probably a class path resource: cache it forever.
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug(resource
								+ " could not be resolved in the file system - assuming that is hasn't changed", ex);
					}
					fileTimestamp = -1;
				}
			}
			propHolder = load(filename, resource, fileTimestamp);
		}

		else {
			// Resource does not exist.
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No properties file found for [" + filename + "] - neither plain properties nor XML");
			}
			// Empty holder representing "not found".
			propHolder = new PropertiesHolder();
		}

		propHolder.setRefreshTimestamp(refreshTimestamp);
		this.cachedProperties.put(filename, propHolder);
		return propHolder;
	}

	private Resource getResource(String filename) {
		Resource resource = this.resourceLoader.getResource(filename + PROPERTIES_SUFFIX);
		if (!resource.exists()) {
			resource = this.resourceLoader.getResource(filename + XML_SUFFIX);
		}
		return resource;
	}

	private PropertiesHolder load(String filename, Resource resource, long fileTimestamp) {
		PropertiesHolder propHolder;
		try {
			Properties props = loadProperties(resource, filename);
			propHolder = new PropertiesHolder(props, fileTimestamp);
		}
		catch (IOException ex) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
			}
			// Empty holder representing "not valid".
			propHolder = new PropertiesHolder();
		}
		return propHolder;
	}

	/**
	 * Load the properties from the given resource.
	 * @param resource the resource to load from
	 * @param filename the original bundle filename (basename + Locale)
	 * @return the populated Properties instance
	 * @throws IOException if properties loading failed
	 */
	private Properties loadProperties(Resource resource, String filename) throws IOException {
		try (InputStream is = resource.getInputStream()) {
			Properties props = new Properties();
			String resourceFilename = resource.getFilename();
			if (resourceFilename != null && resourceFilename.endsWith(XML_SUFFIX)) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Loading properties [" + resourceFilename + "]");
				}
				this.propertiesPersister.loadFromXml(props, is);
			}
			else {
				loadFromProperties(resource, filename, is, props, resourceFilename);
			}
			return props;
		}
	}

	private void loadFromProperties(Resource resource, String filename, InputStream is, Properties props,
			@Nullable String resourceFilename) throws IOException {

		String encoding = null;
		if (this.fileEncodings != null) {
			encoding = this.fileEncodings.getProperty(filename);
		}
		if (encoding == null) {
			encoding = this.defaultEncoding;
		}
		if (encoding != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Loading properties ["
						+ (resourceFilename == null ? resource : resourceFilename)
						+ "] with encoding '" + encoding + "'");
			}
			this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
		}
		else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Loading properties [" + (resourceFilename == null ? resource : resourceFilename)
						+ "]");
			}
			this.propertiesPersister.load(props, is);
		}
	}


	/**
	 * Clear the resource bundle cache.
	 * Subsequent resolve calls will lead to reloading of the properties files.
	 */
	public void clearCache() {
		LOGGER.debug("Clearing entire resource bundle cache");
		synchronized (this.cachedProperties) {
			this.cachedProperties.clear();
		}
		synchronized (this.cachedMergedProperties) {
			this.cachedMergedProperties.clear();
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + ": basenames=[" + StringUtils.arrayToCommaDelimitedString(this.basenames) + "]";
	}


	/**
	 * PropertiesHolder for caching.
	 * Stores the last-modified timestamp of the source file for efficient
	 * change detection, and the timestamp of the last refresh attempt
	 * (updated every time the cache entry gets re-validated).
	 */
	private static final class PropertiesHolder {

		private Properties properties;

		private long fileTimestamp = -1;

		private long refreshTimestamp = -1;

		PropertiesHolder() {
		}

		PropertiesHolder(Properties properties, long fileTimestamp) {
			this.properties = properties;
			this.fileTimestamp = fileTimestamp;
		}

		@Nullable
		public Properties getProperties() {
			return this.properties;
		}

		public long getFileTimestamp() {
			return this.fileTimestamp;
		}

		public void setRefreshTimestamp(long refreshTimestamp) {
			this.refreshTimestamp = refreshTimestamp;
		}

		public long getRefreshTimestamp() {
			return this.refreshTimestamp;
		}

		@Nullable
		public String getProperty(String code) {
			if (this.properties == null) {
				return null;
			}
			return this.properties.getProperty(code);
		}

	}

}
