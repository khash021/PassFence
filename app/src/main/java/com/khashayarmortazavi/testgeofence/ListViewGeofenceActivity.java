package com.khashayarmortazavi.testgeofence;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class ListViewGeofenceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view);

        //find teh ListView and set it up
        ListView listView = findViewById(R.id.list_view);

        //set an empty view
        View emptyView = findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        ArrayList<Fence> fenceArrayList = MainActivity.loadArrayList(this);
        if (fenceArrayList == null || fenceArrayList.size() < 1) {
            return;
        }

//        ArrayList<String> stringFenceList = getStringList(fenceArrayList);
//
//        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
//                R.layout.list_item_simple, R.id.list_text_simple);
//
//        arrayAdapter.addAll(stringFenceList);

        FenceArrayAdapter fenceArrayAdapter = new FenceArrayAdapter(this, new ArrayList<Fence>());

        fenceArrayAdapter.addAll(fenceArrayList);

        listView.setAdapter(fenceArrayAdapter);

//        listView.setAdapter(arrayAdapter);


        //set up on long click listener to delete the data point
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //id is the databse id
                //pass in the id to the confirmation dialog
                return false;
            }
        });//onLongClick

    }//onCreate

    private ArrayList<String> getStringList(ArrayList<Fence> inputArray) {
        ArrayList<String> outputArrayList = new ArrayList<>();
        for (Fence fence : inputArray) {
            String status;
            if (fence.isActive()) {
                status = "Status: Active";
            } else {
                status = "Status: Inactive";
            }//if
            String s = fence.getId() + "\n" + status + "\nExpires: " + fence.getExpiary() +
                    "\nCriteria: " + fence.getStringType();
            outputArrayList.add(s);
        }//for

        return outputArrayList;
    }//getStringList

}//class
