package ch.ploetzli.xbmc.j2me;

public class MusicGenreView extends MusicDatabaseView {
	public static DatabaseView get(String name) {
		return get(MusicGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre", "strGenre", "strGenre");
	}

}
