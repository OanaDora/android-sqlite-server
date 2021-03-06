package org.devtcg.sqliteserver.sample;

import android.database.sqlite.SQLiteDatabase;
import org.devtcg.sqliteserver.SQLiteServiceServer;

public class TestService extends SQLiteServiceServer {
    private static final String DB_NAME = "test_server.db";
    private static final int DB_VERSION = 1;

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return new MyOpenHelper(this, DB_NAME, DB_VERSION).getWritableDatabase();
    }

    @Override
    public String getServerName() {
        return getClass().getName();
    }
}
