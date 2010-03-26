package ch.ploetzli.xbmc.j2me.views;

public class TvshowYearView extends VideoDatabaseView {
	public static DatabaseView get(String name) {
		return get(TvshowYearView.class, name, null, new String[]{"substr(c05,0,5) as year"}, "tvshowview", "year", "year");
	}
	
	protected void select(String[] row) {
		if(row.length > 0) {
			DatabaseView v = TvshowTitleView.get("Year " + row[0], "substr(c05,0,5) = '"+row[0]+"'");
			showChild(v);
		}
	}
}
