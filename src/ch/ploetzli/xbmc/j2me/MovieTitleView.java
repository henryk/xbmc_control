package ch.ploetzli.xbmc.j2me;

public class MovieTitleView extends DatabaseView {
	public MovieTitleView(String name) {
		super(name, "idMovie", new String[]{"c00"}, "movieview", "c00");
	}
}
