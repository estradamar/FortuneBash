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
    private static final String FORTUNE_DELIMITER = "%\n";

    private final Context context;
    private final Map<String, List<String>> fortunes = new HashMap<>();
    private final List<String> safeCategories = new ArrayList<>();
    private final List<String> offCategories = new ArrayList<>();

    public FortuneManager(Context context) {
        this.context = context;
        loadFortunes();
    }

    private void loadFortunes() {
        try {
            // Load safe categories
            String[] safeFiles = context.getAssets().list("datfiles");
            if (safeFiles != null) {
                for (String filename : safeFiles) {
                    if (!filename.equals("off")) { // Exclude the 'off' directory itself
                        loadCategory("datfiles/" + filename, filename, false);
                        safeCategories.add(filename);
                    }
                }
            }

            // Load 'off' categories
            String[] offFiles = context.getAssets().list("datfiles/off");
            if (offFiles != null) {
                for (String filename : offFiles) {
                    loadCategory("datfiles/off/" + filename, "off/" + filename, true);
                    offCategories.add("off/" + filename);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading fortune files: " + e.getMessage());
        }
    }

    private void loadCategory(String assetPath, String categoryName, boolean isOffensive) throws IOException {
        List<String> categoryFortunes = new ArrayList<>();
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(assetPath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        }
        String[] rawFortunes = fileContent.toString().split(FORTUNE_DELIMITER);
        for (String fortune : rawFortunes) {
            String trimmedFortune = fortune.trim();
            if (!trimmedFortune.isEmpty()) {
                categoryFortunes.add(trimmedFortune);
            }
        }
        fortunes.put(categoryName, categoryFortunes);
        Log.d(TAG, "Loaded " + categoryFortunes.size() + " fortunes for category: " + categoryName);
    }

    public String getRandomFortune(List<String> activeCategories) {
        List<String> availableFortunes = new ArrayList<>();
        for (String category : activeCategories) {
            if (fortunes.containsKey(category)) {
                availableFortunes.addAll(fortunes.get(category));
            }
        }

        if (availableFortunes.isEmpty()) {
            return "No active categories with fortunes found. Try enabling some categories.";
        }

        Random random = new Random();
        return availableFortunes.get(random.nextInt(availableFortunes.size()));
    }

    public List<String> getAllCategories() {
        List<String> allCategories = new ArrayList<>();
        allCategories.addAll(safeCategories);
        allCategories.addAll(offCategories);
        return allCategories;
    }

    public boolean isOffensiveCategory(String categoryName) {
        return offCategories.contains(categoryName);
    }
}
