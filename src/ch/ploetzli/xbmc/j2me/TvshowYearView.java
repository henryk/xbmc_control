package ch.ploetzli.xbmc.j2me;

public class TvshowYearView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(TvshowYearView.class, name, null, new String[]{"substr(c05,0,5) as year"}, "tvshowview", "year", "year");
	}
	
	protected void select(int index) {
		if(index >= 0 && index < cache.size()) {
			String[] row = (String[])cache.elementAt(index);
			if(row.length > 0) {
				DatabaseView v = TvshowTitleView.get("Year " + row[0], "substr(c05,0,5) = '"+row[0]+"'");
				v.setParent(this);
				show(v);
			}
		}
	}

}
