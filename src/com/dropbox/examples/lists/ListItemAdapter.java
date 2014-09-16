package com.dropbox.examples.lists;

import android.content.Context;
import com.dropbox.sync.android.DbxRecord;

import java.util.List;

// An adapter for items within a list. Each item is backed by a record in the list's datastore.
public class ListItemAdapter extends DeletableItemAdapter<DbxRecord> {
    public ListItemAdapter(List<DbxRecord> items, Context ctx, Boolean editable) {
        super(items, ctx, editable);
    }

    @Override
    protected String getText(int position) {
       return this.getItem(position).getString("text");
    }
}
