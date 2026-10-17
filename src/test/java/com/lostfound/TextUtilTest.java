package com.lostfound;

import com.lostfound.service.TextUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextUtilTest {

    @Test
    void levenshteinCountsSingleCharacterEdits() {
        assertEquals(0, TextUtil.levenshtein("bag", "bag"));
        assertEquals(1, TextUtil.levenshtein("wallet", "walet"));
        assertEquals(3, TextUtil.levenshtein("kitten", "sitting"));
    }

    @Test
    void fuzzyEqualsToleratesTyposInLongerWordsOnly() {
        assertTrue(TextUtil.fuzzyEquals("laptop", "lapto"));
        assertTrue(TextUtil.fuzzyEquals("wallet", "walet"));
        assertFalse(TextUtil.fuzzyEquals("bag", "bat"));   // short words must match exactly
    }

    @Test
    void diceIgnoresWordOrderAndStopWords() {
        assertEquals(1.0, TextUtil.dice("Black Dell laptop bag", "dell LAPTOP bag, black"), 0.0001);
        assertEquals(0.0, TextUtil.dice("red umbrella", "blue water bottle"), 0.0001);
    }
}
