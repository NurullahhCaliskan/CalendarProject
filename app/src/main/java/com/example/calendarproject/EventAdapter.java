package com.example.calendarproject;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.MyViewHolder> {
    Context context;
    ArrayList<Etkinlikler> arrayList;
    DB DB;

    public EventAdapter(Context context, ArrayList<Etkinlikler> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.show_events_rowlayout, parent, false);

        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {
        final Etkinlikler etkinlikler = arrayList.get(position);
        holder.event.setText(etkinlikler.getEVENT());
        holder.dateTxt.setText(etkinlikler.getDATE());
        holder.time.setText(etkinlikler.getTIME());

        holder.mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent activity2Intent = new Intent(context.getApplicationContext(), MapsActivity.class);
                context.startActivity(activity2Intent);
            }
        });

        //Etkinlik mail olarak gönderilir
        holder.sendMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mailText = "Merhaba, \n mail detayları aşağıdaki gibidir.\n Etkinlik = " + etkinlikler.getEVENT().toString()
                        + "\nEtkinlik günü = " + etkinlikler.getDATE().toString() + "\n" + "Etkinlik saati =" + etkinlikler.getTIME().toString();
                Uri uri = Uri.parse("mailto:" + "nurullahcaliskan96@gmail.com")
                        .buildUpon()
                        .appendQueryParameter("subject", "Etkinlik detayları")
                        .appendQueryParameter("body", mailText)
                        .build();

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
                try {
                    context.startActivity(Intent.createChooser(emailIntent, "Sending.."));

                } catch (android.content.ActivityNotFoundException ex) {
                    Toast toast = Toast.makeText(context.getApplicationContext(), "Mail gönderilemedi", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                }
            }
        });

        //Etkinlik silinir
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteCalendarEvent(etkinlikler.getEVENT(), etkinlikler.getDATE(), etkinlikler.getTIME());
                arrayList.remove(position);
                notifyDataSetChanged();


            }
        });

        if (isAlarmed(etkinlikler.getDATE(), etkinlikler.getEVENT(), etkinlikler.getTIME())) {
            holder.setAlarm.setImageResource(R.drawable.ic_action_notification_on);
            //silinebilir
            //notifyDataSetChanged();

        } else {
            holder.setAlarm.setImageResource(R.drawable.ic_action_notification_off);
            //notifyDataSetChanged();

        }
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(ConvertStringToDate(etkinlikler.getDATE()));
        final int alarmYear = dateCalendar.get(Calendar.YEAR);
        final int alarmMonth = dateCalendar.get(Calendar.MONTH);
        final int alarmDay = dateCalendar.get(Calendar.DAY_OF_MONTH);
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(ConvertStringToTime(etkinlikler.getTIME()));
        final int alarmHour = timeCalendar.get(Calendar.HOUR_OF_DAY);
        final int alarmMinute = timeCalendar.get(Calendar.MINUTE);

        //Etkinlik alarm gündellenir
        holder.setAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAlarmed(etkinlikler.getDATE(), etkinlikler.getEVENT(), etkinlikler.getTIME())) {
                    holder.setAlarm.setImageResource(R.drawable.ic_action_notification_off);
                    cancelAlarm(GetRequestCode(etkinlikler.getDATE(), etkinlikler.getEVENT(), etkinlikler.getTIME()));
                    UpdateEvent(etkinlikler.getDATE(), etkinlikler.getEVENT(), etkinlikler.getTIME(), "off");
                    notifyDataSetChanged();

                } else {
                    holder.setAlarm.setImageResource(R.drawable.ic_action_notification_on);
                    Calendar alarmCalendar = Calendar.getInstance();
                    alarmCalendar.set(alarmYear, alarmMonth, alarmDay, alarmHour, alarmMinute);
                    setAlarm(alarmCalendar, etkinlikler.getEVENT(), etkinlikler.getTIME(), GetRequestCode(etkinlikler.getDATE(), etkinlikler.getEVENT(), etkinlikler.getTIME()));
                    UpdateEvent(etkinlikler.getDATE(), etkinlikler.getEVENT(), etkinlikler.getTIME(), "on");
                    notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }


    public Date ConvertStringToDate(String eventDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(eventDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public Date ConvertStringToTime(String eventDate) {
        SimpleDateFormat format = new SimpleDateFormat("kk:mm", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(eventDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    private void DeleteCalendarEvent(String event, String date, String time) {
        DB = new DB(context);
        SQLiteDatabase database = DB.getWritableDatabase();
        DB.DeleteEvent(event, date, time, database);
        DB.close();

    }

    //Etkinlik alarmı ikonu set edilirken kullanılır.
    private boolean isAlarmed(String date, String event, String time) {
        boolean alarmed = false;
        DB = new DB(context);
        SQLiteDatabase database = DB.getReadableDatabase();
        Cursor cursor = DB.ReadIDEvents(date, event, time, database);
        while (cursor.moveToNext()) {
            String notif = cursor.getString(cursor.getColumnIndex(DBYapisi.NOTIFY));
            if (notif.equals("on")) {
                alarmed = true;
            } else {
                alarmed = false;
            }

        }
        cursor.close();
        database.close();
        return alarmed;
    }

    //Alarm set edilir.
    private void setAlarm(Calendar calendar, String event, String time, int requestCode) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);
        intent.putExtra("event", event);
        intent.putExtra("time", time);
        intent.putExtra("id", requestCode);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

    }

    private void cancelAlarm(int requestCode) {
        Intent intent = new Intent(context.getApplicationContext(), AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

    }

    private int GetRequestCode(String date, String event, String time) {
        int code = 0;
        DB = new DB(context);
        SQLiteDatabase database = DB.getReadableDatabase();
        Cursor cursor = DB.ReadIDEvents(date, event, time, database);
        while (cursor.moveToNext()) {
            code = cursor.getInt(cursor.getColumnIndex(DBYapisi.ID));

        }
        cursor.close();
        database.close();

        return code;
    }

    private void UpdateEvent(String date, String event, String time, String notify) {
        DB = new DB(context);
        SQLiteDatabase database = DB.getWritableDatabase();
        DB.UpdateEvent(date, event, time, notify, database);
        DB.close();

    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView dateTxt, event, time;
        Button delete, sendMail, mapButton;
        ImageButton setAlarm;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.eventtime2);
            dateTxt = itemView.findViewById(R.id.eventdate2);
            event = itemView.findViewById(R.id.eventname2);
            delete = itemView.findViewById(R.id.delete);
            setAlarm = itemView.findViewById(R.id.alarmmeBtn);
            sendMail = itemView.findViewById(R.id.sendMail);
            mapButton = itemView.findViewById(R.id.map);
        }
    }


}
