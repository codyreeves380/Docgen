package com.yourname.docgen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_DOCX_FILE = 1;
    private static final String PREFS_NAME = "saved_tag_values";

    private EditText inputDefendant, inputCauseNo, inputDate, inputDefId, inputDeposit, inputDay, inputYear, inputMonth;
    private EditText inputComplianceOfficer, inputPhone, inputEmail, inputBadge, inputGps, inputProgram, inputTax, inputJudge;
    private Button btnSelectFile, btnGenerate;
    private TextView txtSelectedFile, txtStatus;
    private Uri selectedFileUri = null; // Stores file reference from manager

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
        inputComplianceOfficer = findViewById(R.id.input_co);
        inputPhone = findViewById(R.id.input_phone);
        inputEmail = findViewById(R.id.input_email);
        inputBadge = findViewById(R.id.input_badge);
        inputGps = findViewById(R.id.input_gps);
        inputProgram = findViewById(R.id.input_pro);
        inputTax = findViewById(R.id.input_tax);
        inputJudge = findViewById(R.id.input_judge);
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnGenerate = findViewById(R.id.btn_generate);
        txtSelectedFile = findViewById(R.id.txt_selected_file);
        txtStatus = findViewById(R.id.txt_status);

        SharedPreferences savedValues = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bindRememberedInput(inputComplianceOfficer, findViewById(R.id.save_co), savedValues, "co");
        bindRememberedInput(inputPhone, findViewById(R.id.save_phone), savedValues, "phone");
        bindRememberedInput(inputEmail, findViewById(R.id.save_email), savedValues, "email");
        bindRememberedInput(inputBadge, findViewById(R.id.save_badge), savedValues, "badge");
        bindRememberedInput(inputGps, findViewById(R.id.save_gps), savedValues, "gps");
        bindRememberedInput(inputProgram, findViewById(R.id.save_pro), savedValues, "pro");
        bindRememberedInput(inputTax, findViewById(R.id.save_tax), savedValues, "tax");
        bindRememberedInput(inputJudge, findViewById(R.id.save_judge), savedValues, "judge");

        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, "Select Template File"), PICK_DOCX_FILE);
            }
        });

        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runDocumentGeneration();
            }
        });
    }

    private void bindRememberedInput(final EditText input, final CheckBox saveCheckbox,
                                     final SharedPreferences preferences, final String key) {
        boolean shouldSave = preferences.getBoolean(key + "_enabled", false);
        saveCheckbox.setChecked(shouldSave);
        if (shouldSave) {
            input.setText(preferences.getString(key + "_value", ""));
        }

        saveCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(key + "_enabled", isChecked);
            if (isChecked) {
                editor.putString(key + "_value", input.getText().toString());
            } else {
                editor.remove(key + "_value");
            }
            editor.apply();
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (saveCheckbox.isChecked()) {
                    preferences.edit().putString(key + "_value", s.toString()).apply();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_DOCX_FILE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            txtSelectedFile.setText("Selected: " + selectedFileUri.getLastPathSegment());
            txtStatus.setText("Status: Template Loaded.");
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

        if (replacements.get("[[DEFENDANT]]").isEmpty() || replacements.get("[[CAUSENO]]").isEmpty()) {
            txtStatus.setText("Status: Please enter Defendant and Cause No!");
            return;
        }

        try {
            InputStream is = getContentResolver().openInputStream(selectedFileUri);
            XWPFDocument doc = new XWPFDocument(is);

            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceTags(p, replacements);
            }

            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceTags(p, replacements);
                        }
                    }
                }
            }

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String cleanName = (replacements.get("[[DEFENDANT]]") + "_" + replacements.get("[[CAUSENO]]")).replace(" ", "_");
            File outputFile = new File(downloadsDir, cleanName + ".docx");

            FileOutputStream fos = new FileOutputStream(outputFile);
            doc.write(fos);

            fos.close();
            doc.close();
            is.close();

            txtStatus.setText("✅ Saved to Downloads folder!");
        } catch (Exception e) {
            txtStatus.setText("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void replaceTags(XWPFParagraph p, Map<String, String> data) {
        String text = p.getParagraphText();
        boolean changed = false;

        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (text.contains(entry.getKey())) {
                text = text.replace(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        if (changed) {
            for (int i = p.getRuns().size() - 1; i >= 0; i--) {
                p.removeRun(i);
            }
            XWPFRun newRun = p.createRun();
            newRun.setText(text);
        }
    }
}
