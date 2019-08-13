package tech.khash.passfence;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListViewGeofenceActivity extends AppCompatActivity implements
        FenceListAdapter.ListItemLongClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = ListViewGeofenceActivity.class.getSimpleName();

    private ArrayList<Fence> mFenceArrayList;
    private FenceListAdapter adapter;
    private RecyclerView recyclerView;

    //For geofences
    protected GoogleApiClient mGoogleApiClient;
    private GeofencingClient mGeofencingClient;

    //TODO: update, notify change does not work!!!!!!!!!
    //TODO: either get the list to update properly, use startActivityForResults, send back to Main, or just simply recrete

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        //view containing the empty view
        LinearLayout emptyView = findViewById(R.id.empty_view);

        //find the fab and set it up
        FloatingActionButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addIntent = new Intent(getApplicationContext(), AddGeofenceActivity.class);
                //TODO: change this to activity for results
                startActivity(addIntent);
            }
        });

        //get the arrayList, and set the empty view if the array is empty
        mFenceArrayList = MainActivity.loadArrayList(this);
        if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyView.setVisibility(View.GONE);
        }

        // Get a handle to the RecyclerView.
        recyclerView = findViewById(R.id.recycler_view);
        // Create an adapter and supply the data to be displayed.
        adapter = new FenceListAdapter(this, mFenceArrayList, this);
        // Connect the adapter with the RecyclerView.
        recyclerView.setAdapter(adapter);
        // Give the RecyclerView a default layout manager.
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        //create an instance of the Geofencing client to access the location APIs
        mGeofencingClient = LocationServices.getGeofencingClient(this);

    }//onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //get the inflater
        MenuInflater inflater = getMenuInflater();

        //inflate the menu
        inflater.inflate(R.menu.menu_list, menu);

        //You must return true for the menu to be displayed; if you return false it will not be shown.
        return true;
    }//onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                recreate();
                return true;
            case R.id.action_sort_name_ascending:
                sortNameAscending();
                return true;
            case R.id.action_sort_name_descending:
                sortNameDescending();
                return true;
            case R.id.action_sort_expiry_ascending:
                sortExpiryAscending();
                return true;
            case R.id.action_sort_expiry_descending:
                sortExpiryDescending();
                return true;
            case R.id.action_delete_all:
                //TODO: check to make sure the list is not empty
                if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
                    Toast.makeText(getApplicationContext(), "No registered geofence", Toast.LENGTH_SHORT).show();
                    return true;
                }
                deleteAllList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }//onOptionsItemSelected

    protected synchronized void buildGoogleApiClient() {
        Log.v(TAG, "buildGoogleApiClient called");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }//buildGoogleApiClient

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }//onConnected

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }//onConnectionSuspended

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }//onConnectionFailed

    /*-------------------------HELPER METHODS ----------------------------------*/

    //helper method for removing all data
    private void deleteAllList() {
        //show a dialog for confirmation
        showDeleteAllDialog();
    }//deleteAllList

    //Helper method for sorting list based on their name (ascending)
    private void sortNameAscending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence o1, Fence o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        //notify the adapter that the data has changed, and it should update
        adapter.notifyDataSetChanged();
    }//sortNameAscending

    //Helper method for sorting list based on their name (ascending)
    private void sortNameDescending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence o1, Fence o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
        //notify the adapter that the data has changed, and it should update
        adapter.notifyDataSetChanged();
    }//sortNameAscending

    ////Helper method for sorting list based on their expiray (ascending)
    private void sortExpiryAscending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence f1, Fence f2) {
                Long t1 = f1.getExpiaryTimeMilliSec();
                Long t2 = f2.getExpiaryTimeMilliSec();

                return t1.compareTo(t2);
            }
        });
        //notify the adapter that the data has changed, and it should update
        adapter.notifyDataSetChanged();
    }//sortExpiryAscending

    ////Helper method for sorting list based on their expiray (descending)
    private void sortExpiryDescending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence f1, Fence f2) {
                Long t1 = f1.getExpiaryTimeMilliSec();
                Long t2 = f2.getExpiaryTimeMilliSec();

                return t2.compareTo(t1);
            }
        });
        //notify the adapter that the data has changed, and it should update
        adapter.notifyDataSetChanged();
    }//sortExpiryAscending

    //Helper method for showing the dialog for erasing all data
    private void showDeleteAllDialog() {

        //create the builder
        AlertDialog.Builder builder =new AlertDialog.Builder(this);

        //add message and button functionality
        builder.setMessage(R.string.delete_all_dialog_msg)
                .setPositiveButton(R.string.delete_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Delete all
                        MainActivity.removeAllFences(getApplicationContext());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close the dialog
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }//showUnsavedChangesDialog

    @Override
    public void onListItemLongClick(int clickedItemIndex) {
        //get the corresponding fence object
        Fence fence = mFenceArrayList.get(clickedItemIndex);
        //show a dialog
        showLongClickDialog(fence);
    }//onListItemLongClick

    //helper method for showing the dialog
    private void showLongClickDialog(final Fence fence) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(fence.getId());
        String[] list = {"Edit", "Delete", "Cancel"};
        dialogBuilder.setItems(list, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                switch (index) {
                    case 0:
                        //edit mode
                        //TODO: change to activity for results
                        Intent editIntent = new Intent(getApplicationContext(), AddGeofenceActivity.class);
                        editIntent.putExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT, fence.getId());
                        startActivity(editIntent);
                        break;
                    case 1:
                        //delete
                        showDeleteConfirmationDialog(fence);
                        break;
                    case 2:
                        //cancel
                        dialog.dismiss();
                        break;
                }//switch
            }
        });
        dialogBuilder.create().show();
    }//showBadLocationDialog

    //helper method for delete confirmation
    private void showDeleteConfirmationDialog(final Fence fence) {
        //get the id
        String fenceId = fence.getId();
        //create the builder
        AlertDialog.Builder builder =new AlertDialog.Builder(this);

        String message = getString(R.string.dialog_delete_fence) + " \"" + fenceId + "\"" +
                getString(R.string.question_mark);

        //add message and button functionality
        builder.setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //delete fence
                        deleteGeofence(fence);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //close the dialog
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }//showDeleteConfirmationDialog

    //helper method for deleting the geofence
    private void deleteGeofence(Fence targetFence) {
        //get the id of the fence
        String fenceId = targetFence.getId();
        //retrieve the array list of Fences
        mFenceArrayList = MainActivity.loadArrayList(this);

        //find the Fence object we want to edit
        Fence fence = null;
        int fenceIndexInArray = -1;
        for (Fence f : mFenceArrayList) {
            if (f.getId().equalsIgnoreCase(fenceId)) {
                fence = f;
                //get the index of our Fence object
                fenceIndexInArray = mFenceArrayList.indexOf(f);
                break;
            }//if
        }//for

        //check for null or -1 index
        if (fence == null | fenceIndexInArray == -1) {
            Log.wtf(TAG, "Error in locating the fence object");
            return;
        }

        //remove geofence
        //remove method takes a list, so create a list with the fenceId to be removed
        List<String> oldFenceIdList = new ArrayList<String>();
        oldFenceIdList.add(fenceId);

        mGeofencingClient.removeGeofences(oldFenceIdList).addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.v(TAG, "Geofence removed successfully");
                Toast.makeText(getApplicationContext(), getString(R.string.geofence_removed), Toast.LENGTH_SHORT).show();
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.v(TAG, "Geofence removal error");
                        Toast.makeText(getApplicationContext(), getString(R.string.error_remove_geofence), Toast.LENGTH_SHORT).show();
                    }
                });

        //remove the geofence from the main arraylist (return true if it was removed)
        boolean removeSuccess = mFenceArrayList.remove(fence);

        //update the main arraylist
        if (removeSuccess) {
            //update app's arrayList
            MainActivity.updateArrayList(this, mFenceArrayList);
        }

        //recreate activity
        recreate();
    }//deleteGeofence

}//class
