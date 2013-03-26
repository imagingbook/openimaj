package org.openimaj.demos.sandbox.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openimaj.image.Image;
import org.openimaj.io.ObjectReader;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.collections.CollectionsInterface;
import com.aetrion.flickr.collections.CollectionsSearchParameters;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photosets.PhotosetsInterface;

/**
 * Class to dynamically create image datasets from flickr through various api
 * calls.
 * 
 * @author Jonathon Hare (jsh2@ecs.soton.ac.uk)
 * 
 * @param <IMAGE>
 *            The type of {@link Image} instance held by the dataset.
 */
public class FlickrImageDataset<IMAGE extends Image<?, IMAGE>> extends ReadableListDataset<IMAGE> {
	private final static Pattern GALLERY_URL_PATTERN = Pattern.compile(".*/photos/.*/galleries/[0-9]*(/|$)");
	private final static Pattern PHOTOSET_URL_PATTERN = Pattern.compile(".*/photos/.*/sets/([0-9]*)(/|$)");
	private final static Pattern COLLECTION_URL_PATTERN = Pattern.compile(".*/photos/(.*)/collections/([0-9]*)(/|$)");

	protected List<Photo> photos;

	protected FlickrImageDataset(ObjectReader<IMAGE> reader, List<Photo> photos) {
		super(reader);

		this.photos = photos;
	}

	/**
	 * Get the underlying flickr {@link Photo} objects.
	 * 
	 * @return the underlying list of {@link Photo}s.
	 */
	public List<Photo> getPhotos() {
		return photos;
	}

	/**
	 * Get the a specific underlying flickr {@link Photo} object corresponding
	 * to a particular image instance.
	 * 
	 * @param index
	 *            the index of the instance
	 * 
	 * @return the underlying {@link Photo} corresponding to the given instance
	 *         index.
	 */
	public Photo getPhoto(int index) {
		return photos.get(index);
	}

	@Override
	public IMAGE getInstance(int index) {
		return read(photos.get(index));
	}

	@Override
	public int size() {
		return photos.size();
	}

	private IMAGE read(Photo next) {
		if (next == null)
			return null;

		InputStream stream = null;
		try {
			stream = new URL(next.getUrl()).openStream();
			return reader.read(stream);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (final IOException e) {
				// ignore
			}
		}
	}

	@Override
	public Iterator<IMAGE> iterator() {
		return new Iterator<IMAGE>() {
			Iterator<Photo> internal = photos.iterator();

			@Override
			public boolean hasNext() {
				return internal.hasNext();
			}

			@Override
			public IMAGE next() {
				return read(internal.next());
			}

			@Override
			public void remove() {
				internal.remove();
			}
		};
	}

	@Override
	public String toString() {
		return String.format("%s(%d images)", this.getClass().getName(), this.photos.size());
	}

