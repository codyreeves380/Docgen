package com.yourname.docgen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.*;
import org.apache.poi.xwpf.usermodel.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_DOCX_FILE = 1;
    
    private EditText inputDefendant, inputCauseNo, inputDate, inputDefId;
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
        btnSelectFile = findViewById(R.id.btn_select_file);
        btnGenerate = findViewById(R.id.btn_generate);
        txtSelectedFile = findViewById(R.id.txt_selected_file);
        txtStatus = findViewById(R.id.txt_status);

        // Launches system file picker filtering for docx formats
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

    // Handles the file selected from the file manager
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
        replacements.put("[[DEPOSIT]]", inputDefId.getText().toString().trim());
        replacements.put("[[DAY]]", inputDefId.getText().toString().trim());
        replacements.put("[[MONTH]]", inputDefId.getText().toString().trim());
        replacements.put("[[YEAR]]", inputDefId.getText().toString().trim());

        if(replacements.get("[[DEFENDANT]]").isEmpty() || replacements.get("[[CAUSENO]]").isEmpty()) {
            txtStatus.setText("Status: Please enter Defendant and Cause No!");
            return;
        }

        try {
            // Read chosen template using ContentResolver stream
            InputStream is = getContentResolver().openInputStream(selectedFileUri);
            XWPFDocument doc = new XWPFDocument(is);

            // Process text blocks
            for (XWPFParagraph p : doc.getParagraphs()) {
                replaceTags(p, replacements);
            }

            // Process table grids
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph p : cell.getParagraphs()) {
                            replaceTags(p, replacements);
                        }
                    }
                }
            }

            // Write output cleanly to public Downloads folder 
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
