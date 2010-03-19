package ch.ploetzli.xbmc.j2me;

public class TvshowTitleView extends DatabaseView {
	private static final String orderClause = "c00";
	private static final String table = "tvshowview";
	private static final String[] dataColumns = new String[]{orderClause};
	private static final String keyColumn = "idShow";
	
	/**
	 * Default constructor, necessary for construction through Class.getInstance
	 */
	public TvshowTitleView() { super(); }

	public TvshowTitleView(String name) {
		super(name, keyColumn, dataColumns, table, orderClause);
	}

	public TvshowTitleView(String name, String whereClause) {
		this(name);
		this.whereClause = whereClause;
	}

	protected static DatabaseView get(String name, String whereClause) {
		try {
			return get(Class.forName("ch.ploetzli.xbmc.j2me.TvshowTitleView"), name, keyColumn, dataColumns, table, orderClause, null, whereClause);
		} catch (ClassNotFoundException e) {
			/* Might as well crash and burn */
		}
		return null;
	}
}
