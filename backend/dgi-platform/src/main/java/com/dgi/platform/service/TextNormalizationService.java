package com.dgi.platform.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Reusable text normalization for Arabic and French/Latin identity documents.
 *
 * Handles:
 *  - Arabic diacritics (tashkeel) removal
 *  - Arabic-Indic digit ↔ Western digit conversion
 *  - Arabic letter variant normalization (alef forms, teh marbuta, etc.)
 *  - Latin accent / diacritic stripping (é → e, ç → c, etc.)
 *  - Case folding and whitespace collapsing
 *  - Date format normalization → canonical YYYY-MM-DD
 *  - Punctuation stripping for name comparison
 *
 * All methods are stateless and thread-safe.
 */
@Service
public class TextNormalizationService {

    // ── Arabic Unicode ranges & patterns ─────────────────────────────────────

    /** Arabic diacritics (tashkeel): fatha, damma, kasra, sukun, shadda, etc. */
    private static final Pattern ARABIC_DIACRITICS =
            Pattern.compile("[\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E4\u06E7\u06E8\u06EA-\u06ED]");

    /** Arabic-Indic digits ٠١٢٣٤٥٦٧٨٩ → 0-9 */
    private static final char[] ARABIC_INDIC_DIGITS = {
            '\u0660', '\u0661', '\u0662', '\u0663', '\u0664',
            '\u0665', '\u0666', '\u0667', '\u0668', '\u0669'
    };

    /** Extended Arabic-Indic digits used in Persian/Urdu ۰۱۲۳۴۵۶۷۸۹ */
    private static final char[] EXTENDED_ARABIC_INDIC_DIGITS = {
            '\u06F0', '\u06F1', '\u06F2', '\u06F3', '\u06F4',
            '\u06F5', '\u06F6', '\u06F7', '\u06F8', '\u06F9'
    };

    /** Tatweel (kashida) elongation character — purely cosmetic, remove it. */
    private static final char TATWEEL = '\u0640';

    // ── Date patterns ─────────────────────────────────────────────────────────

    /** DD/MM/YYYY or DD-MM-YYYY */
    private static final Pattern DATE_DMY =
            Pattern.compile("^(\\d{1,2})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4})$");

    /** YYYY/MM/DD or YYYY-MM-DD */
    private static final Pattern DATE_YMD =
            Pattern.compile("^(\\d{4})[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{1,2})$");

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full normalization pipeline for a name token (first or last name).
     * Steps: digits conversion → diacritics removal → alef/teh-marbuta normalization
     *        → tatweel removal → Latin NFD decomposition → accent strip
     *        → uppercase → whitespace collapse → punctuation strip.
     *
     * @param input raw name string (Arabic, Latin, or mixed)
     * @return normalized name suitable for comparison
     */
    public String normalizeName(String input) {
        if (input == null || input.isBlank()) return "";

        String s = input.trim();
        s = convertArabicIndicToWestern(s);
        s = removeArabicDiacritics(s);
        s = normalizeArabicLetters(s);
        s = removeTatweel(s);
        s = stripLatinAccents(s);
        s = s.toUpperCase();
        s = s.replaceAll("[\\p{Punct}&&[^'\\-]]", " "); // keep apostrophe & hyphen
        s = collapseWhitespace(s);
        return s;
    }

    /**
     * Remove Arabic definite article prefix (ال / AL / EL) for resilient name matching.
     * e.g. "الطرابلسي" -> "طرابلسي", "ALTRABELSI" -> "TRABELSI", "EL MEJRI" -> "MEJRI".
     */
    public String stripAlPrefix(String name) {
        if (name == null || name.isBlank()) return "";
        String s = name.trim();
        if (s.startsWith("ال") && s.length() > 3) {
            return s.substring(2).trim();
        }
        String up = s.toUpperCase();
        if (up.startsWith("AL ") && s.length() > 4) {
            return s.substring(3).trim();
        }
        if (up.startsWith("EL ") && s.length() > 4) {
            return s.substring(3).trim();
        }
        if (up.startsWith("AL") && s.length() > 5) {
            return s.substring(2).trim();
        }
        if (up.startsWith("EL") && s.length() > 5) {
            return s.substring(2).trim();
        }
        return s;
    }

    /**
     * Normalize a CIN or passport number.
     * Converts Arabic-Indic digits, uppercases Latin letters, strips all
     * whitespace and punctuation. Result is used for exact matching only.
     *
     * @param input raw document number
     * @return normalized document number
     */
    public String normalizeDocumentNumber(String input) {
        if (input == null || input.isBlank()) return "";

        String s = input.trim();
        s = convertArabicIndicToWestern(s);
        s = removeArabicDiacritics(s);
        s = s.toUpperCase();
        s = s.replaceAll("[\\s\\-\\./]", ""); // strip all separators
        return s;
    }

