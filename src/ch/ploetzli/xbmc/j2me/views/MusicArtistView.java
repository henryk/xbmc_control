package ch.ploetzli.xbmc.j2me.views;

public class MusicArtistView extends MusicDatabaseView {
	public static DatabaseView get(String name) {
		return get(name, "artist", null);
	}

	public static DatabaseView get(String name, String table, String whereClause) {
		return get(MusicArtistView.class, name, "idArtist", new String[]{"strArtist"}, table, "strArtist", "strArtist", whereClause);
	}

	protected void select(String[] row) {
		if(row.length > 1) {
			String w = whereClause;
			if(w != null) w = w + " and idArtist = "+row[0];
			else w = "idArtist = "+row[0];
			DatabaseView v = MusicAlbumView.get(row[1], w);
			showChild(v);
		}
	}
}
