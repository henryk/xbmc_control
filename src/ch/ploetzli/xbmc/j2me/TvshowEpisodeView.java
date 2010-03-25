package ch.ploetzli.xbmc.j2me;

import javax.microedition.lcdui.Image;

import ch.ploetzli.xbmc.Logger;
import ch.ploetzli.xbmc.api.HttpApi;

public class TvshowEpisodeView extends VideoDatabaseView {
	
	public static DatabaseView get(String name, String idShow) {
		return get(TvshowEpisodeView.class, name, "episodeview.idEpisode", new String[]{"c00","c12","c13","playcount","strPath","strFileName"}, "episodeview", "abs(episodeview.c12),abs(episodeview.c13)", null, "episodeview.idShow="+idShow);
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

	protected void select(final String[] row) {
		DatabaseTopMenu topMenu = getDatabaseTopMenu();
		if(topMenu == null)
			return;
		final HttpApi api = topMenu.getApi();
		if(api == null)
			return;
		if(row.length < 7)
			return;
		
		final RemoteControl rc = (RemoteControl)getRoot().getChildByClass(RemoteControl.class);
		
		/* In principle one would want to use addToPlayListFromDB to be abstract from whatever
		 * path or other location information is internally used. However, it is broken.
		 * F.e. AddToPlayListFromDB(episodes;idEpisode=1923) for me generates a playlist item
		 * with a path of 43/1/1923 which are the idShow/Season/idEpisode respectively, and
		 * then "ERROR: CDVDPlayer::OpenInputStream - error opening 43/1/1923" is logged on
		 * the XBMC, clearly indicating that the player can not cope with this.
		 * Instead, use addToPlayList() with strPath+strFileName from the database and hope
		 * that things don't break too hard.
		 */
		new Thread(new Runnable() {
			public void run() {
				try {
					api.clearPlayList(1);
					api.setCurrentPlayList(1);
					if(!row[6].startsWith("stack://"))
						api.addToPlayList(row[5]+row[6]);
					else
						api.addToPlayList(row[6]);
					api.setPlayListSong(1);
					if(rc != null)
						showChild(rc);
				} catch (Exception e) {
					Logger.getLogger().error(e);
				}
			}
		}).start();
	}
}
