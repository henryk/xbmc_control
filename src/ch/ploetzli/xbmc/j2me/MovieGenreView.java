package ch.ploetzli.xbmc.j2me;

public class MovieGenreView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(MovieGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinkmovie on genre.idGenre=genrelinkmovie.idGenre", "strGenre", "strGenre");
	}
}