	/**
	 * Create an image dataset from the flickr gallery, photoset or collection
	 * at the given url.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param url
	 *            the url of the collection/gallery/photo-set
	 * @return a {@link FlickrImageDataset} created from the given url
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> create(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			URL url) throws Exception
	{
		return create(reader, apikey, secret, url, 0);
	}

	/**
	 * Create an image dataset by searching flickr with the given search terms.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param searchTerms
	 *            the search terms; space separated. Prepending a term with a
	 *            "-" means that the term should not appear.
	 * @return a {@link FlickrImageDataset} created from the given url
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> create(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String searchTerms) throws Exception
	{
		return create(reader, apikey, secret, searchTerms, 0);
	}

	/**
	 * Create an image dataset by searching flickr with the given search terms.
	 * The number of images can be limited to a subset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param searchTerms
	 *            the search terms; space separated. Prepending a term with a
	 *            "-" means that the term should not appear.
	 * @param number
	 *            the maximum number of images to add to the dataset. Setting to
	 *            0 or less will attempt to use all the images.
	 * @return a {@link FlickrImageDataset} created from the given url
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> create(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String searchTerms, int number) throws Exception
	{
		final com.aetrion.flickr.photos.SearchParameters params = new com.aetrion.flickr.photos.SearchParameters();
		params.setText(searchTerms);

		return createFromSearch(reader, apikey, secret, params, number);
	}

	/**
	 * Create an image dataset from the flickr gallery, photoset or collection
	 * at the given url. The number of images can be limited to a subset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param url
	 *            the url of the collection/gallery/photo-set
	 * @param number
	 *            the maximum number of images to add to the dataset. Setting to
	 *            0 or less will attempt to use all the images.
	 * @return a {@link FlickrImageDataset} created from the given url
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> create(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			URL url, int number) throws Exception
	{
		final String urlString = url.toString();

		if (GALLERY_URL_PATTERN.matcher(urlString).matches()) {
			return fromGallery(reader, apikey, secret, urlString, number);
		} else if (PHOTOSET_URL_PATTERN.matcher(urlString).matches()) {
			return fromPhotoset(reader, apikey, secret, urlString, number);
		} else if (COLLECTION_URL_PATTERN.matcher(urlString).matches()) {
			return fromCollection(reader, apikey, secret, urlString, number);
		}

		throw new IllegalArgumentException("Unknown URL type " + urlString);
	}

	private static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> fromGallery(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String urlString, int number) throws Exception
	{
		final Flickr flickr = new Flickr(apikey, secret, new REST(Flickr.DEFAULT_HOST));

		final String galleryId = flickr.getUrlsInterface().lookupGallery(urlString);

		final com.aetrion.flickr.galleries.SearchParameters params = new com.aetrion.flickr.galleries.SearchParameters();
		params.setGalleryId(galleryId);

		return createFromGallery(reader, apikey, secret, params, number);
	}

	private static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> fromPhotoset(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String urlString, int number) throws Exception
	{
		final Matcher matcher = PHOTOSET_URL_PATTERN.matcher(urlString);
		matcher.find();
		final String setId = matcher.group(1);

		return createFromPhotoset(reader, apikey, secret, setId, number);
	}

	private static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> fromCollection(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String urlString, int number) throws Exception
	{
		final Flickr flickr = new Flickr(apikey, secret, new REST(Flickr.DEFAULT_HOST));

		final Matcher matcher = COLLECTION_URL_PATTERN.matcher(urlString);
		matcher.find();
		final String userName = matcher.group(1);
		final String collectionsId = matcher.group(2);

		final CollectionsSearchParameters params = new CollectionsSearchParameters();
		params.setCollectionId(collectionsId);
		params.setUserId(flickr.getPeopleInterface().findByUsername(userName).getId());

		return createFromCollection(reader, apikey, secret, params, number);
	}

	/**
	 * Create an image dataset from a flickr gallery with the specified
	 * parameters.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param params
	 *            the parameters describing the gallery and any additional
	 *            constraints.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromGallery(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			com.aetrion.flickr.galleries.SearchParameters params) throws Exception
	{
		return createFromGallery(reader, apikey, secret, params, 0);
	}

	/**
	 * Create an image dataset from a flickr gallery with the specified
	 * parameters. The number of images can be limited to a subset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param params
	 *            the parameters describing the gallery and any additional
	 *            constraints.
	 * @param number
	 *            the maximum number of images to add to the dataset. Setting to
	 *            0 or less will attempt to use all the images.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	@SuppressWarnings("unchecked")
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromGallery(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			com.aetrion.flickr.galleries.SearchParameters params, int number) throws Exception
	{
		final Flickr flickr = new Flickr(apikey, secret, new REST(Flickr.DEFAULT_HOST));

		final List<Photo> photos = new ArrayList<Photo>();
		final PhotoList first = flickr.getGalleriesInterface().getPhotos(params, 250, 0);
		photos.addAll(first);

		if (number > 0)
			number = Math.min(number, first.getTotal());

		for (int page = 1, n = photos.size(); n < number; page++) {
			final PhotoList result = flickr.getGalleriesInterface().getPhotos(params, 250, page);
			photos.addAll(result);
			n += result.size();
		}

		return new FlickrImageDataset<IMAGE>(reader, photos);
	}

	/**
	 * Create an image dataset from a flickr photoset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param setId
	 *            the photoset identifier
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromPhotoset(
			ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String setId) throws Exception
	{
		return createFromPhotoset(reader, apikey, secret, setId, 0);
	}

	/**
	 * Create an image dataset from a flickr photoset. The number of images can
	 * be limited to a subset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param setId
	 *            the photoset identifier
	 * @param number
	 *            the maximum number of images to add to the dataset. Setting to
	 *            0 or less will attempt to use all the images.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	@SuppressWarnings("unchecked")
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromPhotoset(
			ObjectReader<IMAGE> reader,
			String apikey, String secret,
			String setId, int number) throws Exception
	{
		final Flickr flickr = new Flickr(apikey, secret, new REST(Flickr.DEFAULT_HOST));

		final PhotosetsInterface setsInterface = flickr.getPhotosetsInterface();

		final List<Photo> photos = new ArrayList<Photo>();
		final PhotoList first = setsInterface.getPhotos(setId, 250, 0);
		photos.addAll(first);

		if (number > 0)
			number = Math.min(number, first.getTotal());

		for (int page = 1, n = photos.size(); n < number; page++) {
			final PhotoList result = setsInterface.getPhotos(setId, 250, page);
			photos.addAll(result);
			n += result.size();
		}

		return new FlickrImageDataset<IMAGE>(reader, photos);
	}

	/**
	 * Create an image dataset from a flickr collection with the specified
	 * parameters.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param params
	 *            the parameters describing the gallery and any additional
	 *            constraints.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromCollection(
			ObjectReader<IMAGE> reader,
			String apikey, String secret,
			com.aetrion.flickr.collections.CollectionsSearchParameters params) throws Exception
	{
		return createFromCollection(reader, apikey, secret, params, 0);
	}

	/**
	 * Create an image dataset from a flickr collection with the specified
	 * parameters. The number of images can be limited to a subset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param params
	 *            the parameters describing the gallery and any additional
	 *            constraints.
	 * @param number
	 *            the maximum number of images to add to the dataset. Setting to
	 *            0 or less will attempt to use all the images.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	@SuppressWarnings("unchecked")
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromCollection(
			ObjectReader<IMAGE> reader,
			String apikey, String secret,
			com.aetrion.flickr.collections.CollectionsSearchParameters params, int number) throws Exception
	{
		final Flickr flickr = new Flickr(apikey, secret, new REST(Flickr.DEFAULT_HOST));

		List<Photo> photos = new ArrayList<Photo>();
		final CollectionsInterface collectionsInterface = flickr.getCollectionsInterface();
		final PhotoList photoList = collectionsInterface.getTree(params).getPhotoUrls(flickr.getPhotosetsInterface());
		photos.addAll(photoList);

		if (number > 0 && number < photos.size())
			photos = photos.subList(0, number);

		return new FlickrImageDataset<IMAGE>(reader, photos);
	}

	/**
	 * Create an image dataset from a flickr search with the specified
	 * parameters.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param params
	 *            the parameters describing the gallery and any additional
	 *            constraints.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromSearch(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			com.aetrion.flickr.photos.SearchParameters params) throws Exception
	{
		return createFromSearch(reader, apikey, secret, params, 0);
	}

	/**
	 * Create an image dataset from a flickr search with the specified
	 * parameters. The number of images can be limited to a subset.
	 * 
	 * @param reader
	 *            the reader with which to load the images
	 * @param apikey
	 *            the flickr api key
	 * @param secret
	 *            the flickr api secret
	 * @param params
	 *            the parameters describing the gallery and any additional
	 *            constraints.
	 * @param number
	 *            the maximum number of images to add to the dataset. Setting to
	 *            0 or less will attempt to use all the images.
	 * @return a {@link FlickrImageDataset} created from the gallery described
	 *         by the given parameters
	 * @throws Exception
	 *             if an error occurs
	 */
	@SuppressWarnings("unchecked")
	public static <IMAGE extends Image<?, IMAGE>> FlickrImageDataset<IMAGE> createFromSearch(ObjectReader<IMAGE> reader,
			String apikey, String secret,
			com.aetrion.flickr.photos.SearchParameters params, int number) throws Exception
	{
		final Flickr flickr = new Flickr(apikey, secret, new REST(Flickr.DEFAULT_HOST));

		final List<Photo> photos = new ArrayList<Photo>();
		final PhotoList first = flickr.getPhotosInterface().search(params, 250, 0);
		photos.addAll(first);

		if (number > 0)
			number = Math.min(number, first.getTotal());

		for (int page = 1, n = photos.size(); n < number; page++) {
			final PhotoList result = flickr.getPhotosInterface().search(params, 250, page);
			photos.addAll(result);
			n += result.size();
		}

		return new FlickrImageDataset<IMAGE>(reader, photos);
	}
}
