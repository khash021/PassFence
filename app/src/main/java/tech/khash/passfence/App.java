package tech.khash.passfence;

import android.app.Application;
import android.content.Context;

/**
 * Created by Khashayar "Khash" Mortazavi
 * <p>
 * This is a class to get the context from anywhere especially for getting resources from static methods,
 * mainly used in Fence class for now
 * <p>
 * We need to set the android:name in the Application tag in manifest to point to this
 * then we can call App.getContext anywhere in the app
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }//onCreate

    //we call this static method to get the context
    public static Context getContext() {
        return mContext;
    }
}//APP
