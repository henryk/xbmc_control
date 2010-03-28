package ch.ploetzli.xbmc.j2me.views;

public class MusicSongView extends MusicDatabaseView {
	public static DatabaseView get(String name, String whereClause) {
		return get(MusicSongView.class, name, "idSong", new String[]{"strTitle"}, "songview", "iTrack", null, whereClause);
	}
	

}
