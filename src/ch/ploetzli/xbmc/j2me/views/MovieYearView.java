package ch.ploetzli.xbmc.j2me.views;

public class MovieYearView extends VideoDatabaseView {
	public static DatabaseView get(String name) {
		return get(MovieYearView.class, name, null, new String[]{"c07 as year"}, "movieview", "year", "year");
	}

	protected void select(String[] row) {
		if(row.length > 0) {
			DatabaseView v = MovieTitleView.get("Year " + row[0], "c07 = '"+row[0]+"'");
			showChild(v);
		}
	}
}
