package ch.ploetzli.xbmc.j2me.views;

public class TvshowGenreView extends VideoDatabaseView {
	public static DatabaseView get(String name) {
		return get(TvshowGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinktvshow on genre.idGenre=genrelinktvshow.idGenre", "strGenre", "strGenre");
	}
	
	protected void select(String[] row) {
		if(row.length > 1) {
			DatabaseView v = TvshowTitleView.get(row[1], "tvshowview join genrelinktvshow on genrelinktvshow.idShow = tvshowview.idShow", "genrelinktvshow.idGenre = "+row[0]);
			showChild(v);
		}
	}
}
