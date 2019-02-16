package com.example.aklesoft.heartrate_monitor;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainSettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);

        setupOrientationSpinner();
    }

    public void onClickStartBlackMode(View v)
    {
        Toast.makeText(this, "Clicked on Button", Toast.LENGTH_LONG).show();
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

    private void setupOrientationSpinner()
    {
        m_Spinner = findViewById(R.id.spinnerOrientation);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.blackmode_orienntations, R.layout.support_simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        m_Spinner.setAdapter(adapter);
        m_Spinner.setOnItemSelectedListener(this);
    }

    private Spinner m_Spinner;
}
