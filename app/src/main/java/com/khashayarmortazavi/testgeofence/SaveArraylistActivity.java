package com.khashayarmortazavi.testgeofence;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import static com.khashayarmortazavi.testgeofence.MainActivity.MY_PREF_ARRAY_KEY;
import static com.khashayarmortazavi.testgeofence.MainActivity.MY_PREF_NAME;

public class SaveArraylistActivity extends AppCompatActivity {

    private EditText input;
    private TextView output;
    private ArrayList<String> inputList;
    private ArrayList<String> outputList;

    private SharedPreferences shref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_arraylist);

        input = findViewById(R.id.text_input);
        output = findViewById(R.id.text_output);

        Button saveButton = findViewById(R.id.button_save);
        Button loadButton = findViewById(R.id.button_load);
        Button eraseAllButton = findViewById(R.id.button_remove_all);


        inputList = new ArrayList<>();
        outputList = new ArrayList<>();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                saveData();
                ArrayList<String> list = new ArrayList<>();
                list.add(input.getText().toString());
                MainActivity.saveArrayList(getApplicationContext(), list);
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadData();
                output.setText("");
                ArrayList<String> out = MainActivity.loadArrayList(getApplicationContext());

                if (out != null) {
                    for (String item : out) {
                        output.append("\n" + item);
                    }
                }

            }
        });

        eraseAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAll();

            }
        });


        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                output.append(" " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {


            }
        });


    }//onCreate


    private void saveData() {
        inputList.add("Khash");
        inputList.add("HQ");
        inputList.add(input.getText().toString());

        shref = this.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);

        Gson gson = new Gson();

        String json = gson.toJson(inputList);

        editor = shref.edit();
        editor.remove(MY_PREF_ARRAY_KEY).apply();
        editor.putString(MY_PREF_ARRAY_KEY, json);
        editor.apply();


    }//saveData

    private void loadData() {

        Gson gson = new Gson();
        shref = this.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);
        String response = shref.getString(MY_PREF_ARRAY_KEY, "");
        ArrayList<String> loadedList = gson.fromJson(response,
                new TypeToken<List<String>>(){}.getType());

        for (String item : loadedList) {
            output.append("\n" + item);
        }

    }//loadData

    private void removeAll() {
        shref = this.getSharedPreferences(MY_PREF_NAME, Context.MODE_PRIVATE);

        editor = shref.edit();
        editor.remove(MY_PREF_ARRAY_KEY).apply();
    }//removeAll


}//Class
