package com.dgi.platform.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rule-based bidirectional Arabic ↔ Latin transliteration for Tunisian names.
 *
 * Approach
 * --------
 * Rather than a static dictionary (Mohamed = محمد), this service encodes
 * phonetic mapping rules that cover ALL Arabic letters and their common Latin
 * representations in Tunisian administrative documents (following the French
 * AAPC/DEC romanization conventions used on Tunisian CINs).
 *
 * The comparison strategy:
 *  1. Normalize both sides (diacritics, accents, case) via TextNormalizationService.
 *  2. Transliterate the Arabic string to Latin using the phonetic map.
 *  3. Transliterate the Latin string to an Arabic-equivalent string using the
 *     reverse map.
 *  4. Compare normalized-Latin ↔ transliterated-to-Latin  AND
 *     normalized-Arabic ↔ reverse-transliterated-to-Arabic  (whichever pair
 *     yields a higher similarity score is used).
 *
 * This means محمد → MUHAMMED / MOHAMED / MOUHAMED are all near-matches,
 * and بن علي → BEN ALI / BEN ALY are high-confidence matches.
 *
 * The transliteration table is intentionally permissive (maps to the most
 * common Latin form) so that similarity scoring handles the remaining variation.
 */
@Service
public class ArabicLatinTransliterator {

    /**
     * Arabic letter → most common Latin representation in Tunisian admin documents.
     * Ordered longest-match-first where multi-char sequences are involved.
     * Using LinkedHashMap to preserve insertion order for replacement priority.
     */
    private static final Map<String, String> ARABIC_TO_LATIN = new LinkedHashMap<>();

    /**
     * Latin digraph/letter → canonical Arabic letter.
     * Used for reverse transliteration (Latin input → Arabic-equivalent for comparison).
     */
    private static final Map<String, String> LATIN_TO_ARABIC = new LinkedHashMap<>();

