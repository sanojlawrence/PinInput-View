package com.jldevelopers.pininputview;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.jldevelopers.pininput.PinInputView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.pin_login_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        PinInputView enterPin = dialogView.findViewById(R.id.pin_login);
        enterPin.requestFocus(); // Force focus to first PIN input
        enterPin.setLabel("enter_pin");

        final AlertDialog dialog = builder.create();
        dialog.show();

        PinInputView pinInputView = findViewById(R.id.pin_login);
        pinInputView.setLabel("Enter PIN");
        pinInputView.requestFocus();
        pinInputView.setOnPinEnteredListener(new PinInputView.OnPinEnteredListener() {
            @Override
            public void onPinEntered(String pin) {
                Toast.makeText(MainActivity.this, pin, Toast.LENGTH_SHORT).show();
            }
        });

        PinInputView confirmpin = findViewById(R.id.outlinedTextField);
        confirmpin.setLabel("Confirm PIN");
        confirmpin.setOnPinEnteredListener(new PinInputView.OnPinEnteredListener() {
            @Override
            public void onPinEntered(String pin) {
                Toast.makeText(MainActivity.this, pin, Toast.LENGTH_SHORT).show();
            }
        });
    }
}