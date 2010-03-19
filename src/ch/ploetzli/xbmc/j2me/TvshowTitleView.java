package ch.ploetzli.xbmc.j2me;

public class TvshowTitleView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(name, null);
	}
	
	public static DatabaseView get(String name, String whereClause) {
		return get(name, "tvshowview", whereClause);
	}
	
	public static DatabaseView get(String name, String table, String whereClause) {
		return get(TvshowTitleView.class, name, "tvshowview.idShow", new String[]{"c00"}, table, "c00", null, whereClause);
	}
	
}
