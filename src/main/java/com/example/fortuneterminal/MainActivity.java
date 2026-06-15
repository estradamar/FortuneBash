package com.example.fortuneterminal;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements CommandEngine.TerminalOutputCallback {

    private TextView terminalOutput;
    private EditText commandInput;
    private ScrollView scrollView;
    private CommandEngine commandEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        terminalOutput = findViewById(R.id.terminalOutput);
        commandInput = findViewById(R.id.commandInput);
        scrollView = findViewById(R.id.scrollView);

        terminalOutput.setMovementMethod(new ScrollingMovementMethod());
        terminalOutput.setBackgroundColor(Color.BLACK);
        terminalOutput.setTextColor(Color.GREEN);
        terminalOutput.setTextSize(14);

        commandEngine = new CommandEngine(this, this);
        terminalOutput.setTextColor(commandEngine.getCurrentTextColor());

        // Initial welcome message
        appendOutput(new SpannableString("this is not a real terminal, its just for fun. Type 'help' to see commands."));

        commandInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String command = commandInput.getText().toString();
                    if (!command.trim().isEmpty()) {
                        appendOutput(new SpannableString("user@android:~$ " + command));
                        commandInput.setText("");
                        commandEngine.processCommand(command);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onOutput(SpannableString text) {
        appendOutput(text);
    }

    @Override
    public void onClear() {
        terminalOutput.setText("");
    }

    @Override
    public void onColorChange(int color) {
        terminalOutput.setTextColor(color);
    }

    private void appendOutput(SpannableString text) {
        terminalOutput.append(text);
        terminalOutput.append("\n");
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
