package kspt.revkina.gps;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

import java.util.ArrayList;
import java.util.HashMap;

public class TimeListActivity extends AppCompatActivity  {

    SimpleAdapter adapter;
    ArrayList<HashMap<String, String>> mList = new ArrayList<>();
    SharedPreferences pref;
    private ConversionListGson conversionListGson = new ConversionListGson();
    private static final String KEY_TIME = "time";
    static String time = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_option);
        ListView userList = findViewById(R.id.userList);

        pref = getApplicationContext().getSharedPreferences("list", 0);
        String storedCollection = pref.getString(KEY_TIME, null);
        if (storedCollection != null) {
            mList = conversionListGson.getSpeedList(KEY_TIME, pref, mList);
        }

        LinearLayout layoutList = new LinearLayout(this);
        layoutList.setOrientation(LinearLayout.VERTICAL);
        final TextView speedTextView = new TextView(this);
        final TextView parTextView = new TextView(this);
        layoutList.addView(speedTextView);
        layoutList.addView(parTextView);

        adapter = new SimpleAdapter(this, mList, android.R.layout.simple_list_item_2,
                new String[]{"time","par"},
                new int[]{android.R.id.text1, android.R.id.text2});
        userList.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(0);
            }
        });
    }

    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    @SuppressLint("ResourceType")
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText timeFromEditText = new EditText(this);
                final EditText timeBeforeEditText = new EditText(this);
                final EditText secondsEditText = new EditText(this);
                final EditText metersEditText = new EditText(this);

                timeFromEditText.setInputType(InputType.TYPE_DATETIME_VARIATION_TIME);
                timeFromEditText.setHint("00:00");
//                timeFromEditText.setText(DateUtils.formatDateTime(this,
//                        Calendar.getInstance().getTimeInMillis(),
//                         DateUtils.FORMAT_SHOW_TIME));



//                timeFromEditText.setOnClickListener (new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        showTimePickerDialog(v);
//                        timeFromEditText.setText(time);
//                    }
//                });
                timeBeforeEditText.setInputType(InputType.TYPE_DATETIME_VARIATION_TIME);
//                timeBeforeEditText.setId(1);
//                timeBeforeEditText.setOnClickListener (new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        showTimePickerDialog(v);
//                    }
//                });
                timeBeforeEditText.setHint("23:59");
                secondsEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                metersEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                timeFromEditText.setHint(R.string.from);
                timeBeforeEditText.setHint(R.string.before);
                secondsEditText.setHint(R.string.seconds);
                metersEditText.setHint(R.string.meters);
                layout.addView(timeFromEditText);
                layout.addView(timeBeforeEditText);
                layout.addView(secondsEditText);
                layout.addView(metersEditText);

                new AlertDialog.Builder(this)
                        .setTitle("Введите временной диапозон:")
                        .setView(layout)
                        .setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String fromSpeed = String.valueOf(timeFromEditText.getText());
                                String beforeSpeed = String.valueOf(timeBeforeEditText.getText());
                                String seconds = String.valueOf(secondsEditText.getText());
                                String meters = String.valueOf(metersEditText.getText());
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("time", "Time: "+fromSpeed+ " - "+beforeSpeed);
                                hm.put("timeFrom", fromSpeed);
                                hm.put("timeBefore", beforeSpeed);
                                hm.put("par", "seconds: "+seconds+ " meters: "+meters);
                                hm.put("seconds",seconds);
                                hm.put("meters", meters);
                                mList.add(hm);
                                conversionListGson.saveSpeedList(KEY_TIME,pref, mList);
                                adapter.notifyDataSetChanged();
                                Snackbar.make(getCurrentFocus(),  "Изменения сохранены", Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                return null;
            default:
                return null;
        }
    }

//    TimePickerDialog.OnTimeSetListener t=new TimePickerDialog.OnTimeSetListener() {
//        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
//            Calendar.getInstance().set(Calendar.HOUR_OF_DAY, hourOfDay);
//            Calendar.getInstance().set(Calendar.MINUTE, minute);
//            time = view.getCurrentHour()+":"+view.getCurrentMinute();
//        }
//    };
//
//    public void setTime(View v) {
//        new TimePickerDialog(TimeListActivity.this, t,
//                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
//                Calendar.getInstance().get(Calendar.MINUTE), true)
//                .show();
//    }

    public static class TimePickerFragment extends DialogFragment implements
            TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            time = hourOfDay + ":"	+ minute;
        }
    }
}