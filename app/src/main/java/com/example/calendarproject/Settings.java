package com.example.calendarproject;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Settings extends AppCompatActivity {
    Switch mode;
    Spinner eventSpinner;
    Button deleteEvent, addEventBtn;
    EditText addEventText;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPref = this.getSharedPreferences("a", Context.MODE_PRIVATE);
        //ekrandaki itemler initialize edilir
        InitializeLayout();
        //set event list from shared preferences
        CheckNightModeState();
        //Event listesli set edilir
        setEventList();

        //dark/light mode
        mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    saveNightModeState(true);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    saveNightModeState(false);
                }
            }
        });
        //etkinlik ekleme
        addEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AvaibleForAddEvent()) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("EventTypeJson", AddNewEventType());
                    editor.apply();
                    Toast toast = Toast.makeText(getApplicationContext(), "Yeni etkinlik türü eklendi", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    //refresh data
                    setEventList();
                }
            }
        });
        //etkinlik silme
        deleteEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = eventSpinner.getSelectedItem().toString();
                JSONObject json = ReadEventTypes();
                ArrayList<String> list = ConvertToArrayList(json);
                list.remove(text);
                try {

                    JSONObject newJson = new JSONObject();
                    JSONArray a = new JSONArray(list);
                    newJson.put("eventType", a);

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("EventTypeJson", newJson.toString());
                    editor.apply();

                    Toast toast = Toast.makeText(getApplicationContext(), "Seçilen etkinlik silindi", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    //refresh data
                    setEventList();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //dark/light mode u kaydeder
    private void saveNightModeState(boolean nightmode) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("mode", nightmode);
        editor.apply();
    }

    //dark/light modu kontrol eder
    public void CheckNightModeState(){
        if(sharedPref.getBoolean("mode",false)){
            mode.setChecked(true);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else{
            mode.setChecked(false);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    //ekrandaki itemleri initialize eder
    public void InitializeLayout() {
        mode = findViewById(R.id.mode);
        eventSpinner = findViewById(R.id.spinnerSettings);
        deleteEvent = findViewById(R.id.deleteEvent);
        addEventText = findViewById(R.id.addEventText);
        addEventBtn = findViewById(R.id.addEventBtn);
    }

    public void setEventList() {

        JSONObject json = ReadEventTypes();
        ArrayList<String> eventTypes = ConvertToArrayList(json);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, eventTypes);
        eventSpinner.setAdapter(adapter);
    }

    //Etkinlik türlerini shared pref ten alır
    public JSONObject ReadEventTypes() {

        String eventType = sharedPref.getString("EventTypeJson", "{ \"eventType\":[ \"Dogum gunu\", \"Toplanti\", \"Gorev\" ]\n" + "}");
        try {
            JSONObject jsonObj = new JSONObject(eventType);
            return jsonObj;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //shared pref te json array olaran etkinlik tülerini listeye çevirir
    public ArrayList<String> ConvertToArrayList(JSONObject jsonObject) {
        ArrayList<String> eventTypes = new ArrayList<String>();

        try {
            JSONArray eventJsonArray = new JSONArray(jsonObject.getString("eventType"));

            for (int i = 0; i < eventJsonArray.length(); i++) {
                eventTypes.add(eventJsonArray.getString(i));
            }
            return eventTypes;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Yeni etkinlik türü müsait mi
    public boolean AvaibleForAddEvent() {
        if (addEventText.getText() == null) {
            Toast toast = Toast.makeText(getApplicationContext(), "Etkinlik türü boş olamaz", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            return false;
        }
        if (addEventText.getText().toString().trim().toLowerCase().equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), "Etkinlik türü boş olamaz", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            return false;
        }
        JSONObject json = ReadEventTypes();
        ArrayList<String> eventTypes = ConvertToArrayList(json);
        for (String list : eventTypes) {
            if (list.trim().toLowerCase().equals(addEventText.getText().toString().trim().toLowerCase())) {
                Toast toast = Toast.makeText(getApplicationContext(), "Bu etklinlik türü zaten var", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
                return false;
            }
        }

        return true;
    }

    //Yeni etkinlik türü ekler
    public String AddNewEventType() {
        JSONObject json = ReadEventTypes();
        try {
            JSONArray eventJsonArray = new JSONArray(json.getString("eventType"));
            eventJsonArray.put(addEventText.getText().toString().trim().toLowerCase());
            JSONObject newJson = new JSONObject();
            newJson.put("eventType", eventJsonArray);

            return newJson.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
