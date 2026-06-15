package com.example.fortuneterminal;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FortuneManager {

    private static final String TAG = "FortuneManager";

    // Matches a line containing only '%' (with optional trailing whitespace).
    // This avoids false splits when a fortune contains '%' inside a sentence.
    private static final String FORTUNE_DELIMITER = "(?m)^%\\s*$";

    private final Context context;
    private final Map<String, List<String>> fortunes      = new HashMap<>();
    private final List<String>              safeCategories = new ArrayList<>();
    private final List<String>              offCategories  = new ArrayList<>();

    public FortuneManager(Context context) {
        this.context = context;
        loadFortunes();
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    private void loadFortunes() {
        try {
            // Safe categories live directly inside assets/datfiles/
            String[] safeFiles = context.getAssets().list("datfiles");
            if (safeFiles != null) {
                for (String filename : safeFiles) {
                    if (filename.equals("off")) continue; // skip sub-directory entry
                    loadCategory("datfiles/" + filename, filename, false);
                    safeCategories.add(filename);
                }
            }

            // Offensive categories live inside assets/datfiles/off/
            String[] offFiles = context.getAssets().list("datfiles/off");
            if (offFiles != null) {
                for (String filename : offFiles) {
                    String catName = "off/" + filename;
                    loadCategory("datfiles/off/" + filename, catName, true);
                    offCategories.add(catName);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing fortune asset directories: " + e.getMessage());
        }
    }

    private void loadCategory(String assetPath, String categoryName, boolean isOffensive)
            throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(assetPath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }

        // Split on lines that are exactly '%' to avoid splitting inside fortune text.
        String[] raw = content.toString().split(FORTUNE_DELIMITER);
        List<String> entries = new ArrayList<>();
        for (String entry : raw) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                entries.add(trimmed);
            }
        }
        fortunes.put(categoryName, entries);
        Log.d(TAG, "Loaded " + entries.size() + " fortunes from: " + assetPath);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public String getRandomFortune(List<String> activeCategories) {
        List<String> pool = new ArrayList<>();
        for (String cat : activeCategories) {
            List<String> entries = fortunes.get(cat);
            if (entries != null) {
                pool.addAll(entries);
            }
        }
        if (pool.isEmpty()) {
            return "No fortunes available. Enable categories with 'toggle [category]'.";
        }
        return pool.get(new Random().nextInt(pool.size()));
    }

    public List<String> getAllCategories() {
        List<String> all = new ArrayList<>();
        all.addAll(safeCategories);
        all.addAll(offCategories);
        return all;
    }

    public boolean isOffensiveCategory(String categoryName) {
        return offCategories.contains(categoryName);
    }
}
