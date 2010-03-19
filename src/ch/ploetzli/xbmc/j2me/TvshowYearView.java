package ch.ploetzli.xbmc.j2me;

public class TvshowYearView extends DatabaseView {
	public TvshowYearView() {
		super("Year", null, new String[]{"substr(c05,0,5) as year"}, "tvshowview", "year", "year");
	}
}
