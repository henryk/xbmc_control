package ch.ploetzli.xbmc.j2me;

public class MovieTitleView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(name, null);
	}

	public static DatabaseView get(String name, String whereClause) {
		return get(name, "movieview", whereClause);
	}

	public static DatabaseView get(String name, String table, String whereClause) {
		return get(MovieTitleView.class, name, "movieview.idMovie", new String[]{"c00"}, table, "c00", null, whereClause);
	}
}
