package com.practica.camera;

import static com.practica.camera.BottomSheetsKt.DATA_FLAG;
import static com.practica.camera.CameraKt.INTENTARIO_NAME;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btn);
        Button btnFab = findViewById(R.id.btn1);

        btn.setOnClickListener(v->{
            Intent intent = new Intent(this, Camera.class);
            intent.putExtra(INTENTARIO_NAME, "camera");
            someActivityResultLauncher.launch(intent);

        });
        btnFab.setOnClickListener(v->{
            Intent intent = new Intent(this, BottomSheets.class);
            intent.putExtra(DATA_FLAG, "Humberto");
            someActivityResultLauncher.launch(intent);
        });


    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    assert data != null;
                    Toast.makeText(MainActivity.this, data.getStringExtra("path"), Toast.LENGTH_SHORT).show();
                }
            });
}