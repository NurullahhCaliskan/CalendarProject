package com.example.calendarproject;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CustomCalendarView extends LinearLayout {
    ImageButton nextButton, previousButton, settingsButton;
    TextView currentDate;
    GridView gridView;
    private static final int MAX_CALENDAR_DAYS = 42;  //6*7
    Calendar calendar = Calendar.getInstance(Locale.ENGLISH);   //Turkçe yok
    Context context;
    DB dbopenHelper;

    GridAdapter gridAdapter;
    AlertDialog alertDialog;
    List<Date> dates = new ArrayList<>();
    List<Etkinlikler> etkinliklerList = new ArrayList<>();
    int alarmYear, alarmMonth, alarmDay, alarmHour, alarmMinute;
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
    SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.ENGLISH);
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.ENGLISH);
    SimpleDateFormat eventDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    SharedPreferences sharedPref;


    public CustomCalendarView(Context context) {
        super(context);
    }

    public CustomCalendarView(final Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        //Etkinlik türü ve dark/light mode bilgileri shared prefte saklanır
        sharedPref = this.context.getSharedPreferences("a", Context.MODE_PRIVATE);
        //Ekrandaki itemler initialize edilir
        InitializeLayout();
        //Takvim güncelleme
        SetUpCalendar();

        //ayarlar butonu ile Ayarlar activity açılır
        settingsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activity2Intent = new Intent(context.getApplicationContext(), Settings.class);
                context.startActivity(activity2Intent);

            }
        });
        //Takvimde önceki ay a gider.
        previousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(calendar.MONTH, -1);
                SetUpCalendar();
            }
        });

        //Takvimde sonraki ay a gider.
        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.add(calendar.MONTH, 1);
                SetUpCalendar();
            }
        });

        //Seçilen gün ile yeni layout açılır, Etkinlik işlemleri yapılır.
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(true);
                final View addView = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_newevent_layout, null);
                final EditText eventName = addView.findViewById(R.id.eventname);
                final TextView eventTime = addView.findViewById(R.id.eventtime);
                final ImageButton setTime = addView.findViewById(R.id.seteventtime);
                final Button addEvent = addView.findViewById(R.id.addevent);
                final CheckBox alarmMe = addView.findViewById(R.id.alarmMe123);
                final Spinner eventType = addView.findViewById(R.id.spinner1);

                JSONObject json = ReadEventTypes();
                ArrayList<String> eventTypes = ConvertToArrayList(json);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, eventTypes);
                eventType.setAdapter(adapter);
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_YEAR, -1);
                Date Mytomorrow = tomorrow.getTime();

                Calendar dateCalendar = Calendar.getInstance();
                dateCalendar.setTime(dates.get(position));
                alarmYear = dateCalendar.get(Calendar.YEAR);
                alarmMonth = dateCalendar.get(Calendar.MONTH);
                alarmDay = dateCalendar.get(Calendar.DAY_OF_MONTH);

                if(Mytomorrow.after(dateCalendar.getTime())){
                        alarmMe.setVisibility(View.INVISIBLE);
                }

                setTime.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Calendar calendar = Calendar.getInstance();
                        int hours = calendar.get(Calendar.HOUR_OF_DAY);
                        int minuts = calendar.get(Calendar.MINUTE);

                        //TimerPicker ile kullanıcının seçtiği saat bilgisi aınır
                        TimePickerDialog timePickerDialog = new TimePickerDialog(addView.getContext(), R.style.Theme_AppCompat_Dialog, new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                Calendar c = Calendar.getInstance();
                                c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                c.set(Calendar.MINUTE, minute);
                                c.setTimeZone(TimeZone.getDefault());
                                SimpleDateFormat hformate = new SimpleDateFormat("K:mm a", Locale.ENGLISH);
                                String event_Time = hformate.format(c.getTime());
                                eventTime.setText(event_Time);

                                alarmHour = c.get(Calendar.HOUR_OF_DAY);
                                alarmMinute = c.get(Calendar.MINUTE);
                            }
                        }, hours, minuts, false);
                        timePickerDialog.show();
                    }
                });
                final String date = eventDateFormat.format(dates.get(position));
                final String month = monthFormat.format(dates.get(position));
                final String year = yearFormat.format(dates.get(position));
                //Event eklenir.
                addEvent.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Kullanıcının alarm seçimi set edilir. DB de on-off olarak kaydedilir.
                        if (alarmMe.isChecked()) {
                            SaveEvent(eventName.getText().toString(), eventTime.getText().toString(), date, month, year, "on", eventType.getSelectedItem().toString());
                            SetUpCalendar();
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(alarmYear, alarmMonth, alarmDay, alarmHour, alarmMinute);
                            setAlarm(calendar, eventName.getText().toString(), eventTime.getText().toString(), eventType.getSelectedItem().toString(), GetRequestCode(date,
                                    eventName.getText().toString(), eventTime.getText().toString()));

                            alertDialog.dismiss();

                        } else {
                            SaveEvent(eventName.getText().toString(), eventTime.getText().toString(), date, month, year, "off", eventType.getSelectedItem().toString());
                            SetUpCalendar();
                            alertDialog.dismiss();

                        }
                    }
                });
                builder.setView(addView);
                alertDialog = builder.create();
                alertDialog.show();
                //Etkinlikten geri geldiğimizde etkinlikler güncellensin
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        SetUpCalendar();
                    }
                });
            }
        });
        //Gün e uzun basıldığında gündeki etkinlikler listelensin.
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String date = eventDateFormat.format(dates.get(position));

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(true);
                View showView = LayoutInflater.from(parent.getContext()).inflate(R.layout.show_events_layout, null);
                RecyclerView recyclerView = showView.findViewById(R.id.EventsRV);
                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(showView.getContext());
                recyclerView.setLayoutManager(layoutManager);
                recyclerView.setHasFixedSize(true);
                EventAdapter eventAdapter = new EventAdapter(showView.getContext(), CollectEventByDate(date));
                recyclerView.setAdapter(eventAdapter);
                eventAdapter.notifyDataSetChanged();

                builder.setView(showView);
                alertDialog = builder.create();
                alertDialog.show();
