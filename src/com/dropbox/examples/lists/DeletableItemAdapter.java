package com.dropbox.examples.lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

// A generic adapter that supports deleting elements, with a callback when items are deleted.
public abstract class DeletableItemAdapter<T> extends ArrayAdapter<T> {
    private List<T> items;
    private Context context;
    private OnItemDeletedListener<T> listener;
    private Boolean editable;

    public DeletableItemAdapter(List<T> items, Context ctx, Boolean editable) {
        super(ctx, R.layout.listitem, items);
        this.items = items;
        this.context = ctx;
        this.editable = editable;
    }

    protected abstract String getText(int position);

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listitem, parent, false);
        }

        TextView tv = (TextView) convertView.findViewById(R.id.text);
        tv.setText(this.getText(position));

        final T item = items.get(position);

        Button deleteButton = (Button) convertView.findViewById(R.id.deleteButton);

        if (editable) {
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) listener.onItemDeleted(item);
                }
            });
        } else {
            deleteButton.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    public void setOnItemDeleted(OnItemDeletedListener<T> listener) {
        this.listener = listener;
    }
}
