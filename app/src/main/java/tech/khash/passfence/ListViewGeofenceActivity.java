package tech.khash.passfence;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListViewGeofenceActivity extends AppCompatActivity {

    private static final String TAG = ListViewGeofenceActivity.class.getSimpleName();

    private ArrayList<Fence> mFenceArrayList;
    private FenceListAdapter adapter;
    private RecyclerView recyclerView;

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
        adapter = new FenceListAdapter(this, mFenceArrayList);
        // Connect the adapter with the RecyclerView.
        recyclerView.setAdapter(adapter);
        // Give the RecyclerView a default layout manager.
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
            case R.id.action_delete_all_list:
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

    /*-------------------------HELPER METHODS ----------------------------------*/

    //helper method for removing all data
    private void deleteAllList() {
        //TODO: add a dialog for confirmation
        MainActivity.removeAllFences(this);
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


}//class
