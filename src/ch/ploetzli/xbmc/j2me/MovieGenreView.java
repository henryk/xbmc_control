package ch.ploetzli.xbmc.j2me;

public class MovieGenreView extends DatabaseView {
	public MovieGenreView(String name) {
		super(name, "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinkmovie on genre.idGenre=genrelinkmovie.idGenre", "strGenre", "strGenre");
	}
}
