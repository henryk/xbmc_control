package ch.ploetzli.xbmc.j2me;

public class MusicAlbumView extends MusicDatabaseView {
	public static DatabaseView get(String name) {
		return get(MusicAlbumView.class, name, "idAlbum", new String[]{"strAlbum", "strArtist"}, "albumview", "strAlbum,strArtist", "strAlbum");
	}
	
	protected Object[] formatRow(int index, String[] data) {
		if(data.length > 2) {
			return new Object[]{data[1] + " - " + data[2], null};
		}
		return super.formatRow(index, data);
	}
}
