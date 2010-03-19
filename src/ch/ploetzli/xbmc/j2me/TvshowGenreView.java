package ch.ploetzli.xbmc.j2me;

public class TvshowGenreView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(TvshowGenreView.class, name, "genre.idGenre", new String[]{"strGenre"}, "genre join genrelinktvshow on genre.idGenre=genrelinktvshow.idGenre", "strGenre", "strGenre");
	}
	
	protected void select(int index) {
		if(index >= 0 && index < cache.size()) {
			String[] row = (String[])cache.elementAt(index);
			if(row.length > 1) {
				DatabaseView v = TvshowTitleView.get(row[1], "tvshowview join genrelinktvshow on genrelinktvshow.idShow = tvshowview.idShow", "genrelinktvshow.idGenre = "+row[0]);
				v.setParent(this);
				show(v);
			}
		}
	}
}
