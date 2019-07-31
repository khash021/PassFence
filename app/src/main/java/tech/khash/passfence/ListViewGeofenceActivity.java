package tech.khash.passfence;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListViewGeofenceActivity extends AppCompatActivity {

    //TODO: add FAB for adding new item from this list

    private ArrayList<Fence> mFenceArrayList;

    private FenceListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        //view containing the empty view
        LinearLayout emptyView = findViewById(R.id.empty_view);

        //get the arrayList, and set the empty view if the array is empty
        mFenceArrayList = MainActivity.loadArrayList(this);
        if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        } else {
            emptyView.setVisibility(View.GONE);
        }

        // Get a handle to the RecyclerView.
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }//onOptionsItemSelected

    /*-------------------------HELPER METHODS ----------------------------------*/

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
