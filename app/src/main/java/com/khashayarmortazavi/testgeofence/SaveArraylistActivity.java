package com.khashayarmortazavi.testgeofence;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

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
        setContentView(R.layout.activity_save_arraylist);

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
                //create a test Fence object and add it to array list
                Fence fence = new Fence ("Test", 49.235675, -123.057118, 100, 3600000, Fence.FENCE_TYPE_ENTER_EXIT);
                ArrayList<Fence> list = new ArrayList<>();
                list.add(fence);
                MainActivity.saveArrayList(getApplicationContext(), list);
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<Fence> list = MainActivity.loadArrayList(getApplicationContext());
                if (list == null) {
                    output.setText("Empty");
                } else {
                    output.setText("");
                    for (Fence item : list) {
                        output.append(item.getSnippet());
                    }//for
                }//if
            }
        });

        eraseAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.eraseAllArrays(getApplicationContext());
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


}//Class
