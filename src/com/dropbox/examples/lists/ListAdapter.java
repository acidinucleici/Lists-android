package com.dropbox.examples.lists;

import android.content.Context;
import com.dropbox.sync.android.DbxDatastoreInfo;

import java.util.List;

// An adapter for lists. Each item is backed by a datastore.
public class ListAdapter extends DeletableItemAdapter<DbxDatastoreInfo> {
    public ListAdapter(List<DbxDatastoreInfo> items, Context ctx) {
        super(items, ctx, true);
    }

    @Override
    protected String getText(int position) {
        DbxDatastoreInfo info = this.getItem(position);
        if (info.title != null) return info.title;
        else return info.id;
    }
}
