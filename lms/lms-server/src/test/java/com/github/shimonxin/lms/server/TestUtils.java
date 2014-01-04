package com.github.shimonxin.lms.server;

import static org.junit.Assert.assertFalse;

import java.io.File;

public class TestUtils {
	public static void cleanStoreFiles() {
		File dbFile = new File("/mqtt_inflight.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		assertFalse(dbFile.exists());
		dbFile = new File("/mqtt_persist.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		assertFalse(dbFile.exists());
		dbFile = new File("/mqtt_retained.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		assertFalse(dbFile.exists());
		dbFile = new File("/mqtt_subscription.db");
		if (dbFile.exists()) {
			dbFile.delete();
		}
		assertFalse(dbFile.exists());
	}
}
