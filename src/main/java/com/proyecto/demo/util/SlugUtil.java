package com.proyecto.demo.util;

import java.text.Normalizer;
import java.util.Locale;

public class SlugUtil {

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Normalize the string to remove accents and special characters
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert to lowercase, replace spaces and special characters with hyphens
        String slug = normalized.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\s-]", "") // Remove invalid characters
                .replaceAll("\s+", "-")          // Replace spaces with hyphens
                .replaceAll("-+", "-")           // Merge multiple hyphens
                .replaceAll("^-|-$", "");        // Trim hyphens from start and end

        // Truncate to 100 characters
        return slug.length() > 100 ? slug.substring(0, 100) : slug;
    }
}