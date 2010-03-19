package ch.ploetzli.xbmc.j2me;

public class TvshowTitleView extends DatabaseView {
	public TvshowTitleView(String name) {
		super(name, "idShow", new String[]{"c00"}, "tvshowview", "c00");
	}

	public TvshowTitleView(String name, String whereClause) {
		this(name);
		this.whereClause = whereClause;
	}
}
