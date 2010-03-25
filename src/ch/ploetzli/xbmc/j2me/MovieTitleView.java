package ch.ploetzli.xbmc.j2me;

import java.io.IOException;

import ch.ploetzli.xbmc.api.HttpApi;

public class MovieTitleView extends VideoDatabaseView {
	public static DatabaseView get(String name) {
		return get(name, null);
	}

	public static DatabaseView get(String name, String whereClause) {
		return get(name, "movieview", whereClause);
	}

	public static DatabaseView get(String name, String table, String whereClause) {
		return get(MovieTitleView.class, name, "movieview.idMovie", new String[]{"c00","strPath","strFileName"}, table, "c00", null, whereClause);
	}

	protected void select(final String[] row) {
		DatabaseTopMenu topMenu = getDatabaseTopMenu();
		if(topMenu == null)
			return;
		final HttpApi api = topMenu.getApi();
		if(api == null)
			return;
		if(row.length < 4)
			return;
		
		final RemoteControl rc = (RemoteControl)getRoot().getChildByClass(RemoteControl.class);
		
		new Thread(new Runnable() {
			public void run() {
				try {
					/* See TvshowEpisodeView.select for a rant on why the path */
					api.clearPlayList(1);
					api.setCurrentPlayList(1);
					if(!row[3].startsWith("stack://"))
						api.addToPlayList(row[2]+row[3]);
					else
						api.addToPlayList(row[3]);
					api.setPlayListSong(1);
					if(rc != null)
						showChild(rc);
				} catch (IOException e) {;}
			}
		}).start();
	}
}
