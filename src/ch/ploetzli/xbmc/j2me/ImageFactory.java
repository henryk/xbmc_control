package ch.ploetzli.xbmc.j2me;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import ch.ploetzli.xbmc.LRUHashtable;
import ch.ploetzli.xbmc.api.HttpApi;

public class ImageFactory {
	private static Hashtable resourceImageCache = new Hashtable();
	private static Hashtable remoteImageCache = new LRUHashtable(100);
	public static final String ICON_EMPTY = "ball_none.png";
	public static final String ICON_HALF = "ball_half.png";
	public static final String ICON_FULL = "ball.png";
	
	/**
	 * Returns a reference to an immutable image represented by the resource
	 * 	imageName (as returned by getResourceAsStream(), e.g. the file must
	 *  be in the JAR in the same package directory as this class). Image objects
	 *  are statically cached and subsequent calls with the same imageName return
	 *  the same object.
	 * @param imageName A file name in the JAR in the same directory as this class.
	 * @return An image object created from that file, or null if the file is not found.
	 * @throws IOException
	 */
	public static Image getResourceImage(String imageName) throws IOException {
		Image img = null;
		if(imageName == null)
			return null;
		img = (Image)resourceImageCache.get(imageName);
		if(img == null) {
			InputStream is = ImageFactory.class.getResourceAsStream(imageName);
			if(is != null) {
				img = Image.createImage(is);
				if(img != null) {
					resourceImageCache.put(imageName, img);
				}
			}
		}
		return img;
	}
	
	public static Image getRemoteImage(HttpApi api, String url) throws IOException, IllegalArgumentException {
		if(url == null)
			return null;
		Image img = (Image) remoteImageCache.get(url);
		if(img == null) {
			byte[] data = api.fileDownload(url);
			if(data.length == 0)
				return null;
			img = Image.createImage(data, 0, data.length);
			if(img != null)
				remoteImageCache.put(url, img);
		}
		return img;
	}
}
