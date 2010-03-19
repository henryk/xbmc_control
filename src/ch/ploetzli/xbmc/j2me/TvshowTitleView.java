package ch.ploetzli.xbmc.j2me;

public class TvshowTitleView extends DatabaseView {
	/**
	 * Default constructor, necessary for construction through Class.getInstance
	 */
	public TvshowTitleView() { super(); }

	protected static DatabaseView get(String name) {
		return get(name, null);
	}
	
	protected static DatabaseView get(String name, String whereClause) {
		return get(TvshowTitleView.class, name, "idShow", new String[]{"c00"}, "tvshowview", "c00", null, whereClause);
	}
}