//                TextView events = showView.findViewById(R.id.eventname);
//                TextView time = showView.findViewById(R.id.eventtime);
//                TextView date = showView.findViewById(R.id.eventdate);
                return true;
            }
        });
    }


    private int GetRequestCode(String date, String event, String time) {
        int code = 0;
        //burada hata olabilir UNUTMAAAAAAAAAA 32:10
        dbopenHelper = new DB(context);
        SQLiteDatabase database = dbopenHelper.getReadableDatabase();
        Cursor cursor = dbopenHelper.ReadIDEvents(date, event, time, database);
        while (cursor.moveToNext()) {
            code = cursor.getInt(cursor.getColumnIndex(DBYapisi.ID));
        }
        cursor.close();
        database.close();
        return code;
    }

    //Alarm set edilir.
    private void setAlarm(Calendar calendar, String event, String time, String eventType, int requestCode) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
        intent.putExtra("event", event);
        intent.putExtra("time", time);
        intent.putExtra("id", requestCode);
        intent.putExtra("eventType", eventType);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    //Bir gündeki etkinlikler listelenir
    private ArrayList<Etkinlikler> CollectEventByDate(String date) {
        ArrayList<Etkinlikler> arrayList = new ArrayList<>();
        dbopenHelper = new DB(context);
        SQLiteDatabase database = dbopenHelper.getReadableDatabase();
        Cursor cursor = dbopenHelper.ReadEvents(date, database);
        while (cursor.moveToNext()) {
            String event = cursor.getString(cursor.getColumnIndex(DBYapisi.EVENT));
            String time = cursor.getString(cursor.getColumnIndex(DBYapisi.TIME));
            String Date = cursor.getString(cursor.getColumnIndex(DBYapisi.DATE));
            String month = cursor.getString(cursor.getColumnIndex(DBYapisi.MONTH));
            String Year = cursor.getString(cursor.getColumnIndex(DBYapisi.YEAR));
            Etkinlikler etkinlikler = new Etkinlikler(event, time, Date, month, Year);
            arrayList.add(etkinlikler);
        }
        cursor.close();
        database.close();
        return arrayList;
    }

    //Etkinlik kaydedilir.
    private void SaveEvent(String event, String time, String date, String month, String year, String notify, String eventType) {

        dbopenHelper = new DB(context);
        SQLiteDatabase database = dbopenHelper.getWritableDatabase();
        dbopenHelper.SaveEvent(event, time, date, month, year, notify, eventType, database);
        dbopenHelper.close();
        Toast.makeText(context, "Olay kaydedildi", Toast.LENGTH_SHORT).show();
    }

    public void InitializeLayout() {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.calendar_layout, this);
        nextButton = view.findViewById(R.id.nextBtn);
        previousButton = view.findViewById(R.id.previousBtn);
        currentDate = view.findViewById(R.id.current_Date);
        gridView = view.findViewById(R.id.gridView);
        settingsButton = view.findViewById(R.id.settingsBtn);
    }

    public void SetUpCalendar() {
        String currentDatem = dateFormat.format(calendar.getTime());
        currentDate.setText(currentDatem);
        dates.clear();
        Calendar monthCalendar = (Calendar) calendar.clone();
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDay = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        monthCalendar.add(Calendar.DAY_OF_MONTH, -firstDay);
        CollectEventsPerMonth(monthFormat.format(calendar.getTime()), yearFormat.format(calendar.getTime()));

        while (dates.size() < MAX_CALENDAR_DAYS) {
            dates.add(monthCalendar.getTime());
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        gridAdapter = new GridAdapter(context, dates, calendar, etkinliklerList);
        gridView.setAdapter(gridAdapter);
    }

    //Bir aydaki günler set edilir (Main sayfada her günün etkinlikleri sayısı göterilir)
    private void CollectEventsPerMonth(String Month, String year) {

        etkinliklerList.clear();
        dbopenHelper = new DB(context);
        SQLiteDatabase database = dbopenHelper.getReadableDatabase();
        Cursor cursor = dbopenHelper.ReadEventsPerMonth(Month, year, database);
        while (cursor.moveToNext()) {
            String event = cursor.getString(cursor.getColumnIndex(DBYapisi.EVENT));
            String time = cursor.getString(cursor.getColumnIndex(DBYapisi.TIME));
            String date = cursor.getString(cursor.getColumnIndex(DBYapisi.DATE));
            String month = cursor.getString(cursor.getColumnIndex(DBYapisi.MONTH));
            String Year = cursor.getString(cursor.getColumnIndex(DBYapisi.YEAR));
            Etkinlikler etkinlikler = new Etkinlikler(event, time, date, month, Year);
            etkinliklerList.add(etkinlikler);
        }
        cursor.close();
        dbopenHelper.close();
    }

    //Shared pref te saklanan etkinlik türü bilgileri JSON olarak saklanır.
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

    //Shared pref ten etkinlik türü bilgileri alınır
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
}
