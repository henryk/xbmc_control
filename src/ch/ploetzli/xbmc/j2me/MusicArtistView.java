package ch.ploetzli.xbmc.j2me;

public class MusicArtistView extends MusicDatabaseView {
	public static DatabaseView get(String name) {
		return get(MusicArtistView.class, name, "artist.idArtist", new String[]{"strArtist"}, "artist", "strArtist", "strArtist");
	}

}
