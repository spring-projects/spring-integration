package org.springframework.integration.metadata;

/**
 *
 * {@link ConcurrentMetadataStore} with the ability of registering {@link MetadataStoreListener} callbacks, to be
 * invoked when changes occur in the metadata store.
 *
 * @author Marius Bogoevici
 */
public interface ListenableMetadataStore extends ConcurrentMetadataStore {

	/**
	 * Registers a listener with the metadata store
	 *
	 * @param callback the callback to be registered
	 */
	void addListener(MetadataStoreListener callback);

	/**
	 * Unregisters a listener
	 *
	 * @param callback the callback to be unregistered
	 */
	void removeListener(MetadataStoreListener callback);
}
