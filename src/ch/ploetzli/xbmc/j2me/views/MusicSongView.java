package ch.ploetzli.xbmc.j2me.views;

import ch.ploetzli.xbmc.Logger;
import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.j2me.DatabaseTopMenu;
import ch.ploetzli.xbmc.j2me.RemoteControl;

public class MusicSongView extends MusicDatabaseView {
	public static DatabaseView get(String name, String whereClause) {
		return get(MusicSongView.class, name, "idSong", new String[]{"strTitle"}, "songview", "iTrack", null, whereClause);
	}
	
	protected void select(final int index) {
		DatabaseTopMenu topMenu = getDatabaseTopMenu();
		if(topMenu == null)
			return;
		final HttpApi api = topMenu.getApi();
		if(api == null)
			return;
		
		final RemoteControl rc = (RemoteControl)getRoot().getChildByClass(RemoteControl.class);
		
		/* We're just going to assume that the order of entries hasn't changed between the fetch
		 * of this database view and the select operation. In that case the selection index should
		 * perfectly correspond to setPlayListSong(), if the correct where and order clauses are
		 * given.
		 */
		new Thread(new Runnable() {
			public void run() {
				try {
					api.clearPlayList(0);
					api.setCurrentPlayList(0);
					String w = whereClause;
					if(orderClause != null)
						w = w + " ORDER BY " + orderClause;
					api.addToPlayListFromDB("songs", w);
					api.setPlayListSong(index);
					if(rc != null)
						showChild(rc);
				} catch (Exception e) {
					Logger.getLogger().error(e);
				}
			}
		}).start();
	}

}
