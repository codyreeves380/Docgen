package com.yourname.docgen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.aspose.words.Document;
import com.aspose.words.FindReplaceOptions;
import com.aspose.words.SaveFormat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_DOCX_FILE = 1;
    private static final int CREATE_DOCX_FILE = 2;
    private static final String PREFS_NAME = "saved_form_inputs";

    private EditText inputDefendant, inputCauseNo, inputDate, inputDefId;
    private EditText inputDeposit, inputDay, inputYear, inputMonth;
    private EditText inputComplianceOfficer, inputPhone, inputEmail, inputBadge;
    private EditText inputGps, inputProgram, inputTax, inputJudge;

    private Button btnSelectFile, btnGenerate;
    private TextView txtSelectedFile, txtStatus;
    private Uri selectedFileUri;
    private Map<String, String> pendingReplacements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputDefendant = findViewById(R.id.input_defendant);
        inputCauseNo = findViewById(R.id.input_causeno);
        inputDate = findViewById(R.id.input_date);
        inputDefId = findViewById(R.id.input_defid);
        inputDeposit = findViewById(R.id.input_deposit);
        inputDay = findViewById(R.id.input_day);
        inputMonth = findViewById(R.id.input_month);
        inputYear = findViewById(R.id.input_year);

        inputComplianceOfficer = findViewById(R.id.input_compliance_officer);
        inputPhone = findViewById(R.id.input_phone);
        inputEmail = findViewById(R.id.input_email);
        inputBadge = findViewById(R.id.input_badge);
        inputGps = findViewById(R.id.input_gps);
        inputProgram = findViewById(R.id.input_program);
        inputTax = findViewById(R.id.input_tax);
        inputJudge = findViewById(R.id.input_judge);

        bindSavedInput(inputDefendant, findViewById(R.id.save_defendant), "defendant");
        bindSavedInput(inputCauseNo, findViewById(R.id.save_causeno), "causeno");
        bindSavedInput(inputDate, findViewById(R.id.save_date), "date");
        bindSavedInput(inputDefId, findViewById(R.id.save_defid), "defid");
        bindSavedInput(inputDeposit, findViewById(R.id.save_deposit), "deposit");
        bindSavedInput(inputDay, findViewById(R.id.save_day), "day");
        bindSavedInput(inputMonth, findViewById(R.id.save_month), "month");
        bindSavedInput(inputYear, findViewById(R.id.save_year), "year");
        bindSavedInput(inputComplianceOfficer,
                findViewById(R.id.save_compliance_officer), "compliance_officer");
        bindSavedInput(inputPhone, findViewById(R.id.save_phone), "phone");
        bindSavedInput(inputEmail, findViewById(R.id.save_email), "email");
        bindSavedInput(inputBadge, findViewById(R.id.save_badge), "badge");
        bindSavedInput(inputGps, findViewById(R.id.save_gps), "gps");
        bindSavedInput(inputProgram, findViewById(R.id.save_program), "program");
        bindSavedInput(inputTax, findViewById(R.id.save_tax), "tax");
        bindSavedInput(inputJudge, findViewById(R.id.save_judge), "judge");

        btnSelectFile = findViewById(R.id.btn_select_file);
        btnGenerate = findViewById(R.id.btn_generate);
        txtSelectedFile = findViewById(R.id.txt_selected_file);
        txtStatus = findViewById(R.id.txt_status);

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                startActivityForResult(intent, PICK_DOCX_FILE);
            }
        });

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDocumentGeneration();
            }
        });
    }

    private void bindSavedInput(
            final EditText input,
            final CheckBox saveCheckbox,
            final String key) {
        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (prefs.contains(key)) {
            input.setText(prefs.getString(key, ""));
            saveCheckbox.setChecked(true);
        } else {
            saveCheckbox.setChecked(false);
        }

        saveCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                prefs.edit().putString(key, input.getText().toString()).apply();
            } else {
                prefs.edit().remove(key).apply();
            }
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (saveCheckbox.isChecked()) {
                    prefs.edit().putString(key, s.toString()).apply();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            if (requestCode == CREATE_DOCX_FILE) {
                txtStatus.setText("Status: Save cancelled.");
            }
            return;
        }

        Uri resultUri = data.getData();
        if (requestCode == PICK_DOCX_FILE) {
            selectedFileUri = resultUri;
            int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            try {
                getContentResolver().takePersistableUriPermission(resultUri, takeFlags);
            } catch (SecurityException ignored) {
                // Some document providers grant only temporary access.
            }
            txtSelectedFile.setText("Selected: " + resultUri.getLastPathSegment());
            txtStatus.setText("Status: Template Loaded.");
        } else if (requestCode == CREATE_DOCX_FILE) {
            writeGeneratedDocument(resultUri);
        }
    }

    private void runDocumentGeneration() {
        if (selectedFileUri == null) {
            txtStatus.setText("Status: Please select a template file first!");
            return;
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("[[DEFENDANT]]", inputDefendant.getText().toString().trim());
        replacements.put("[[CAUSENO]]", inputCauseNo.getText().toString().trim());
        replacements.put("[[DATE]]", inputDate.getText().toString().trim());
        replacements.put("[[DEFID]]", inputDefId.getText().toString().trim());
        replacements.put("[[DEPOSIT]]", inputDeposit.getText().toString().trim());
        replacements.put("[[DAY]]", inputDay.getText().toString().trim());
        replacements.put("[[MONTH]]", inputMonth.getText().toString().trim());
        replacements.put("[[YEAR]]", inputYear.getText().toString().trim());
        replacements.put("[[CO]]", inputComplianceOfficer.getText().toString().trim());
        replacements.put("[[PHONE]]", inputPhone.getText().toString().trim());
        replacements.put("[[EMAIL]]", inputEmail.getText().toString().trim());
        replacements.put("[[BADGE]]", inputBadge.getText().toString().trim());
        replacements.put("[[GPS]]", inputGps.getText().toString().trim());
        replacements.put("[[PRO]]", inputProgram.getText().toString().trim());
        replacements.put("[[TAX]]", inputTax.getText().toString().trim());
        replacements.put("[[JUDGE]]", inputJudge.getText().toString().trim());

        if (replacements.get("[[DEFENDANT]]").isEmpty()
                || replacements.get("[[CAUSENO]]").isEmpty()) {
            txtStatus.setText("Status: Please enter Defendant and Cause No!");
            return;
        }

        pendingReplacements = replacements;
        Intent createIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        createIntent.addCategory(Intent.CATEGORY_OPENABLE);
        createIntent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        createIntent.putExtra(Intent.EXTRA_TITLE,
                "Generated_" + System.currentTimeMillis() + ".docx");
        startActivityForResult(createIntent, CREATE_DOCX_FILE);
    }

    private void writeGeneratedDocument(Uri outputUri) {
        if (pendingReplacements == null || selectedFileUri == null) {
            txtStatus.setText("Status: Missing template or form data.");
            return;
        }

        try (InputStream input = getContentResolver().openInputStream(selectedFileUri)) {
            if (input == null) {
                txtStatus.setText("Status: Could not open the selected template.");
                return;
            }

            Document document = new Document(input);
            FindReplaceOptions options = new FindReplaceOptions();
            for (Map.Entry<String, String> replacement : pendingReplacements.entrySet()) {
                document.getRange().replace(
                        replacement.getKey(), replacement.getValue(), options);
            }

            try (OutputStream output = getContentResolver().openOutputStream(outputUri, "w")) {
                if (output == null) {
                    txtStatus.setText("Status: Could not create the output document.");
                    return;
                }
                document.save(output, SaveFormat.DOCX);
            }
            txtStatus.setText("Status: Generated document saved.");
            pendingReplacements = null;
        } catch (Exception e) {
            txtStatus.setText("Status: Document generation failed: " + e.getMessage());
        }
    }
}
