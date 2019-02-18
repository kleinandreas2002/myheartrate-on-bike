package com.example.aklesoft.heartrate_monitor;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class MainSettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);

        m_ReloadBtImage = findViewById(R.id.imageBtRefresh);

        initSwitched();
        setupOrientationSpinner();
    }

    public void onClickStartBlackMode(View v)
    {
        Toast.makeText(this, "onClickStartBlackMode", Toast.LENGTH_LONG).show();

        Log.d("Test Log", "onClickStartBlackMode Clock: " + m_ClockSwitch.isChecked());
        Log.d("Test Log", "onClickStartBlackMode Stopwatch: " + m_StopwatchSwitch.isChecked());
        Log.d("Test Log", "onClickStartBlackMode Speedometer: " + m_SpeedometerSwitch.isChecked());
        Log.d("Test Log", "onClickStartBlackMode Heartrate: " + m_HeartrateSwitch.isChecked());
        Log.d("Test Log", "onClickStartBlackMode Spinner: " + m_OrientationSpinner.getSelectedItem());
    }

    public void onClickReloadBt(View v)
    {
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_refresh_24px);
        m_ReloadBtImage.setImageResource(R.drawable.ic_baseline_clear_24px);
    }


    // NOTE: Spinner selection callbacks

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        String selectedValue = parent.getItemAtPosition(pos).toString();
        ImageView img = findViewById(R.id.imageViewOrientationSettings);

        if (selectedValue.equals(getResources().getString(R.string.rbHorizontalOrientation)))
        {
            img.setImageResource(R.drawable.ic_baseline_screen_lock_landscape_24px_w);
        }
        else if (selectedValue.equals(getString(R.string.rbVertivalOrientation)))
        {
            img.setImageResource(R.drawable.ic_baseline_screen_lock_portrait_24px_w);
        }
        else if (selectedValue.equals(getString(R.string.rbRotationOrientation)))
        {
            img.setImageResource(R.drawable.ic_baseline_screen_rotation_24px_w);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView)
    {

    }

    // ---------------------------------------------------------------------------------------------
    // PRIVATE

    private void initSwitched()
    {
        m_ClockSwitch = findViewById(R.id.switchClock);
        m_StopwatchSwitch = findViewById(R.id.switchStopwatch);
        m_SpeedometerSwitch = findViewById(R.id.switchSpeedometer);
        m_HeartrateSwitch = findViewById(R.id.switchHeartrate);
    }

    private void setupOrientationSpinner()
    {
        m_OrientationSpinner = findViewById(R.id.spinnerOrientation);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.blackmode_orienntations, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        m_OrientationSpinner.setAdapter(adapter);
        m_OrientationSpinner.setOnItemSelectedListener(this);
    }

    private Spinner m_OrientationSpinner;
    private Switch m_ClockSwitch;
    private Switch m_StopwatchSwitch;
    private Switch m_SpeedometerSwitch;
    private Switch m_HeartrateSwitch;
    private ImageView m_ReloadBtImage;
}
