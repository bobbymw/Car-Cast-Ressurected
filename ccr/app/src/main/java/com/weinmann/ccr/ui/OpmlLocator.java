package com.weinmann.ccr.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import com.weinmann.ccr.R;
import com.weinmann.ccr.core.Util;

import java.io.File;

public class OpmlLocator extends BaseActivity implements Runnable {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.opml_locator);

        final EditText pathEditText = findViewById(R.id.path);
        final Button button = findViewById(R.id.import_oiml_button);

        button.setOnClickListener(v -> {
            button.setEnabled(false);
            pathEditText.setEnabled(false);
            pathEditText.setInputType(InputType.TYPE_NULL);

            try {
                String text = pathEditText.getText().toString();
                if (!text.startsWith("/")){
                    if (!text.startsWith("http://") ||
                            !text.startsWith("https://") ){
                        text = "http://"+text;
                        pathEditText.setText(text);
                    }
                } else {
                    File file = new File(text);
                    if (!file.exists()) {
                        sorry("That file does not exist.");
                    } else if (!file.canRead()) {
                        sorry("That file cannot be read.");
                    }else{
                        Intent intent = new Intent(getApplicationContext(), OpmlImport.class);
                        intent.setData(Uri.fromFile(file));
                        startActivity(intent);
                    }
                }
            } catch(Throwable t){
                sorry(t.getMessage());
            }

        });
    }

    private void sorry(String message) {
        final EditText pathEditText = findViewById(R.id.path);
        final Button button = findViewById(R.id.import_oiml_button);

        button.setEnabled(true);
        pathEditText.setEnabled(true);
        pathEditText.setInputType(InputType.TYPE_CLASS_TEXT);

        Util.toast(this, "SORRY!\n\n"+message);
    }


    @Override
    public void run() {
        final EditText pathEditText = findViewById(R.id.path);
        String text = pathEditText.getText().toString();

        try {

            Intent intent = new Intent(getApplicationContext(), OpmlImport.class);
            intent.setData(Uri.parse(text));
            startActivity(intent);

        } catch(Throwable t){
            // on UI thread?
            sorry(t.getMessage());
        }
    }
}
