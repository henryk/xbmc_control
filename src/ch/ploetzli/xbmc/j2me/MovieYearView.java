package ch.ploetzli.xbmc.j2me;

public class MovieYearView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(MovieYearView.class, name, null, new String[]{"c07 as year"}, "movieview", "year", "year");
	}
}
