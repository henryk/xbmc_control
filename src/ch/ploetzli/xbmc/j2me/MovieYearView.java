package ch.ploetzli.xbmc.j2me;

public class MovieYearView extends DatabaseView {
	public MovieYearView(String name) {
		super(name, null, new String[]{"c07 as year"}, "movieview", "year", "year");
	}
}
