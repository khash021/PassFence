package com.khashayarmortazavi.testgeofence;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FenceArrayAdapter extends ArrayAdapter<Fence> {


    /**
     * This is our own custom constructor (it doesn't mirror a superclass constructor).
     * The context is used to inflate the layout file, and the list is the data we want
     * to populate into the lists.
     *
     * @param context        The current context. Used to inflate the layout file.
     * @param fenceArrayList An ArrayList of Fence objects to display in a list
     */
    public FenceArrayAdapter(Context context, ArrayList<Fence> fenceArrayList) {
        super (context, 0, fenceArrayList);
    }//FenceArrayAdapter


    /**
     * Provides a view for an AdapterView (ListView, GridView, etc.)
     *
     * @param position The position in the list of data that should be displayed in the
     *                 list item view.
     * @param convertView The recycled view to populate.
     * @param parent The parent ViewGroup that is used for inflation.
     * @return The View for the position in the AdapterView.
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.list_item, parent, false);
        }

        // Get the {@link Fence} object located at this position in the list
        Fence fence = getItem(position);

        // Find the TextViews in the xml layout with the ID magnitude
        TextView idTextView = (TextView) listItemView.findViewById(R.id.list_text_id);
        TextView activeTextView = (TextView) listItemView.findViewById(R.id.list_text_active);
        TextView detailTextView = (TextView) listItemView.findViewById(R.id.list_text_detail);

        idTextView.setText(fence.getId());

        String activeText = (fence.isActive()) ? "Active" : "Inactive";
        activeTextView.setText(activeText);

        String detail = "Expires: " + fence.getExpiary() + " - Criteria: " + fence.getStringType();
        detailTextView.setText(detail);

        return listItemView;
    }//getView

}//main
