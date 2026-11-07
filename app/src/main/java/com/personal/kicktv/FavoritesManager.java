package com.personal.kicktv;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {

    private static final String PREF_NAME = "kicktv_favorites";
    private static final String KEY_FAVORITES = "channels";

    private final SharedPreferences prefs;

    public FavoritesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getFavorites() {
        Set<String> set = prefs.getStringSet(KEY_FAVORITES, null);
        if (set == null || set.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>(set);
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    public boolean addFavorite(String channel) {
        if (channel == null || channel.trim().isEmpty()) {
            return false;
        }
        String clean = cleanChannelName(channel);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        boolean added = set.add(clean);
        if (added) {
            prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
        }
        return added;
    }

    public boolean removeFavorite(String channel) {
        if (channel == null) return false;
        String clean = cleanChannelName(channel);
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
        boolean removed = set.remove(clean);
        if (removed) {
            prefs.edit().putStringSet(KEY_FAVORITES, set).apply();
        }
        return removed;
    }

    public boolean isFavorite(String channel) {
        if (channel == null) return false;
        String clean = cleanChannelName(channel);
        Set<String> set = prefs.getStringSet(KEY_FAVORITES, null);
        return set != null && set.contains(clean);
    }

    public static String cleanChannelName(String input) {
        String clean = input.trim().toLowerCase();
        if (clean.startsWith("https://")) clean = clean.substring(8);
        if (clean.startsWith("http://")) clean = clean.substring(7);
        if (clean.startsWith("kick.com/")) clean = clean.substring(9);
        if (clean.startsWith("/")) clean = clean.substring(1);
        int slashIdx = clean.indexOf('/');
        if (slashIdx != -1) {
            clean = clean.substring(0, slashIdx);
        }
        int queryIdx = clean.indexOf('?');
        if (queryIdx != -1) {
            clean = clean.substring(0, queryIdx);
        }
        return clean;
    }
}
