package ph.edu.usc24100596;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class CreateCategoryDialog extends Dialog {

    private EditText etCategoryName;
    private Button btnCancel, btnOK;
    private DBHelper dbHelper;

    public CreateCategoryDialog(@NonNull Context context) {
        super(context);
        dbHelper = new DBHelper(context); // Initialize DBHelper here
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove title and set layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_create_category);

        if(getWindow() != null){
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        etCategoryName = findViewById(R.id.etCategoryName);
        btnCancel = findViewById(R.id.btnCancel);
        btnOK = findViewById(R.id.btnOK);

        btnCancel.setOnClickListener(v -> dismiss());

        btnOK.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if(!categoryName.isEmpty()){
                boolean added = dbHelper.addCategory(categoryName);
                if(added){
                    Toast.makeText(getContext(), "Category added: " + categoryName, Toast.LENGTH_SHORT).show();
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Failed to add category", Toast.LENGTH_SHORT).show();
                }
            } else {
                etCategoryName.setError("Please enter a name");
            }
        });
    }
}