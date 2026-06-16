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
                    if (isAssetDirectory("datfiles/" + filename)) continue;
                    if (tryLoadCategory("datfiles/" + filename, filename)) {
                        safeCategories.add(filename);
                    }
                }
            }

            // Pool every file in assets/datfiles/off/ into one single "ofensivo" category
            List<String> offPool = new ArrayList<>();
            String[] offFiles = context.getAssets().list("datfiles/off");
            if (offFiles != null) {
                for (String filename : offFiles) {
                    if (isAssetDirectory("datfiles/off/" + filename)) continue;
                    try {
                        List<String> entries = readFortuneEntries("datfiles/off/" + filename);
                        offPool.addAll(entries);
                        Log.d(TAG, "Pooled " + entries.size() + " fortunes from: datfiles/off/" + filename);
                    } catch (IOException e) {
                        Log.w(TAG, "Could not load 'datfiles/off/" + filename + "': " + e.getMessage());
                    }
                }
            }
            if (!offPool.isEmpty()) {
                fortunes.put("ofensive", offPool);
                offCategories.add("ofensive");
            }

            if (safeCategories.isEmpty() && offCategories.isEmpty()) {
                Log.w(TAG, "No fortune files found. Place plain-text .dat files in:\n"
                        + "  assets/datfiles/       (safe)\n"
                        + "  assets/datfiles/off/   (offensive)");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing fortune asset directories: " + e.getMessage());
        }
    }

    private boolean isAssetDirectory(String assetPath) {
        try {
            String[] entries = context.getAssets().list(assetPath);
            return entries != null && entries.length > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean tryLoadCategory(String assetPath, String categoryName) {
        try {
            loadCategory(assetPath, categoryName, false);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Could not load '" + assetPath + "': " + e.getMessage());
            return false;
        }
    }

    private List<String> readFortuneEntries(String assetPath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(assetPath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        String[] raw = content.toString().split(FORTUNE_DELIMITER);
        List<String> entries = new ArrayList<>();
        for (String entry : raw) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) entries.add(trimmed);
        }
        return entries;
    }

    private void loadCategory(String assetPath, String categoryName, boolean isOffensive)
            throws IOException {
        List<String> entries = readFortuneEntries(assetPath);
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
