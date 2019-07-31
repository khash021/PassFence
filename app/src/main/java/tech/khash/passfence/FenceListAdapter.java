package tech.khash.passfence;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class FenceListAdapter extends RecyclerView.Adapter<FenceListAdapter.FenceViewHolder> {

    //list of data
    private final ArrayList<Fence> fenceArrayList;
    //inflator used for creating the view
    private LayoutInflater inflater;

    private Context context;

    //constructor
    public FenceListAdapter(Context context, ArrayList<Fence> fenceArrayList) {
        this.context = context;
        inflater =LayoutInflater.from(context);
        this.fenceArrayList = fenceArrayList;
    }//constructor


    @NonNull
    @Override
    public FenceListAdapter.FenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemView = inflater.inflate(R.layout.list_item,
                parent, false);
        return new FenceViewHolder(itemView, this, context);
    }//onCreateViewHolder

    @Override
    public void onBindViewHolder(@NonNull FenceListAdapter.FenceViewHolder holder, int position) {
        //Get the corresponding Fence object
        Fence fence =fenceArrayList.get(position);

        //extract data and set them
        holder.idTextView.setText(fence.getId());

        String activeText = (fence.isActive()) ? "Active" : "Inactive";
        holder.activeTextView.setText(activeText);

        String detail = "Expires: " + fence.getExpiary() + " - Criteria: " + fence.getStringType();
        holder.detailTextView.setText(detail);

        holder.activeCheckBox.setChecked(fence.isActive());

    }//onBindViewHolder

    @Override
    public int getItemCount() {
        return fenceArrayList.size();
    }//getItemCount


    //Inner class for the view holder
    class FenceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        //our views
        public final TextView idTextView, activeTextView, detailTextView;
        public final CheckBox activeCheckBox;
        final FenceListAdapter fenceListAdapter;
        private Context context;

        //constructor
        public FenceViewHolder(View itemView, FenceListAdapter adapter, Context context) {
            super(itemView);
            this.context = context;
            //find view
            idTextView = itemView.findViewById(R.id.list_text_id);
            activeTextView = itemView.findViewById(R.id.list_text_active);
            detailTextView = itemView.findViewById(R.id.list_text_detail);
            activeCheckBox = itemView.findViewById(R.id.list_box_active);
            //adapter
            this.fenceListAdapter = adapter;
            //for click listener
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

        }//FenceViewHolder

        @Override
        public void onClick(View v) {
            // Get the position of the item that was clicked.
            int position = getLayoutPosition();
            //get the corresponding fence object
            Fence element = fenceArrayList.get(position);
            //show a toast for now
            Toast.makeText(context, element.getId() + " : clicked", Toast.LENGTH_SHORT).show();

        }//onClick

        @Override
        public boolean onLongClick(View v) {
            // Get the position of the item that was clicked.
            int position = getLayoutPosition();
            //get the corresponding fence object
            Fence element = fenceArrayList.get(position);

            //show a dialog
            showOptionsDialog(element);
            //true if the callback consumed the long click, false otherwise.
            return true;
        }//onLongClick

        //helper method for showing the dialog
        private void showOptionsDialog(final Fence fence) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle(fence.getId());
            String[] list = {"Edit", "Delete", "Cancel"};
            dialogBuilder.setItems(list, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index) {
                    switch (index) {
                        case 0:
                            //edit mode
                            Intent editIntent = new Intent(context, AddGeofenceActivity.class);
                            editIntent.putExtra(MainActivity.FENCE_EDIT_EXTRA_INTENT, fence.getId());
                            context.startActivity(editIntent);
                            break;
                        case 1:
                            //delete
                            Toast.makeText(context, "DELETE", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            //cancel
                            Toast.makeText(context, "CANCEL", Toast.LENGTH_SHORT).show();
                            break;
                    }//switch
                }
            });
            dialogBuilder.create().show();
        }//showBadLocationDialog

    }//FenceViewHolder-class

}//FenceListAdapter-class