package com.example.aklesoft.heartrate_monitor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;

public class MainSettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);

        m_StopwatchStartAuto = findViewById(R.id.cbStartStopwatch);
        m_ReloadBtImage = findViewById(R.id.imageBtRefresh);

        initSwitched();
        setupOrientationSpinner();
    }

    public void onClickStartBlackMode(View v)
    {
        Intent intentToStartBlackMode = new Intent(getApplicationContext(), BlackMode.class);

        intentToStartBlackMode.putExtra("ShowSpeed", m_SpeedometerSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowHR", m_HeartrateSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowStopwatch", m_StopwatchSwitch.isChecked());
        intentToStartBlackMode.putExtra("ShowClock", m_ClockSwitch.isChecked());
        intentToStartBlackMode.putExtra("StartStopwatch", m_StopwatchStartAuto.isChecked());
        intentToStartBlackMode.putExtra("BlackModeOrientation", m_OrientationSpinner.getSelectedItem().toString());

        startActivity(intentToStartBlackMode);
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
        /*
        Get the imageView next to the spinner to update the image accordingly to the selected value
        of the spinner itself.
         */

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
    
    // ---------------------------------------------------------------------------------------------

    private CheckBox m_StopwatchStartAuto;
    private Spinner m_OrientationSpinner;
    private Switch m_ClockSwitch;
    private Switch m_StopwatchSwitch;
    private Switch m_SpeedometerSwitch;
    private Switch m_HeartrateSwitch;
    private ImageView m_ReloadBtImage;
}
