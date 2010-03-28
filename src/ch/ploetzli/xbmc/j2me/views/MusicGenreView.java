package ch.ploetzli.xbmc.j2me.views;

public class MusicGenreView extends MusicDatabaseView {
	public static DatabaseView get(String name) {
		return get(MusicGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre", "strGenre", "strGenre");
	}

	protected void select(String[] row) {
		if(row.length > 1) {
			DatabaseView v = MusicArtistView.get(row[1], "albumview", "idGenre = "+row[0]);
			showChild(v);
		}
	}
}
