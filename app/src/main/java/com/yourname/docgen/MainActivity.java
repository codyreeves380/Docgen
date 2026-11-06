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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_DOCX_FILE = 1;
    private static final String PREFS_NAME = "saved_form_inputs";

    private EditText inputDefendant, inputCauseNo, inputDate, inputDefId;
    private EditText inputDeposit, inputDay, inputYear, inputMonth;
    private EditText inputComplianceOfficer, inputPhone, inputEmail, inputBadge;
    private EditText inputGps, inputProgram, inputTax, inputJudge;

    private Button btnSelectFile, btnGenerate;
    private TextView txtSelectedFile, txtStatus;
    private Uri selectedFileUri = null;

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

        // Restore and save each field based on its checkbox.
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
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(
                        Intent.createChooser(intent, "Select Template File"),
                        PICK_DOCX_FILE);
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

        final SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Restore the saved value and checked state.
        if (prefs.contains(key)) {
            input.setText(prefs.getString(key, ""));
            saveCheckbox.setChecked(true);
        } else {
            saveCheckbox.setChecked(false);
        }

        // Save the current value when checked; delete it when unchecked.
        saveCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                prefs.edit()
                        .putString(key, input.getText().toString())
                        .apply();
            } else {
                prefs.edit()
                        .remove(key)
                        .apply();
            }
        });

        // Keep the stored value updated as the user types.
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                if (saveCheckbox.isChecked()) {
                    prefs.edit()
                            .putString(key, s.toString())
                            .apply();
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

        if (requestCode == PICK_DOCX_FILE
                && resultCode == RESULT_OK
                && data != null) {
            selectedFileUri = data.getData();
            txtSelectedFile.setText(
                    "Selected: " + selectedFileUri.getLastPathSegment());
            txtStatus.setText("Status: Template Loaded.");
        }
    }

    private void runDocumentGeneration() {
        if (selectedFileUri == null) {
            txtStatus.setText("Status: Please select a template file first!");
            return;
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("[[DEFENDANT]]",
                inputDefendant.getText().toString().trim());
        replacements.put("[[CAUSENO]]",
                inputCauseNo.getText().toString().trim());
        replacements.put("[[DATE]]",
                inputDate.getText().toString().trim());
        replacements.put("[[DEFID]]",
                inputDefId.getText().toString().trim());
        replacements.put("[[DEPOSIT]]",
                inputDeposit.getText().toString().trim());
        replacements.put("[[DAY]]",
                inputDay.getText().toString().trim());
        replacements.put("[[MONTH]]",
                inputMonth.getText().toString().trim());
        replacements.put("[[YEAR]]",
                inputYear.getText().toString().trim());

        replacements.put("[[CO]]",
                inputComplianceOfficer.getText().toString().trim());
        replacements.put("[[PHONE]]",
                inputPhone.getText().toString().trim());
        replacements.put("[[EMAIL]]",
                inputEmail.getText().toString().trim());
        replacements.put("[[BADGE]]",
                inputBadge.getText().toString().trim());
        replacements.put("[[GPS]]",
                inputGps.getText().toString().trim());
        replacements.put("[[PRO]]",
                inputProgram.getText().toString().trim());
        replacements.put("[[TAX]]",
                inputTax.getText().toString().trim());
        replacements.put("[[JUDGE]]",
                inputJudge.getText().toString().trim());

        if (replacements.get("[[DEFENDANT]]").isEmpty()
                || replacements.get("[[CAUSENO]]").isEmpty()) {
            txtStatus.setText("Status: Please enter Defendant and Cause No!");
            return;
        }

        File outputDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outputDir == null) {
            txtStatus.setText("Status: Could not access the Documents folder.");
            return;
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            txtStatus.setText("Status: Could not create the Documents folder.");
            return;
        }

        File outputFile = new File(
                outputDir,
                "Generated_" + System.currentTimeMillis() + ".docx");

        try (InputStream input = getContentResolver()
                     .openInputStream(selectedFileUri)) {
            if (input == null) {
                txtStatus.setText("Status: Could not open the selected template.");
                return;
            }

            try (XWPFDocument document = new XWPFDocument(input)) {
                replaceInParagraphs(document.getParagraphs(), replacements);

                for (XWPFTable table : document.getTables()) {
                    replaceInTable(table, replacements);
                }

                try (FileOutputStream output = new FileOutputStream(outputFile)) {
                    document.write(output);
                }
            }

            txtStatus.setText("Status: Generated " + outputFile.getName());
        } catch (Exception e) {
            txtStatus.setText("Status: Document generation failed: "
                    + e.getMessage());
        }
    }

    private void replaceInParagraphs(
            Iterable<XWPFParagraph> paragraphs,
            Map<String, String> replacements) {
        for (XWPFParagraph paragraph : paragraphs) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs.isEmpty()) {
                continue;
            }

            StringBuilder paragraphText = new StringBuilder();
            for (XWPFRun run : runs) {
                String runText = run.getText(0);
                if (runText != null) {
                    paragraphText.append(runText);
                }
            }

            String updatedText = paragraphText.toString();
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                updatedText = updatedText.replace(
                        replacement.getKey(), replacement.getValue());
            }

            if (!updatedText.equals(paragraphText.toString())) {
                runs.get(0).setText(updatedText, 0);
                for (int i = 1; i < runs.size(); i++) {
                    runs.get(i).setText("", 0);
                }
            }
        }
    }

    private void replaceInTable(
            XWPFTable table,
            Map<String, String> replacements) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                replaceInParagraphs(cell.getParagraphs(), replacements);
                for (XWPFTable nestedTable : cell.getTables()) {
                    replaceInTable(nestedTable, replacements);
                }
            }
        }
    }
}