    static {
        // ── Multi-character Arabic sequences first ────────────────────────
        // Lam-Alef ligature forms
        ARABIC_TO_LATIN.put("\u0644\u0627", "LA");   // لا
        ARABIC_TO_LATIN.put("\u0644\u0623", "LA");   // لأ
        ARABIC_TO_LATIN.put("\u0644\u0625", "LI");   // لإ
        ARABIC_TO_LATIN.put("\u0644\u0622", "LA");   // لآ

        // ── Single Arabic letters ─────────────────────────────────────────
        ARABIC_TO_LATIN.put("\u0627", "A");    // ا alef
        ARABIC_TO_LATIN.put("\u0628", "B");    // ب ba
        ARABIC_TO_LATIN.put("\u062A", "T");    // ت ta
        ARABIC_TO_LATIN.put("\u062B", "TH");   // ث tha (often written S or T in Tunisian docs)
        ARABIC_TO_LATIN.put("\u062C", "J");    // ج jim
        ARABIC_TO_LATIN.put("\u062D", "H");    // ح ha (pharyngeal)
        ARABIC_TO_LATIN.put("\u062E", "KH");   // خ kha
        ARABIC_TO_LATIN.put("\u062F", "D");    // د dal
        ARABIC_TO_LATIN.put("\u0630", "TH");   // ذ dhal
        ARABIC_TO_LATIN.put("\u0631", "R");    // ر ra
        ARABIC_TO_LATIN.put("\u0632", "Z");    // ز zay
        ARABIC_TO_LATIN.put("\u0633", "S");    // س sin
        ARABIC_TO_LATIN.put("\u0634", "CH");   // ش shin → CH in French convention
        ARABIC_TO_LATIN.put("\u0635", "S");    // ص sad (emphatic)
        ARABIC_TO_LATIN.put("\u0636", "D");    // ض dad (emphatic)
        ARABIC_TO_LATIN.put("\u0637", "T");    // ط ta (emphatic)
        ARABIC_TO_LATIN.put("\u0638", "TH");   // ظ dha (emphatic)
        ARABIC_TO_LATIN.put("\u0639", "A");    // ع ain → often omitted or A
        ARABIC_TO_LATIN.put("\u063A", "GH");   // غ ghain → GH
        ARABIC_TO_LATIN.put("\u0641", "F");    // ف fa
        ARABIC_TO_LATIN.put("\u0642", "K");    // ق qaf → K in Tunisian dialect
        ARABIC_TO_LATIN.put("\u0643", "K");    // ك kaf
        ARABIC_TO_LATIN.put("\u0644", "L");    // ل lam
        ARABIC_TO_LATIN.put("\u0645", "M");    // م mim
        ARABIC_TO_LATIN.put("\u0646", "N");    // ن nun
        ARABIC_TO_LATIN.put("\u0647", "H");    // ه heh
        ARABIC_TO_LATIN.put("\u0648", "OU");   // و waw → OU in French convention
        ARABIC_TO_LATIN.put("\u064A", "I");    // ي yeh → I
        ARABIC_TO_LATIN.put("\u0629", "A");    // ة teh marbuta → A (end of word)
        ARABIC_TO_LATIN.put("\u0649", "A");    // ى alef maqsura → A
        // Hamza forms
        ARABIC_TO_LATIN.put("\u0621", "");     // ء hamza → silent
        ARABIC_TO_LATIN.put("\u0623", "A");    // أ
        ARABIC_TO_LATIN.put("\u0625", "I");    // إ
        ARABIC_TO_LATIN.put("\u0626", "I");    // ئ
        ARABIC_TO_LATIN.put("\u0624", "OU");   // ؤ

        // ── Latin → Arabic reverse map (for reverse transliteration) ─────
        // Digraphs before single chars — order matters
        LATIN_TO_ARABIC.put("CH", "\u0634");   // CH → ش
        LATIN_TO_ARABIC.put("KH", "\u062E");   // KH → خ
        LATIN_TO_ARABIC.put("GH", "\u063A");   // GH → غ
        LATIN_TO_ARABIC.put("TH", "\u062B");   // TH → ث
        LATIN_TO_ARABIC.put("OU", "\u0648");   // OU → و
        LATIN_TO_ARABIC.put("OI", "\u0648");   // OI → و (variant)
        // Single chars
        LATIN_TO_ARABIC.put("A", "\u0627");    // A → ا
        LATIN_TO_ARABIC.put("B", "\u0628");
        LATIN_TO_ARABIC.put("T", "\u062A");
        LATIN_TO_ARABIC.put("J", "\u062C");
        LATIN_TO_ARABIC.put("H", "\u062D");
        LATIN_TO_ARABIC.put("D", "\u062F");
        LATIN_TO_ARABIC.put("R", "\u0631");
        LATIN_TO_ARABIC.put("Z", "\u0632");
        LATIN_TO_ARABIC.put("S", "\u0633");
        LATIN_TO_ARABIC.put("F", "\u0641");
        LATIN_TO_ARABIC.put("K", "\u0643");
        LATIN_TO_ARABIC.put("L", "\u0644");
        LATIN_TO_ARABIC.put("M", "\u0645");
        LATIN_TO_ARABIC.put("N", "\u0646");
        LATIN_TO_ARABIC.put("I", "\u064A");
        LATIN_TO_ARABIC.put("E", "\u0647");    // E → ه (schwa)
        LATIN_TO_ARABIC.put("O", "\u0648");    // O → و
        LATIN_TO_ARABIC.put("U", "\u0648");    // U → و
        LATIN_TO_ARABIC.put("W", "\u0648");    // W → و
        LATIN_TO_ARABIC.put("Y", "\u064A");    // Y → ي
        LATIN_TO_ARABIC.put("G", "\u062C");    // G → ج (Tunisian convention)
        LATIN_TO_ARABIC.put("C", "\u0643");    // C → ك
        LATIN_TO_ARABIC.put("P", "\u0628");    // P → ب (no /p/ in Arabic)
        LATIN_TO_ARABIC.put("V", "\u0641");    // V → ف
        LATIN_TO_ARABIC.put("X", "\u0643\u0633"); // X → كس
        LATIN_TO_ARABIC.put("Q", "\u0642");    // Q → ق
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transliterate an Arabic string to its Latin phonetic equivalent using
     * Tunisian French administrative conventions.
     *
     * @param arabic normalized Arabic string (no diacritics, normalized letters)
     * @return Latin representation, uppercase
     */
    public String arabicToLatin(String arabic) {
        if (arabic == null || arabic.isBlank()) return "";

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < arabic.length()) {
            boolean matched = false;
            // Try 2-char Arabic sequences first (lam-alef ligatures)
            if (i + 1 < arabic.length()) {
                String twoChar = arabic.substring(i, i + 2);
                if (ARABIC_TO_LATIN.containsKey(twoChar)) {
                    result.append(ARABIC_TO_LATIN.get(twoChar));
                    i += 2;
                    matched = true;
                }
            }
            if (!matched) {
                String oneChar = arabic.substring(i, i + 1);
                String mapped = ARABIC_TO_LATIN.get(oneChar);
                if (mapped != null) {
                    result.append(mapped);
                } else {
                    // Non-Arabic character (space, digit, etc.) — keep as-is
                    result.append(oneChar);
                }
                i++;
            }
        }
        return result.toString().toUpperCase();
    }

    /**
     * Reverse-transliterate a Latin string to its Arabic phonetic equivalent.
     * Processes digraphs before single characters (longest-match-first).
     *
     * @param latin normalized Latin string (uppercase, no accents)
     * @return Arabic representation for comparison
     */
    public String latinToArabic(String latin) {
        if (latin == null || latin.isBlank()) return "";

        StringBuilder result = new StringBuilder();
        int i = 0;
        String upper = latin.toUpperCase();
        while (i < upper.length()) {
            boolean matched = false;
            // Try 2-char Latin digraphs first
            if (i + 1 < upper.length()) {
                String twoChar = upper.substring(i, i + 2);
                if (LATIN_TO_ARABIC.containsKey(twoChar)) {
                    result.append(LATIN_TO_ARABIC.get(twoChar));
                    i += 2;
                    matched = true;
                }
            }
            if (!matched) {
                String oneChar = upper.substring(i, i + 1);
                String mapped = LATIN_TO_ARABIC.get(oneChar);
                if (mapped != null) {
                    result.append(mapped);
                } else {
                    result.append(oneChar); // space, digit, hyphen, etc.
                }
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Detect whether a string contains Arabic Unicode characters.
     *
     * @param input any string
     * @return true if the string contains at least one Arabic letter
     */
    public boolean containsArabic(String input) {
        if (input == null) return false;
        return input.chars().anyMatch(c ->
                (c >= 0x0600 && c <= 0x06FF) ||  // Arabic block
                (c >= 0x0750 && c <= 0x077F) ||  // Arabic Supplement
                (c >= 0xFB50 && c <= 0xFDFF) ||  // Arabic Presentation Forms-A
                (c >= 0xFE70 && c <= 0xFEFF)     // Arabic Presentation Forms-B
        );
    }
}
