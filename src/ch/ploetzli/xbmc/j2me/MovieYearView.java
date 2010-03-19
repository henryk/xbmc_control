package ch.ploetzli.xbmc.j2me;

public class MovieYearView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(MovieYearView.class, name, null, new String[]{"c07 as year"}, "movieview", "year", "year");
	}

	protected void select(String[] row) {
		if(row.length > 0) {
			DatabaseView v = MovieTitleView.get("Year " + row[0], "c07 = '"+row[0]+"'");
			v.setParent(this);
			show(v);
		}
	}
}
