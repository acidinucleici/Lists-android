package com.dropbox.examples.lists;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import com.dropbox.sync.android.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// Our main activity, which displays a list of lists and allows a user to link or unlink a Dropbox account.
public class MainActivity extends Activity {
    DbxAccountManager accountManager;
    ListsApplication app;

    static final int REQUEST_LINK_TO_DBX = 0;
    Button linkUnlinkButton;
    EditText listInput;

    // Called when the user finishes the linking process.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) {
                DbxAccount account = accountManager.getLinkedAccount();
                try {
                    // Migrate any local datastores.
                    app.datastoreManager.migrateToAccount(account);
                    // Start using the remote datastore manager.
                    app.datastoreManager = DbxDatastoreManager.forAccount(account);
                    setUpListeners();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
                // Swap the button.
                linkUnlinkButton.setText("Unlink from Dropbox");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setUpListeners() {
        app.datastoreManager.addListListener(new DbxDatastoreManager.ListListener() {
            @Override
            public void onDatastoreListChange(DbxDatastoreManager dbxDatastoreManager) {
                // Update the UI when the list of datastores changes.
                MainActivity.this.updateList();
            }
        });
        updateList();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.app = ListsApplication.getInstance();

        setContentView(R.layout.main);

        // Set up the text input that allows adding a new list.
        listInput = new EditText(this);
        listInput.setSingleLine();
        listInput.setHint("Create a new list...");
        listInput.setSingleLine();
        listInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // If the enter key is pressed...
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER && listInput.getText().length() > 0) {
                    try {
                        // Create a new datastore and set its title.
                        DbxDatastore datastore = app.datastoreManager.createDatastore();
                        datastore.setTitle(listInput.getText().toString());

                        // Sync (to send the title change).
                        datastore.sync();

                        // Close the datastore. (It will be opened again if the user taps on that list.)
                        datastore.close();
                        listInput.setText("");
                    } catch (DbxException e) {
                        e.printStackTrace();
                    }
                    return true;
                }

                // We'll also get a key up event.
                if (event.getAction() == KeyEvent.ACTION_UP &&
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    listInput.setText("");
                    listInput.requestFocus();
                    return true;
                }
                return false;
            }
        });
        ((ListView)findViewById(R.id.listView)).addFooterView(listInput);

        linkUnlinkButton = (Button) findViewById(R.id.linkUnlinkButton);

        // Get the account manager for our app (using our app key and secret).
        accountManager = DbxAccountManager.getInstance(getApplicationContext(), "gmd9bz0ihf8t30o", "gt6onalc86cbetu");

        if (accountManager.hasLinkedAccount()) {
            // If there's a linked account, use that.
            try {
                app.datastoreManager = DbxDatastoreManager.forAccount(accountManager.getLinkedAccount());
                linkUnlinkButton.setText("Unlink from Dropbox");
            } catch (DbxException.Unauthorized unauthorized) {
                unauthorized.printStackTrace();
            }
        } else {
            // Otherwise, use a local datastore manager.
            app.datastoreManager = DbxDatastoreManager.localManager(accountManager);
        }

        setUpListeners();

        final MainActivity activity = this;
        linkUnlinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!accountManager.hasLinkedAccount()) {
                    // If we're not already linked, start the linking process.
                    accountManager.startLink(activity, REQUEST_LINK_TO_DBX);
                } else {
                    // If we're linked, unlink and start using a local datastore manager again.
                    accountManager.unlink();
                    app.datastoreManager = DbxDatastoreManager.localManager(accountManager);
                    linkUnlinkButton.setText("Link with Dropbox");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
                // When a list is tapped, open the ListActivity, passing in the right datastore ID.
                Intent intent = new Intent(MainActivity.this, ListActivity.class);
                intent.putExtra("com.dropbox.examples.lists.DSID",
                        ((DbxDatastoreInfo)adapter.getItemAtPosition(position)).id);
                startActivity(intent);
            }
        });
        updateList();
    }

    // Update the UI based on the current list of datastores.
    private void updateList() {
        ListView listView = (ListView) findViewById(R.id.listView);
        ArrayList<DbxDatastoreInfo> infos = new ArrayList<DbxDatastoreInfo>();
        try {
            infos.addAll(app.datastoreManager.listDatastores());
        } catch (DbxException e) {
            e.printStackTrace();
        }
        // Sort by the modified time.
        Collections.sort(infos,
            new Comparator<DbxDatastoreInfo>() {
                @Override
                public int compare(DbxDatastoreInfo a, DbxDatastoreInfo b) {
                    if (a.mtime != null && b.mtime != null) {
                        return a.mtime.compareTo(b.mtime);
                    } else {
                        return a.id.compareTo(b.id);
                    }
                }
            });
        ListAdapter adapter = new ListAdapter(infos, this);
        // Set the handler for when a list should be deleted.
        adapter.setOnItemDeleted(new OnItemDeletedListener<DbxDatastoreInfo>() {
            @Override
            public void onItemDeleted(DbxDatastoreInfo item) {
                try {
                    // Delete the datastore.
                    app.datastoreManager.deleteDatastore(item.id);
                } catch (DbxException e) {
                    e.printStackTrace();
                }
            }
        });
        listView.setAdapter(adapter);
        listInput.requestFocus();
    }
}
