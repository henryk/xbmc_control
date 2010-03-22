package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Image;

import ch.ploetzli.xbmc.Logger;

public class TvshowEpisodeView extends DatabaseView {
	
	public static DatabaseView get(String name, String idShow) {
		return get(TvshowEpisodeView.class, name, "episodeview.idEpisode", new String[]{"c00","c12","c13","playcount"}, "episodeview", "abs(episodeview.c12),abs(episodeview.c13)", null, "episodeview.idShow="+idShow);
	}

	protected Object[] formatRow(int index, String[] data)
	{
		String label = "No label";
		Image img = null;
		if(data.length > 4) {
			label = data[2] +"x"+ data[3] + " " + data[1];
			try {
				if(data[4].equals(""))
					img = ImageFactory.getResourceImage(ImageFactory.ICON_EMPTY);
				else
					img = ImageFactory.getResourceImage(ImageFactory.ICON_FULL);
			} catch(Exception e) {
				/* Ignore, but set no image */
				Logger.getLogger().info(e);
			}
		}
		return new Object[]{label, img};
	}
}
