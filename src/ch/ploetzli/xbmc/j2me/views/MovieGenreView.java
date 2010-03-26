package ch.ploetzli.xbmc.j2me.views;

public class MovieGenreView extends VideoDatabaseView {
	public static DatabaseView get(String name) {
		return get(MovieGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinkmovie on genre.idGenre=genrelinkmovie.idGenre", "strGenre", "strGenre");
	}
	
	protected void select(String[] row) {
		if(row.length > 1) {
			DatabaseView v = MovieTitleView.get(row[1], "movieview join genrelinkmovie on genrelinkmovie.idMovie = movieview.idMovie", "genrelinkmovie.idGenre = "+row[0]);
			showChild(v);
		}
	}
}
