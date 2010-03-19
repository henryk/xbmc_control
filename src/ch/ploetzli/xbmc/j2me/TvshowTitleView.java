package ch.ploetzli.xbmc.j2me;

public class TvshowTitleView extends DatabaseView {
	public static TvshowTitleView get(String name) {
		DatabaseView view = get(name, "idShow", new String[]{"c00"}, "tvshowview", "c00");
		if(view instanceof TvshowTitleView) {
			return (TvshowTitleView)view;
		} else {
			/* Fallback because I know this is going to break; */
			return new TvshowTitleView(name, "idShow", new String[]{"c00"}, "tvshowview", "c00", null, null);
		}
	}

	public static TvshowTitleView get(String name, String whereClause) {
		TvshowTitleView view = get(name);
		view.whereClause = whereClause;
		return view;
	}
	
	private TvshowTitleView(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause, String whereClause)
	{
		super(name, keyRow, dataRows, table, orderClause, groupClause, whereClause);
	}

	/** Factory method to return newly made object. <b>Must</b> be overriden by all subclasses */
	protected static DatabaseView make(String name, String keyRow, String[] dataRows, String table, String orderClause, String groupClause, String whereClause)
	{
		System.out.println("Making TvshowTitleView");
		return new TvshowTitleView(name, keyRow, dataRows, table, orderClause, groupClause, whereClause);
	}
}
