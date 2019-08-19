package tech.khash.passfence;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;

public class ToastAdListener extends AdListener {

    private final String TAG = ToastAdListener.class.getSimpleName();

    private Context mContext;
    private String mErrorReason;

    //public constructor
    public ToastAdListener(Context context) {
        mContext = context;

    }//ToastAdListener

    @Override
    public void onAdLoaded() {
        super.onAdLoaded();
        Log.v(TAG, "onAdLoaded called");
    }//onAdLoaded

    @Override
    public void onAdOpened() {
        super.onAdOpened();
        Log.v(TAG, "onAdOpened called");

    }//onAdOpened

    @Override
    public void onAdFailedToLoad(int errorCode) {
        super.onAdFailedToLoad(errorCode);

        //initialize error message
        mErrorReason = "";
        switch (errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                //Something happened internally; for instance, an invalid response was received from the ad server.
                mErrorReason = "INTERNAL_ERROR : Something happened internally; for instance, an invalid response was received from the ad server.";
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                //The ad request was invalid; for instance, the ad unit ID was incorrect.
                mErrorReason = "INVALID_REQUEST : The ad request was invalid; for instance, the ad unit ID was incorrect.";
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                //The ad request was unsuccessful due to network connectivity.
                mErrorReason = "NETWORK_ERROR : The ad request was unsuccessful due to network connectivity.";
                break;
            case AdRequest.ERROR_CODE_NO_FILL:
                //The ad request was successful, but no ad was returned due to lack of ad inventory.
                mErrorReason = "NO_FILL : The ad request was successful, but no ad was returned due to lack of ad inventory.";
                break;
        }//switch
        Log.v(TAG, "onAdFailedToLoad called: " + mErrorReason );
        //the format, replaces %s with our message
        Toast.makeText(mContext,
                String.format("onAdFailedToLoad(%s)", mErrorReason), Toast.LENGTH_SHORT).show();

    }//onAdFailedToLoad

    public String getErrorReason () {

//        return mErrorReason == null ? "" : mErrorReason;
        //above code is the same as below

        if (mErrorReason == null) {
            return "";
        }
        return mErrorReason;
    }//getErrorReason

    @Override
    public void onAdLeftApplication() {
        super.onAdLeftApplication();
        Log.v(TAG, "onAdLeftApplication called");
    }//onAdLeftApplication

    @Override
    public void onAdClosed() {
        super.onAdClosed();
        Log.v(TAG, "onAdClosed called");
    }//onAdClosed


}//ToastListener
