package ch.ploetzli.xbmc.j2me;

public class TvshowGenreView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(TvshowGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinktvshow on genre.idGenre=genrelinktvshow.idGenre", "strGenre", "strGenre");
	}
}
