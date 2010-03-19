package ch.ploetzli.xbmc.j2me;

public class TvshowTitleView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(name, null);
	}
	
	public static DatabaseView get(String name, String whereClause) {
		return get(TvshowTitleView.class, name, "idShow", new String[]{"c00"}, "tvshowview", "c00", null, whereClause);
	}
}
