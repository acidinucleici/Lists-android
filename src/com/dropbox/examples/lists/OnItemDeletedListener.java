package com.dropbox.examples.lists;

// Just a simple interface used by DeletableItemAdapter.
public interface OnItemDeletedListener<T> {
    public void onItemDeleted(T item);
}
