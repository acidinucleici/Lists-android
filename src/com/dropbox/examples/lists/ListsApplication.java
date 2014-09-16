package com.dropbox.examples.lists;

import android.app.Application;
import com.dropbox.sync.android.DbxDatastoreManager;

// We're creating our own Application just to have a singleton off of which to hand the datastore manager.
public class ListsApplication extends Application {
    public DbxDatastoreManager datastoreManager;

    private static ListsApplication singleton;
    public static ListsApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
    }
}
