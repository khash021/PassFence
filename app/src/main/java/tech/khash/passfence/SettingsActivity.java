package tech.khash.passfence;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

/**
 * Created by Khashayar "Khash" Mortazavi
 * Main class responsible for the settings. As of now, it only controls the notification settings.
 * It uses a PreferenceFragmentCompact for devices API 25 and lower. For API 26 and above, all these
 * notification settings are controlled by the channel and can be changed in the system settings,
 * as a result, it will only show the set values for those Oreo and higher devices
 */

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    static final String PACKAGE_NAME = "tech.khash.passfence";

    //for activity results
    private static final int REQUEST_CODE_ALERT_RINGTONE = 1;

    SharedPreferences sharedPreferences;
    String currentRing;

    //for tracking selected color change to update UI
    private boolean needsUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //get a reference to shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //check the version here and just show the different view for only viewing the set preferences
        if (isApi26Above()) {
            setContentView(R.layout.activity_settings_oreo);
            setupOreoVersion();
        } else {
            setContentView(R.layout.activity_settings);

            //set the current color
            setSelectedColor();
            Button buttonColorPicker = findViewById(R.id.button_color_picker);
            buttonColorPicker.setOnClickListener(this);

            boolean ledBoolean = sharedPreferences.getBoolean(MainActivity.LED_PREF_KEY, true);
            buttonColorPicker.setActivated(ledBoolean);
        }//if - below oreo

        currentRing = sharedPreferences.getString(MainActivity.RINGTONE_PREF_KEY, "");
    }//onCreate

    @Override
    protected void onResume() {
        super.onResume();
        //check here to see if we are coming back from the settings, if so recreate to update the list
        if (needsUpdate) {
            setupOreoVersion();
        }
    }//onResume

    /* Since this activity hosts the fragment that started the startActivityForResults, this gets
        the results first and we get the selected ringtone from it here. There is no need to pass
        it to the fragment, we handle the results here
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.v(TAG, "onActivityResult called" +"\nRequest Code: " + requestCode + "\nResult Code: " + resultCode);
        //TODO: gotta be a better way than recreating
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtone != null) {
                String ringString = ringtone.toString();
                sharedPreferences.edit().putString(MainActivity.RINGTONE_PREF_KEY, ringString).apply();
                //recreate to apply changes
                recreate();
            } else {
                // "Silent" was selected
                sharedPreferences.edit().putString(MainActivity.RINGTONE_PREF_KEY, "").apply();
                //recreate to apply changes
                recreate();
            }
        }
    }//onActivityResult

    @Override
    public void onClick(View v) {
        //if any of the oreo views are clicked, we show them the explanation dialog
        int id = v.getId();
        switch (id) {
            //if any of the oreo views are clicked, we show them the explanation dialog
            case R.id.text_ringtone_summary:
            case R.id.text_ringtone_header:
            case R.id.text_priority_header:
            case R.id.text_priority_summary:
            case R.id.switch_vibrate:
            case R.id.switch_led:
                showExplanationDialog(v.getContext());
                break;

            //show the color picker
            case R.id.button_color_picker:
                //get the current color and start the picker
                setupAndShowColorPicker();
                break;

            //go to notification channel settings
            case R.id.button_notification:
                //open app notification setting
                openNotificationChannelSettings();
                break;

        }//switch
    }//onClick


    public static class PreferencesFragment extends PreferenceFragmentCompat implements
            Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

        private Context context;
        private SharedPreferences sharedPreferences;

        @Override
        public void onCreatePreferences(Bundle bundle, String rootKey) {
            Log.v(TAG, "onCreatePreferences called");
            setPreferencesFromResource(R.xml.settings, rootKey);

            context = getActivity().getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            Preference preferenceRing = findPreference(MainActivity.RINGTONE_PREF_KEY);
            Preference preferencePriority = findPreference(MainActivity.PRIORITY_PREF_KEY);

            preferencePriority.setOnPreferenceClickListener(this);
            preferenceRing.setOnPreferenceClickListener(this);

            bindPreferenceSummaryToValue(preferencePriority);
            bindRingtoneSummaryToValue(preferenceRing);
        }//onCreatePreferences

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();
            if (preference instanceof ListPreference) {
                //cast to list preference
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(stringValue);
                if (prefIndex >= 0) {
                    CharSequence[] labels = listPreference.getEntries();
                    preference.setSummary(labels[prefIndex]);
                }
            } else {
                preference.setSummary(stringValue);
            }
            return false;
        }//onPreferenceChange

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Log.d(TAG, preference.getKey());

            //return if this is oreo or above, since they dont have actual preference objects here
            if (isApi26Above()) {
                return true;
            } else {
                //other devices. check for ringtone and show the picker
                if (preference.getKey().equals(MainActivity.RINGTONE_PREF_KEY)) {
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    //Given to the ringtone picker as a boolean. Whether to show an item for "Silent".
                    // If the "Silent" item is picked, EXTRA_RINGTONE_PICKED_URI will be null.
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

                    String existingValue = sharedPreferences.getString(MainActivity.RINGTONE_PREF_KEY, "");
                    if (existingValue != null) {
                        if (existingValue.length() == 0) {
                            // Select "Silent"
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                        } else {
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                        }
                    } else {
                        // No ringtone has been selected, set to the default
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
                    }

                    getActivity().startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
                    return true;
                } else {
                    return super.onPreferenceTreeClick(preference);
                }
            }
        }//onPreferenceClick

        private void bindPreferenceSummaryToValue(Preference preference) {
            //set preference change listener on the preference and set the callbacks to this class
            preference.setOnPreferenceChangeListener(this);
            String preferenceString = sharedPreferences.getString(preference.getKey(), "");
            if (preferenceString.equals("")) {
                preferenceString = "Silent";
            }
            onPreferenceChange(preference, preferenceString);
        }//bindPreferenceSummaryToValue

        private void bindRingtoneSummaryToValue(Preference preference) {
            //get the ringtone value
            String currentRingUriString = sharedPreferences.getString(MainActivity.RINGTONE_PREF_KEY, "");
            //create a uri
            Uri currentRingUri = Uri.parse(currentRingUriString);
            //create ringtone using that uri
            Ringtone currentRingtone = RingtoneManager.getRingtone(context, currentRingUri);
            //get the name
            String currentRingName = currentRingtone.getTitle(context);
            onPreferenceChange(preference, currentRingName);
        }//bindRingtoneSummaryToValue

    }//PreferencesFragment

    //the call getNotificationChannel requires API 26, but we have already done that in onCreate, so we
    //added SuppressLint for that reason
    @SuppressLint("NewApi")
    private void setupOreoVersion() {
        Button notificationButton = findViewById(R.id.button_notification);
        notificationButton.setOnClickListener(this);

        //find views
        TextView textRingtoneHeader = findViewById(R.id.text_ringtone_header);
        TextView textRingtone = findViewById(R.id.text_ringtone_summary);
        TextView textPriorityHeader = findViewById(R.id.text_priority_header);
        TextView textPriority = findViewById(R.id.text_priority_summary);
        Switch switchVibrate = findViewById(R.id.switch_vibrate);
        Switch switchLed = findViewById(R.id.switch_led);
        textRingtone.setOnClickListener(this);
        textPriorityHeader.setOnClickListener(this);
        textRingtoneHeader.setOnClickListener(this);
        textPriority.setOnClickListener(this);
        switchVibrate.setOnClickListener(this);
        switchLed.setOnClickListener(this);

        //get the values from notification channel
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel myChannel = manager.getNotificationChannel(MainActivity.CHANNEL_ID);

        boolean vibrate = myChannel.shouldVibrate();
        boolean light = myChannel.shouldShowLights();
        int importance = myChannel.getImportance();
        String priority = getImportanceString(importance);
        Uri ringUri = myChannel.getSound();
        Ringtone ring = RingtoneManager.getRingtone(this, ringUri);
        String ringtone = ring.getTitle(this);


        //set the values
        textRingtone.setText(ringtone);
        textPriority.setText(priority);
        switchLed.setChecked(light);
        switchVibrate.setChecked(vibrate);

    }//setupOreoVersion

    //helper method for getting the current color and starting the color picker using that
    private void setupAndShowColorPicker() {
        int colorInt = sharedPreferences.getInt(MainActivity.COLOR_PICKER_PREF_KEY, -1);
        //set the default as blue
        int initialColor = getResources().getColor(R.color.blue_argb);
        Log.v(TAG, "resource color blue: " + initialColor);
        Log.v(TAG, "Initial color: " + initialColor);
        if (colorInt != -1) {
            initialColor = colorInt;
        }
        showColorPickerDialog(SettingsActivity.this, initialColor);
    }//setupAndShowColorPicker

    //helper method for sending the user to the phone's notification channel settings
    private void openNotificationChannelSettings() {
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra("android.provider.extra.APP_PACKAGE", PACKAGE_NAME);
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, MainActivity.CHANNEL_ID);
        //set the boolean to true, so we can update when we come back
        needsUpdate = true;
        startActivity(intent);
    }//openNotificationChannelSettings

    //shows the color picker dialog
    private void showColorPickerDialog(Context context, int initialColor) {
        ColorPickerDialogBuilder
                .with(context)
                .setTitle("Choose color")
                .initialColor(initialColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        //convert selected color to hex string and add to preference
                        //add to preferences
                        sharedPreferences.edit().putInt(MainActivity.COLOR_PICKER_PREF_KEY, selectedColor).apply();
                        //call the method to show the selected color
                        setSelectedColor();
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();
    }//showColorPickerDialog

    //helper method for setting the current led color for API 25 and below
    private void setSelectedColor() {
        TextView colorText = findViewById(R.id.text_color);
        //get the color and pass it in as the initial/current color
        int colorInt = sharedPreferences.getInt(MainActivity.COLOR_PICKER_PREF_KEY, -1);

        if (colorInt != -1) {
            String colorStringHex = Integer.toHexString(colorInt);
            int initialColor = Color.parseColor("#" + colorStringHex);
            colorText.setBackgroundColor(initialColor);
        }
    }//setSelectedColor

    //Helper method for checking if the device is running API 26 (Oreo) or higher
    private static boolean isApi26Above() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }//isApi26Above

    @SuppressLint("InlinedApi")
    private static void showExplanationDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.settings_explanation_dialog_title))
                .setMessage(context.getString(R.string.settings_explanation_dialog_body))
                .setPositiveButton(context.getString(R.string.got_to_settings), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //open app notification setting
                        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                        intent.putExtra("android.provider.extra.APP_PACKAGE", PACKAGE_NAME);
                        intent.putExtra(Settings.EXTRA_CHANNEL_ID, MainActivity.CHANNEL_ID);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }//showExplanationDialog

    private String getImportanceString(int importance) {
        switch (importance) {
            case NotificationManager.IMPORTANCE_UNSPECIFIED:
                return "Unspecified";
            case NotificationManager.IMPORTANCE_NONE:
                return "None";
            case NotificationManager.IMPORTANCE_MIN:
                return "Minimum";
            case NotificationManager.IMPORTANCE_LOW:
                return "Low";
            case NotificationManager.IMPORTANCE_DEFAULT:
                return "Default";
            case NotificationManager.IMPORTANCE_HIGH:
                return "High";
            case NotificationManager.IMPORTANCE_MAX:
                return "Maximum";
            default:
                return "";
        }//getImportanceString
    }//getImportanceString

}//SettingsActivity - class
