package ch.ploetzli.xbmc.j2me.views;

public class MusicAlbumView extends MusicDatabaseView {
	public static DatabaseView get(String name) {
		return get(name, null);
	}
	
	public static DatabaseView get(String name, String whereClause) {
		return get(MusicAlbumView.class, name, "idAlbum", new String[]{"strAlbum", "strArtist"}, "albumview", "strAlbum,strArtist", "strAlbum", whereClause);
	}
	
	protected Object[] formatRow(int index, String[] data) {
		if(data.length > 2 && (whereClause == null || whereClause.indexOf("idArtist") == -1)) {
			return new Object[]{data[1] + " - " + data[2], null};
		}
		return super.formatRow(index, data);
	}
}
