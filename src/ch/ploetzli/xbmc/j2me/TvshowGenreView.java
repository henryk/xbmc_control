package ch.ploetzli.xbmc.j2me;

public class TvshowGenreView extends DatabaseView {
	public TvshowGenreView() {
		super("Genre", "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinktvshow on genre.idGenre=genrelinktvshow.idGenre", "strGenre", "strGenre");
	}
}
