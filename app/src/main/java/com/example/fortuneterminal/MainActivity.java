package com.example.fortuneterminal;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements CommandEngine.TerminalOutputCallback {

    private RelativeLayout rootLayout;
    private ScrollView     scrollView;
    private TextView       terminalOutput;
    private TextView       promptText;
    private EditText       commandInput;
    private CommandEngine  commandEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout     = findViewById(R.id.rootLayout);
        scrollView     = findViewById(R.id.scrollView);
        terminalOutput = findViewById(R.id.terminalOutput);
        promptText     = findViewById(R.id.promptText);
        commandInput   = findViewById(R.id.commandInput);

        commandEngine = new CommandEngine(this, this);

        // Apply persisted color scheme and username before rendering anything.
        applyColors(commandEngine.getCurrentBgColor(), commandEngine.getCurrentFgColor());
        promptText.setText(commandEngine.getPromptString());

        appendLine("FortuneBash v1.0  --  Not a real terminal, just for fun.");
        appendLine("Type 'help' for a list of available commands.");

        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean submit = (actionId == EditorInfo.IME_ACTION_DONE)
                || (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN);

            if (submit) {
                String input = commandInput.getText().toString();
                if (!input.trim().isEmpty()) {
                    // Echo the command with the current prompt prefix.
                    appendLine(commandEngine.getPromptString() + input);
                    commandInput.setText("");
                    commandEngine.processCommand(input);
                }
                // Keep keyboard visible and EditText focused for consecutive commands.
                commandInput.post(() -> {
                    commandInput.requestFocus();
                    InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(commandInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                return true;
            }
            return false;
        });

        // Tapping the terminal area returns focus to the input field.
        terminalOutput.setOnClickListener(v -> {
            commandInput.requestFocus();
            InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commandInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    // -------------------------------------------------------------------------
    // TerminalOutputCallback
    // -------------------------------------------------------------------------

    @Override
    public void onOutput(SpannableString text) {
        appendLine(text);
    }

    @Override
    public void onClear() {
        terminalOutput.setText("");
    }

    @Override
    public void onColorsChange(int bgColor, int fgColor) {
        applyColors(bgColor, fgColor);
    }

    @Override
    public void onPromptChange(String prompt) {
        promptText.setText(prompt);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void applyColors(int bgColor, int fgColor) {
        rootLayout.setBackgroundColor(bgColor);
        // TextView and EditText backgrounds are transparent in the XML,
        // so the root background shows through. Only text colors need updating.
        terminalOutput.setTextColor(fgColor);
        promptText.setTextColor(fgColor);
        commandInput.setTextColor(fgColor);
        commandInput.setHintTextColor(Color.argb(128,
            Color.red(fgColor), Color.green(fgColor), Color.blue(fgColor)));
    }

    private void appendLine(CharSequence text) {
        terminalOutput.append(text);
        terminalOutput.append("\n");
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}
