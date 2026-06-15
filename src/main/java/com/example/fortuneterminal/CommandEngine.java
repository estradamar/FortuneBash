package com.example.fortuneterminal;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CommandEngine {

    private static final String PREFS_NAME = "FortuneTerminalPrefs";
    private static final String KEY_TEXT_COLOR = "textColor";
    private static final String KEY_ACTIVE_CATEGORIES = "activeCategories";

    private final Context context;
    private final FortuneManager fortuneManager;
    private final SharedPreferences sharedPreferences;
    private final Random random;

    private boolean awaitingConfirmation = false;
    private String pendingCategoryToggle = null;

    public interface TerminalOutputCallback {
        void onOutput(SpannableString text);
        void onClear();
        void onColorChange(int color);
    }

    private TerminalOutputCallback callback;

    public CommandEngine(Context context, TerminalOutputCallback callback) {
        this.context = context;
        this.callback = callback;
        this.fortuneManager = new FortuneManager(context);
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.random = new Random();
        loadPreferences();
    }

    private int currentTextColor;
    private Set<String> activeCategories;

    private void loadPreferences() {
        currentTextColor = sharedPreferences.getInt(KEY_TEXT_COLOR, Color.GREEN);
        activeCategories = sharedPreferences.getStringSet(KEY_ACTIVE_CATEGORIES, new HashSet<>());
        if (activeCategories.isEmpty()) {
            // Activate all safe categories by default if none are set
            for (String category : fortuneManager.getAllCategories()) {
                if (!fortuneManager.isOffensiveCategory(category)) {
                    activeCategories.add(category);
                }
            }
            saveActiveCategories();
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TEXT_COLOR, currentTextColor);
        editor.putStringSet(KEY_ACTIVE_CATEGORIES, activeCategories);
        editor.apply();
    }

    private void saveActiveCategories() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_ACTIVE_CATEGORIES, activeCategories);
        editor.apply();
    }

    public int getCurrentTextColor() {
        return currentTextColor;
    }

    public void processCommand(String input) {
        SpannableString output;
        if (awaitingConfirmation) {
            handleConfirmation(input);
            return;
        }

        String[] parts = input.trim().toLowerCase(Locale.US).split("\\s+", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "help":
                output = formatOutput(getHelpMessage());
                break;
            case "fortune":
                output = formatOutput(fortuneManager.getRandomFortune(new ArrayList<>(activeCategories)));
                break;
            case "categories":
                output = formatOutput(getCategoriesList());
                break;
            case "toggle":
                output = handleToggleCommand(args);
                break;
            case "color":
                output = handleColorCommand(args);
                break;
            case "clear":
                callback.onClear();
                output = null; // No text output for clear command
                break;
            case "yesorno":
                output = formatOutput(random.nextBoolean() ? "yes" : "no");
                break;
            case "whoami":
                output = formatOutput("iron_lion_admin"); // Default response
                break;
            case "date":
                output = formatOutput(new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US).format(new Date()));
                break;
            case "echo":
                output = formatOutput(args);
                break;
            case "roll":
                output = formatOutput(handleRollCommand(args));
                break;
            case "matrix":
                output = formatOutput(generateMatrixEffect());
                break;
            case "sudo":
                output = formatOutput("This incident will be reported.");
                break;
            default:
                output = formatOutput("Command not found: " + command + ". Type 'help' to see commands.");
                break;
        }
        if (output != null) {
            callback.onOutput(output);
        }
    }

    private SpannableString formatOutput(String text) {
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(currentTextColor), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private String getHelpMessage() {
        return "Available commands:\n"
                + "  help                   - Display this help message.\n"
                + "  fortune                - Get a random fortune message.\n"
                + "  categories             - List all fortune categories and their status.\n"
                + "  toggle [category]      - Toggle a fortune category on or off.\n"
                + "  color [hex/name]       - Change terminal text color (e.g., #RRGGBB, matrix, hacker).\n"
                + "  clear                  - Clear the terminal history.\n"
                + "  yesorno                - Get a random 'yes' or 'no'.\n"
                + "  whoami                 - Display current user.\n"
                + "  date                   - Display current date and time.\n"
                + "  echo [text]            - Repeat the input text.\n"
                + "  roll [NdX]             - Roll N dice with X faces (e.g., roll 2d6, roll for 1d6).\n"
                + "  matrix                 - Simulate matrix digital rain effect.\n"
                + "  sudo [command]         - Execute a command with superuser privileges (mostly for fun).";
    }

    private String getCategoriesList() {
        StringBuilder sb = new StringBuilder("Fortune Categories:\n");
        List<String> allCategories = fortuneManager.getAllCategories();
        for (String category : allCategories) {
            sb.append(activeCategories.contains(category) ? "[X] " : "[ ] ");
            sb.append(category);
            if (fortuneManager.isOffensiveCategory(category)) {
                sb.append(" (offensive)");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private SpannableString handleToggleCommand(String args) {
        if (args.isEmpty()) {
            return formatOutput("Usage: toggle [category]");
        }

        String categoryToToggle = args;
        if (!fortuneManager.getAllCategories().contains(categoryToToggle)) {
            return formatOutput("Category not found: " + categoryToToggle);
        }

        if (fortuneManager.isOffensiveCategory(categoryToToggle) && !activeCategories.contains(categoryToToggle)) {
            awaitingConfirmation = true;
            pendingCategoryToggle = categoryToToggle;
            return formatOutput("WARNING: This category may contain offensive material. Are you sure you want to enable it? (yes/no)");
        } else {
            toggleCategory(categoryToToggle);
            return formatOutput("Category '" + categoryToToggle + "' toggled.");
        }
    }

    private void handleConfirmation(String input) {
        if (input.trim().equalsIgnoreCase("yes")) {
            if (pendingCategoryToggle != null) {
                toggleCategory(pendingCategoryToggle);
                callback.onOutput(formatOutput("Category '" + pendingCategoryToggle + "' enabled."));
            }
        } else {
            callback.onOutput(formatOutput("Category toggle cancelled."));
        }
        awaitingConfirmation = false;
        pendingCategoryToggle = null;
    }

    private void toggleCategory(String category) {
        if (activeCategories.contains(category)) {
            activeCategories.remove(category);
        } else {
            activeCategories.add(category);
        }
        saveActiveCategories();
    }

    private SpannableString handleColorCommand(String args) {
        if (args.isEmpty()) {
            return formatOutput("Usage: color [hex/name]");
        }

        int newColor;
        switch (args.toLowerCase(Locale.US)) {
            case "matrix":
                newColor = Color.parseColor("#00FF00"); // Bright green
                break;
            case "hacker":
                newColor = Color.parseColor("#33FF33"); // Lighter green
                break;
            case "red":
                newColor = Color.RED;
                break;
            case "blue":
                newColor = Color.BLUE;
                break;
            case "white":
                newColor = Color.WHITE;
                break;
            case "yellow":
                newColor = Color.YELLOW;
                break;
            default:
                try {
                    newColor = Color.parseColor(args);
                } catch (IllegalArgumentException e) {
                    return formatOutput("Invalid color format. Use #RRGGBB or names like 'matrix', 'hacker'.");
                }
                break;
        }
        currentTextColor = newColor;
        savePreferences();
        callback.onColorChange(newColor);
        return formatOutput("Text color changed.");
    }

    private String handleRollCommand(String args) {
        int numDice = 1;
        int numFaces = 6;

        if (!args.isEmpty()) {
            String[] rollParts = args.split("d");
            try {
                if (rollParts.length == 2) {
                    numDice = Integer.parseInt(rollParts[0]);
                    numFaces = Integer.parseInt(rollParts[1]);
                } else if (rollParts.length == 1) {
                    // If only 'Nd' or 'dM' is provided, assume 1 die or 6 faces respectively
                    if (args.startsWith("d")) {
                        numFaces = Integer.parseInt(rollParts[0]);
                    } else {
                        numDice = Integer.parseInt(rollParts[0]);
                    }
                }
            } catch (NumberFormatException e) {
                return "Invalid roll format. Use NdX (e.g., 2d6) or just 'roll'.";
            }
        }

        if (numDice <= 0 || numFaces <= 0) {
            return "Number of dice and faces must be positive.";
        }

        StringBuilder result = new StringBuilder();
        int total = 0;
        for (int i = 0; i < numDice; i++) {
            int roll = random.nextInt(numFaces) + 1;
            total += roll;
            result.append("Roll ").append(i + 1).append(": ").append(roll).append("\n");
        }
        result.append("Total: ").append(total);
        return result.toString();
    }

    private String generateMatrixEffect() {
        StringBuilder matrix = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 40; j++) { // 40 characters per line
                matrix.append(random.nextInt(2)); // 0 or 1
            }
            matrix.append("\n");
        }
        return matrix.toString();
    }
}
