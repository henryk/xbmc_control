package ch.ploetzli.xbmc.j2me.views;

import java.io.IOException;

import ch.ploetzli.xbmc.api.HttpApi;
import ch.ploetzli.xbmc.api.RecordSetConnection;

public class MusicDatabaseView extends DatabaseView {

	protected RecordSetConnection queryDatabase(HttpApi api, String query) throws IOException {
		return api.queryMusicDatabase(query);
	}

}
