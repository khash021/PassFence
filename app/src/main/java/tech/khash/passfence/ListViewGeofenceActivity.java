package tech.khash.passfence;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListViewGeofenceActivity extends AppCompatActivity {

    //TODO: set the sort type in the app bar (by name, or expiry, ascending or descending), maybe by a contextual menu
    //TODO: add FAB for adding new item from this list

    private ArrayList<Fence> mFenceArrayList;
    private ListView mListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        //find teh ListView and set it up
        mListView = findViewById(R.id.list_view);

        //set an empty view
        View emptyView = findViewById(R.id.empty_view);
        mListView.setEmptyView(emptyView);

        mFenceArrayList = MainActivity.loadArrayList(this);
        if (mFenceArrayList == null || mFenceArrayList.size() < 1) {
            return;
        }

        //sort the array list based on their name ascending
//        sortNameAscending();
        sortNameDescending();

        FenceArrayAdapter fenceArrayAdapter = new FenceArrayAdapter(this, new ArrayList<Fence>());

        fenceArrayAdapter.addAll(mFenceArrayList);

        mListView.setAdapter(fenceArrayAdapter);

        //set up on long click listener to delete the data point
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "position = " + position + "\nid: " + id, Toast.LENGTH_SHORT).show();
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showOptionsDialog(mFenceArrayList.get(position));

                return false;
            }
        });



    }//onCreate

    //Helper method for sorting list based on their name (ascending)
    private void sortNameAscending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence o1, Fence o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
    }//sortNameAscending

    //Helper method for sorting list based on their name (ascending)
    private void sortNameDescending() {
        Collections.sort(mFenceArrayList, new Comparator<Fence>() {
            @Override
            public int compare(Fence o1, Fence o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
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
    }//sortExpiryAscending


    private void showOptionsDialog(final Fence fence) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(fence.getId());
        String[] list = {"Edit", "Delete", "Cancel"};
        dialogBuilder.setItems(list, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index) {
                switch (index) {
                    case 0:
                        //edit mode
                        Intent editIntent = new Intent(ListViewGeofenceActivity.this, AddGeofenceActivity.class);
                        editIntent.putExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT, fence.getId());
                        startActivity(editIntent);
                        break;
                    case 1:
                        //delete
                        mFenceArrayList.remove(index);
                        mListView.requestApplyInsets();
                        break;
                    case 2:
                        //cancel
                        break;
                }//switch
            }
        });
        dialogBuilder.create().show();
    }//showBadLocationDialog


}//class
