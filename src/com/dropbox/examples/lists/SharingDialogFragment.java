package com.dropbox.examples.lists;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxPrincipal;

// This is the sharing dialog, which allows users to change permissions and send links to other users.
public class SharingDialogFragment extends DialogFragment {
    private DbxDatastore datastore;
    private View v;
    DbxDatastore.SyncStatusListener listener;

    public SharingDialogFragment(DbxDatastore datastore) {
        super();
        this.datastore = datastore;
    }

    // This is used in conjunction with the spinners to figure out what role was selected.
    private DbxDatastore.Role mapIndexToRole(int index) {
        switch (index) {
            case 1: return DbxDatastore.Role.VIEWER;
            case 2: return DbxDatastore.Role.EDITOR;
            default: return DbxDatastore.Role.NONE;
        }
    }

    // This is used in conjunction with the spinners to figure out what position to put
    // the spinner in based on the current role.
    private int mapRoleToIndex(DbxDatastore.Role role) {
        switch (role) {
            case VIEWER: return 1;
            case EDITOR: return 2;
            default: return 0;
        }
    }

    // Update the UI based on the current permissions.
    private void updateState() {
        Spinner publicRoleSpinner = (Spinner)v.findViewById(R.id.publicRoleSpinner);
        Spinner teamRoleSpinner = (Spinner)v.findViewById(R.id.teamRoleSpinner);

        publicRoleSpinner.setSelection(mapRoleToIndex(datastore.getRole(DbxPrincipal.PUBLIC)));
        teamRoleSpinner.setSelection(mapRoleToIndex(datastore.getRole(DbxPrincipal.TEAM)));

        // Only enable spinners if the datastore is writable.
        publicRoleSpinner.setEnabled(datastore.isWritable());
        teamRoleSpinner.setEnabled(datastore.isWritable());

        String team = ListsApplication.getInstance().datastoreManager.getAccount().getAccountInfo().orgName;

        // Make the team spinner visible only if the user is on a Dropbox for Business team.
        v.findViewById(R.id.teamRoleRow).setVisibility(team.isEmpty() ? View.INVISIBLE : View.VISIBLE);

        if (!team.isEmpty()) {
            // Put the team name in the label.
            TextView teamRoleLabel = (TextView)v.findViewById(R.id.teamRoleLabel);
            teamRoleLabel.setText("Team (" + team + ") role");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        v = inflater.inflate(R.layout.sharing, null);

        Spinner publicRoleSpinner = (Spinner)v.findViewById(R.id.publicRoleSpinner);
        Spinner teamRoleSpinner = (Spinner)v.findViewById(R.id.teamRoleSpinner);

        class RoleUpdater implements AdapterView.OnItemSelectedListener {
            DbxDatastore datastore;
            DbxPrincipal principal;

            public RoleUpdater(DbxDatastore datastore, DbxPrincipal principal) {
                this.datastore = datastore;
                this.principal = principal;
            }

            private void updateRole(DbxDatastore.Role role) {
                if (datastore.isWritable()) {
                    datastore.setRole(principal, role);
                    try {
                        datastore.sync();
                        updateState();
                    } catch (DbxException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateRole(mapIndexToRole(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateRole(DbxDatastore.Role.NONE);
            }
        }

        // When the spinner is changed, update the role to match.
        publicRoleSpinner.setOnItemSelectedListener(new RoleUpdater(datastore, DbxPrincipal.PUBLIC));
        teamRoleSpinner.setOnItemSelectedListener(new RoleUpdater(datastore, DbxPrincipal.TEAM));

        // Send email with a link.
        ((Button)v.findViewById(R.id.emailButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_SUBJECT, "I want to share a list with you.");
                i.putExtra(Intent.EXTRA_TEXT, "Here's the link: https://dslists.site44.com/#" + datastore.getId());
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Send a text message with a link.
        ((Button)v.findViewById(R.id.smsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setType("vnd.android-dir/mms-sms");
                i.putExtra("sms_body", "I want to share a list with you: https://dslists.site44.com/#" + datastore.getId());
                try {
                    startActivity(Intent.createChooser(i, "Send SMS..."));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), "There are no SMS clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up a listener to follow changes to the datastore (including permissions changes).
        listener = new DbxDatastore.SyncStatusListener() {
            @Override
            public void onDatastoreStatusChange(DbxDatastore dbxDatastore) {
                try {
                    dbxDatastore.sync();
                } catch (DbxException e) {
                    e.printStackTrace();
                }
                updateState();
            }
        };
        datastore.addSyncStatusListener(listener);
        updateState();

        return new AlertDialog.Builder(getActivity())
                .setTitle("Share")
                .setView(v)
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        datastore.removeSyncStatusListener(listener);
    }
}
