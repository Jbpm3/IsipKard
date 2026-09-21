package ph.edu.usc24100596;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnRegister;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);

        dbHelper = new DBHelper(this);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            } else if(dbHelper.checkEmailExists(email)) {
                Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
            } else {
                boolean inserted = dbHelper.insertUser(email, password);
                if(inserted) {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // go back to login
                } else {
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}