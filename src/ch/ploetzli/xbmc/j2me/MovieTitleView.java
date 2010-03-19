package ch.ploetzli.xbmc.j2me;

public class MovieTitleView extends DatabaseView {
	public static DatabaseView get(String name) {
		return get(MovieTitleView.class, name, "idMovie", new String[]{"c00"}, "movieview", "c00");
	}
}
