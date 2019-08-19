package tech.khash.passfence;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Khashayar "Khash" Mortazavi
 *
 * Main adapter class to be used with RecyclerView in the MainActivity
 */


public class FenceListAdapter extends RecyclerView.Adapter<FenceListAdapter.FenceViewHolder> {

    //list of data
    private final ArrayList<Fence> fenceArrayList;
    //inflater used for creating the view
    private LayoutInflater inflater;
    //context
    private Context context;

    //This is our listener implemented as an interface, to be used in the Activity
    private ListItemLongClickListener itemLongClickListener;

    /**
     * The interface that receives onClick messages.
     */
    public interface ListItemLongClickListener {
        void onListItemLongClick(int clickedItemIndex);
    }//ListItemLongClickListener

    /**
     * Public constructor
     *
     * @param context        : context of the parent activity
     * @param fenceArrayList : ArrayList<Fence> containing data
     */
    public FenceListAdapter(Context context, ArrayList<Fence> fenceArrayList,
                            ListItemLongClickListener listener) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.fenceArrayList = fenceArrayList;
        itemLongClickListener = listener;
    }//constructor


    //It inflates the item layout, and returns a ViewHolder with the layout and the adapter.
    @NonNull
    @Override
    public FenceListAdapter.FenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View itemView = inflater.inflate(R.layout.list_item,
                parent, false);
        return new FenceViewHolder(itemView, this, context);
    }//onCreateViewHolder

    /**
     * This connects the data to the view holder. This is where it creates each item
     *
     * @param holder   : the custome view holder
     * @param position : index of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull FenceListAdapter.FenceViewHolder holder, int position) {
        //Get the corresponding Fence object
        Fence fence = fenceArrayList.get(position);
        //check for null fence
        if (fence == null) {
            return;
        }

        //extract data and set them
        //name
        holder.idTextView.setText(fence.getId());
        //active/expired
        String activeText = (fence.isActive()) ? context.getString(R.string.active) : context.getString(R.string.inactive);
        holder.activeTextView.setText(activeText);
        //expiry
        String detail = context.getString(R.string.expires_colon) + " " + fence.getExpiary() +
                 " - " + context.getString(R.string.criteria_colon) + " " + fence.getStringType();
        holder.detailTextView.setText(detail);
    }//onBindViewHolder

    @Override
    public int getItemCount() {
        if (fenceArrayList == null) {
            return 0;
        }
        return fenceArrayList.size();
    }//getItemCount


    //Inner class for the view holder
    class FenceViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        //our views
        final TextView idTextView, activeTextView, detailTextView;
        final FenceListAdapter fenceListAdapter;
        private Context context;

        //constructor
        private FenceViewHolder(View itemView, FenceListAdapter adapter, Context context) {
            super(itemView);
            this.context = context;
            //find view
            idTextView = itemView.findViewById(R.id.list_text_id);
            activeTextView = itemView.findViewById(R.id.list_text_active);
            detailTextView = itemView.findViewById(R.id.list_text_detail);
            //adapter
            this.fenceListAdapter = adapter;
            //for click listener
            itemView.setOnLongClickListener(this);
        }//FenceViewHolder

        @Override
        public boolean onLongClick(View v) {
            //get the index of the item
            int position = getLayoutPosition();
            itemLongClickListener.onListItemLongClick(position);
            //true if the callback consumed the long click, false otherwise.
            return true;
        }//onLongClick
    }//FenceViewHolder
}//FenceListAdapter-class


