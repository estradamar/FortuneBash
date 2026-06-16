package com.example.fortuneterminal;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.SpannableString;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class CommandEngine {

    private static final String PREFS_NAME = "FortuneTerminalPrefs";
    private static final String KEY_COLOR_CODE      = "colorCode";
    private static final String KEY_USERNAME        = "username";
    private static final String KEY_ACTIVE_CATEGORIES = "activeCategories";

    // Classic CMD color palette, index 0-15 maps to hex digits 0-F.
    private static final int[] CMD_COLORS = {
        0xFF000000, // 0 Black
        0xFF0000AA, // 1 Blue
        0xFF00AA00, // 2 Green
        0xFF00AAAA, // 3 Aqua
        0xFFAA0000, // 4 Red
        0xFFAA00AA, // 5 Purple
        0xFFAA5500, // 6 Yellow
        0xFFAAAAAA, // 7 White
        0xFF555555, // 8 Gray
        0xFF5555FF, // 9 Light Blue
        0xFF55FF55, // A Light Green
        0xFF55FFFF, // B Light Aqua
        0xFFFF5555, // C Light Red
        0xFFFF55FF, // D Light Purple
        0xFFFFFF55, // E Light Yellow
        0xFFFFFFFF  // F Bright White
    };

    private static final String[] CMD_COLOR_NAMES = {
        "Black", "Blue", "Green", "Aqua", "Red", "Purple",
        "Yellow", "White", "Gray", "Light Blue", "Light Green",
        "Light Aqua", "Light Red", "Light Purple", "Light Yellow", "Bright White"
    };

    public interface TerminalOutputCallback {
        void onOutput(SpannableString text);
        void onClear();
        void onColorsChange(int bgColor, int fgColor);
        void onPromptChange(String promptText);
    }

    private final Context context;
    private final FortuneManager fortuneManager;
    private final SharedPreferences prefs;
    private final TerminalOutputCallback callback;
    private final Random random;

    private int currentBgColor;
    private int currentFgColor;
    private String currentUsername;
    private Set<String> activeCategories;

    // State machine for offensive-category confirmation.
    private boolean awaitingConfirmation = false;
    private String pendingCategoryToggle = null;

    public CommandEngine(Context context, TerminalOutputCallback callback) {
        this.context       = context;
        this.callback      = callback;
        this.fortuneManager = new FortuneManager(context);
        this.prefs         = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.random        = new Random();
        loadPreferences();
    }

    // -------------------------------------------------------------------------
    // Preferences
    // -------------------------------------------------------------------------

    private void loadPreferences() {
        String colorCode = prefs.getString(KEY_COLOR_CODE, "0A");
        resolveColorCode(colorCode);

        currentUsername = prefs.getString(KEY_USERNAME, "guest");

        // getStringSet returns an unmodifiable live reference — copy it.
        Set<String> saved = prefs.getStringSet(KEY_ACTIVE_CATEGORIES, new HashSet<String>());
        activeCategories = new HashSet<>(saved);
        if (activeCategories.isEmpty()) {
            Set<String> defaultOff = new HashSet<>(java.util.Arrays.asList(
                    "debian", "goedel", "law", "miscellaneous", "perl", "platitudes"));
            for (String cat : fortuneManager.getAllCategories()) {
                if (!fortuneManager.isOffensiveCategory(cat) && !defaultOff.contains(cat)) {
                    activeCategories.add(cat);
                }
            }
            saveActiveCategories();
        }
    }

    private void resolveColorCode(String code) {
        if (code == null || code.length() != 2) return;
        int bgIdx = hexCharToIndex(code.charAt(0));
        int fgIdx = hexCharToIndex(code.charAt(1));
        if (bgIdx < 0 || fgIdx < 0) return;
        currentBgColor = CMD_COLORS[bgIdx];
        currentFgColor = CMD_COLORS[fgIdx];
    }

    private void saveColorCode(String code) {
        prefs.edit().putString(KEY_COLOR_CODE, code).apply();
    }

    private void saveUsername() {
        prefs.edit().putString(KEY_USERNAME, currentUsername).apply();
    }

    private void saveActiveCategories() {
        prefs.edit().putStringSet(KEY_ACTIVE_CATEGORIES, activeCategories).apply();
    }

    // -------------------------------------------------------------------------
    // Public accessors used by MainActivity for initial UI setup.
    // -------------------------------------------------------------------------

    public int getCurrentBgColor()  { return currentBgColor; }
    public int getCurrentFgColor()  { return currentFgColor; }
    public String getPromptString() { return currentUsername + "@android:~$ "; }

    // -------------------------------------------------------------------------
    // Command dispatch
    // -------------------------------------------------------------------------

    public void processCommand(String rawInput) {
        if (awaitingConfirmation) {
            handleConfirmation(rawInput);
            return;
        }

        String trimmed = rawInput.trim();
        int space = trimmed.indexOf(' ');
        String cmd  = (space >= 0 ? trimmed.substring(0, space) : trimmed).toLowerCase(Locale.US);
        String args = (space >= 0 ? trimmed.substring(space + 1).trim() : "");

        SpannableString result;
        switch (cmd) {
            case "help":       result = out(getHelpText());                                          break;
            case "fortune":    result = out(getFortune());                                           break;
            case "categories": result = out(getCategoriesList());                                    break;
            case "toggle":     result = cmdToggle(args.toLowerCase(Locale.US));                      break;
            case "color":      result = cmdColor(args);                                              break;
            case "login":      result = cmdLogin(args);                                              break;
            case "whoami":     result = out(currentUsername);                                        break;
            case "clear":      callback.onClear(); result = null;                                    break;
            case "yesorno":    result = out(random.nextBoolean() ? "yes" : "no");                   break;
            case "date":       result = out(new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",
                                   Locale.US).format(new Date()));                                   break;
            case "echo":       result = out(args);                                                   break;
            case "roll":       result = out(cmdRoll(args));                                          break;
            case "matrix":     result = out(generateMatrix());                                       break;
            case "sudo":       result = out("This incident will be reported.");                      break;
            default:           result = out("'" + cmd + "' is not recognized. Type 'help'.");        break;
        }

        if (result != null) {
            callback.onOutput(result);
        }
    }

    // -------------------------------------------------------------------------
    // Command implementations
    // -------------------------------------------------------------------------

    private String getHelpText() {
        return "Available commands:\n"
            + "  help                   - Display this help message.\n"
            + "  fortune                - Display a random fortune.\n"
            + "  categories             - List fortune categories and their status.\n"
            + "  toggle [category]      - Enable or disable a fortune category.\n"
            + "  toggle allon           - Turn all categories on.\n"
            + "  toggle alloff          - Turn all categories off.\n"
            + "  color [BG][FG]         - Set colors via CMD hex code (e.g., color 0A).\n"
            + "  color list             - Show all CMD color codes (0-F).\n"
            + "  login [name]           - Set the active terminal username.\n"
            + "  whoami                 - Display the current username.\n"
            + "  clear                  - Clear the terminal history.\n"
            + "  yesorno                - Random yes or no answer.\n"
            + "  date                   - Display current date and time.\n"
            + "  echo [text]            - Repeat the given text.\n"
            + "  roll [NdX]             - Roll dice (e.g., roll 2d6).\n"
            + "  matrix                 - Display a matrix rain effect.\n"
            + "  sudo [command]         - Superuser privileges (joke).";
    }

    private String getFortune() {
        if (activeCategories.isEmpty()) {
            return "No categories selected. Please select at least one category with 'toggle [category]' or 'toggle allon'.";
        }
        return fortuneManager.getRandomFortune(new ArrayList<>(activeCategories));
    }

    private String getCategoriesList() {
        List<String> all = fortuneManager.getAllCategories();
        if (all.isEmpty()) {
            return "No fortune categories found.\n\n"
                 + "Place plain-text fortune files inside the app's asset folders:\n"
                 + "  app/src/main/assets/datfiles/      <- safe categories\n"
                 + "  app/src/main/assets/datfiles/off/  <- offensive categories\n\n"
                 + "Each file should contain fortunes separated by a '%' on its own line.";
        }
        StringBuilder sb = new StringBuilder("Fortune Categories:\n");
        for (String cat : all) {
            sb.append(activeCategories.contains(cat) ? "  [ON]  " : "  [OFF] ");
            sb.append(cat);
            if (fortuneManager.isOffensiveCategory(cat)) {
                sb.append("  (offensive)");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    // color command -----------------------------------------------------------

    private SpannableString cmdColor(String args) {
        if (args.isEmpty()) {
            return out("Usage: color [BG][FG]  (e.g., color 0A)  or  color list");
        }

        if (args.equalsIgnoreCase("list")) {
            return out(buildColorList());
        }

        String code = args.toUpperCase(Locale.US);
        if (code.length() != 2) {
            return out("Error: color requires exactly 2 hex characters (e.g., color 0A).");
        }

        int bgIdx = hexCharToIndex(code.charAt(0));
        int fgIdx = hexCharToIndex(code.charAt(1));

        if (bgIdx < 0 || fgIdx < 0) {
            return out("Error: Invalid characters. Valid range is 0-9 and A-F.");
        }

        if (bgIdx == fgIdx) {
            return out("Error: Background and text colors cannot be the same.");
        }

        currentBgColor = CMD_COLORS[bgIdx];
        currentFgColor = CMD_COLORS[fgIdx];
        saveColorCode(code);
        callback.onColorsChange(currentBgColor, currentFgColor);

        return out("Colors set:  background=" + CMD_COLOR_NAMES[bgIdx]
                + "  text=" + CMD_COLOR_NAMES[fgIdx]);
    }

    private String buildColorList() {
        StringBuilder sb = new StringBuilder("CMD Color Attribute List:\n\n");
        for (int i = 0; i < CMD_COLORS.length; i++) {
            sb.append(String.format(Locale.US, "  %X  -  %s\n", i, CMD_COLOR_NAMES[i]));
        }
        sb.append("\nFirst char = Background, Second char = Text.\n");
        sb.append("Example: 'color 0A' = Black background, Light Green text.");
        return sb.toString();
    }

    // login command -----------------------------------------------------------

    private SpannableString cmdLogin(String args) {
        if (args.isEmpty()) {
            return out("Usage: login [name]");
        }
        // Accept only safe characters for a username.
        String name = args.split("\\s+")[0];
        if (!name.matches("[a-zA-Z0-9_-]+")) {
            return out("Error: Username may only contain letters, digits, underscores, or hyphens.");
        }
        currentUsername = name;
        saveUsername();
        callback.onPromptChange(getPromptString());
        return out("Logged in as: " + currentUsername);
    }

    // toggle command ----------------------------------------------------------

    private SpannableString cmdToggle(String args) {
        if (args.isEmpty()) {
            return out("Usage: toggle [category] | toggle allon | toggle alloff");
        }
        if (args.equals("allon")) {
            activeCategories.addAll(fortuneManager.getAllCategories());
            saveActiveCategories();
            return out("All " + fortuneManager.getAllCategories().size() + " categories are now ON.");
        }
        if (args.equals("alloff")) {
            activeCategories.clear();
            saveActiveCategories();
            return out("All categories are now OFF.");
        }
        if (!fortuneManager.getAllCategories().contains(args)) {
            return out("Category not found: '" + args + "'. Use 'categories' to list options.");
        }
        // Enabling an offensive category requires explicit confirmation.
        if (fortuneManager.isOffensiveCategory(args) && !activeCategories.contains(args)) {
            awaitingConfirmation = true;
            pendingCategoryToggle = args;
            return out("WARNING: This category may contain offensive material.\n"
                     + "Are you sure you want to enable it? (yes/no)");
        }
        toggleCategory(args);
        boolean nowOn = activeCategories.contains(args);
        return out("Category '" + args + "' is now " + (nowOn ? "ON" : "OFF") + ".");
    }

    private void handleConfirmation(String rawInput) {
        String answer = rawInput.trim().toLowerCase(Locale.US);
        if (answer.equals("yes")) {
            if (pendingCategoryToggle != null) {
                toggleCategory(pendingCategoryToggle);
                callback.onOutput(out("Category '" + pendingCategoryToggle + "' has been enabled."));
            }
            awaitingConfirmation = false;
            pendingCategoryToggle = null;
        } else if (answer.equals("no")) {
            callback.onOutput(out("Category toggle cancelled."));
            awaitingConfirmation = false;
            pendingCategoryToggle = null;
        } else {
            // Keep waiting — only "yes" or "no" are accepted.
            callback.onOutput(out("Please type 'yes' or 'no'."));
        }
    }

    private void toggleCategory(String category) {
        if (activeCategories.contains(category)) {
            activeCategories.remove(category);
        } else {
            activeCategories.add(category);
        }
        saveActiveCategories();
    }

    // roll command ------------------------------------------------------------

    private String cmdRoll(String args) {
        int numDice = 1;
        int numFaces = 6;

        if (!args.isEmpty()) {
            String[] parts = args.toLowerCase(Locale.US).split("d", 2);
            if (parts.length != 2) {
                return "Invalid format. Use NdX (e.g., 2d6).";
            }
            try {
                if (!parts[0].isEmpty()) numDice  = Integer.parseInt(parts[0].trim());
                if (!parts[1].isEmpty()) numFaces = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                return "Invalid format. Use NdX (e.g., 2d6).";
            }
        }

        if (numDice < 1 || numDice > 100 || numFaces < 2 || numFaces > 1000) {
            return "Dice must be 1-100 and faces must be 2-1000.";
        }

        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (int i = 0; i < numDice; i++) {
            int roll = random.nextInt(numFaces) + 1;
            total += roll;
            sb.append("Roll ").append(i + 1).append(": ").append(roll).append("\n");
        }
        sb.append("Total: ").append(total);
        return sb.toString();
    }

    // matrix command ----------------------------------------------------------

    private String generateMatrix() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@#$%&*";
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 38; col++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            if (row < 7) sb.append("\n");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SpannableString out(String text) {
        return new SpannableString(text != null ? text : "");
    }

    private int hexCharToIndex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }
}
