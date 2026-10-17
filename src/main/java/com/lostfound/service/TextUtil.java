package com.lostfound.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Small text helpers used for fuzzy search and item matching (no external libraries). */
public final class TextUtil {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "an", "and", "or", "of", "in", "on", "at", "to", "my", "is", "was", "it",
            "with", "for", "near", "lost", "found", "has", "have", "this", "that", "from", "by");

    private TextUtil() { }

    /** Lower-case words, punctuation and stop words removed. */
    public static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String t : text.toLowerCase().split("[^a-z0-9]+")) {
            if (t.length() > 1 && !STOP_WORDS.contains(t)) out.add(t);
        }
        return out;
    }

    /** Classic edit distance: number of single-character edits to turn a into b. */
    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }

    /** Two words are "the same" if equal, or close enough to be a typo (longer words tolerate more typos). */
    public static boolean fuzzyEquals(String a, String b) {
        if (a.equals(b)) return true;
        int longest = Math.max(a.length(), b.length());
        int allowed = longest >= 8 ? 2 : longest >= 4 ? 1 : 0;
        return allowed > 0 && levenshtein(a, b) <= allowed;
    }

    /** Number of words of A that have a (fuzzy) partner in B, each partner used once. */
    private static int matchedWords(List<String> a, List<String> b) {
        boolean[] used = new boolean[b.size()];
        int matches = 0;
        for (String wa : a) {
            for (int j = 0; j < b.size(); j++) {
                if (!used[j] && fuzzyEquals(wa, b.get(j))) {
                    used[j] = true;
                    matches++;
                    break;
                }
            }
        }
        return matches;
    }

    /** Dice similarity 0..1: 2*shared / (words in A + words in B). Good for short titles. */
    public static double dice(String x, String y) {
        List<String> a = tokens(x), b = tokens(y);
        if (a.isEmpty() || b.isEmpty()) return 0;
        return 2.0 * matchedWords(a, b) / (a.size() + b.size());
    }

    /** Overlap coefficient 0..1: shared / size of the smaller text. Good when one text is longer. */
    public static double overlap(String x, String y) {
        List<String> a = tokens(x), b = tokens(y);
        if (a.isEmpty() || b.isEmpty()) return 0;
        return (double) matchedWords(a, b) / Math.min(a.size(), b.size());
    }
}