    /**
     * Normalize a date string to canonical ISO format YYYY-MM-DD.
     * Accepts: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD, YYYY/MM/DD.
     * Returns the canonical form, or the original trimmed string if
     * the format is not recognized.
     *
     * @param input raw date string
     * @return YYYY-MM-DD or original if unparseable
     */
    public String normalizeDate(String input) {
        if (input == null || input.isBlank()) return "";

        String s = convertArabicIndicToWestern(input.trim());

        var dmyMatcher = DATE_DMY.matcher(s);
        if (dmyMatcher.matches()) {
            String day   = zeroPad(dmyMatcher.group(1));
            String month = zeroPad(dmyMatcher.group(2));
            String year  = dmyMatcher.group(3);
            return year + "-" + month + "-" + day;
        }

        var ymdMatcher = DATE_YMD.matcher(s);
        if (ymdMatcher.matches()) {
            String year  = ymdMatcher.group(1);
            String month = zeroPad(ymdMatcher.group(2));
            String day   = zeroPad(ymdMatcher.group(3));
            return year + "-" + month + "-" + day;
        }

        // Already ISO or unrecognized — return as-is
        return s;
    }

    /**
     * Normalize an address for flexible (non-exact) comparison.
     * Strips accents, uppercases, collapses whitespace, removes common
     * filler words (rue, avenue, cité, etc.) to improve token overlap.
     *
     * @param input raw address string
     * @return normalized address
     */
    public String normalizeAddress(String input) {
        if (input == null || input.isBlank()) return "";

        String s = input.trim();
        s = convertArabicIndicToWestern(s);
        s = removeArabicDiacritics(s);
        s = normalizeArabicLetters(s);
        s = stripLatinAccents(s);
        s = s.toUpperCase();
        // Remove very common French/Arabic address filler words
        s = s.replaceAll("\\b(RUE|AVENUE|AVE|BOULEVARD|BLVD|CITE|CITÉ|QUARTIER|QRT"
                + "|ROUTE|IMPASSE|LOTISSEMENT|شارع|حي|طريق|نهج)\\b", " ");
        s = s.replaceAll("[,\\./\\\\#]", " ");
        s = collapseWhitespace(s);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Package-visible helpers (also used by ArabicLatinTransliterator)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convert Arabic-Indic (٠-٩) and Extended Arabic-Indic (۰-۹) digits
     * to their Western equivalents (0-9).
     */
    public String convertArabicIndicToWestern(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            boolean converted = false;
            for (int i = 0; i < 10; i++) {
                if (c == ARABIC_INDIC_DIGITS[i] || c == EXTENDED_ARABIC_INDIC_DIGITS[i]) {
                    sb.append((char) ('0' + i));
                    converted = true;
                    break;
                }
            }
            if (!converted) sb.append(c);
        }
        return sb.toString();
    }

    /** Remove Arabic tashkeel diacritics. */
    public String removeArabicDiacritics(String input) {
        if (input == null) return "";
        return ARABIC_DIACRITICS.matcher(input).replaceAll("");
    }

    /**
     * Normalize common Arabic letter variants:
     *  - All alef forms (أ إ آ ا ٱ) → plain alef (ا)
     *  - Teh marbuta (ة) → heh (ه) — common in Tunisian names
     *  - Alef maqsura (ى) → yeh (ي)
     *  - Waw with hamza above (ؤ) → waw (و)
     *  - Yeh with hamza above (ئ) → yeh (ي)
     */
    public String normalizeArabicLetters(String input) {
        if (input == null) return "";
        return input
                .replace('\u0623', '\u0627')  // أ → ا
                .replace('\u0625', '\u0627')  // إ → ا
                .replace('\u0622', '\u0627')  // آ → ا
                .replace('\u0671', '\u0627')  // ٱ → ا
                .replace('\u0629', '\u0647')  // ة → ه
                .replace('\u0649', '\u064A')  // ى → ي
                .replace('\u0624', '\u0648')  // ؤ → و
                .replace('\u0626', '\u064A'); // ئ → ي
    }

    /** Remove Arabic tatweel (kashida) elongation character. */
    public String removeTatweel(String input) {
        if (input == null) return "";
        return input.replace(String.valueOf(TATWEEL), "");
    }

    /**
     * Strip Latin diacritics/accents using Unicode NFD decomposition.
     * é→e, è→e, ê→e, à→a, ç→c, ü→u, etc.
     */
    public String stripLatinAccents(String input) {
        if (input == null) return "";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    /** Collapse multiple consecutive whitespace characters into a single space. */
    public String collapseWhitespace(String input) {
        if (input == null) return "";
        return input.replaceAll("\\s+", " ").trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String zeroPad(String number) {
        return number.length() == 1 ? "0" + number : number;
    }
}
