package com.dgi.platform.service;

import com.dgi.platform.service.impl.OcrServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused regression tests for the CIN-only extraction path
 * (OcrServiceImpl.extractCinFields), verifying the number and name fixes
 * without invoking Tesseract or Spring.
 */
class OcrServiceImplCinTest {

    private OcrServiceImpl ocr;
    private Method extractCinFields;

    @BeforeEach
    void setUp() throws Exception {
        ocr = new OcrServiceImpl();

        // Inject the real normalizer (plain stateless service) into the @Autowired field.
        Field normalizerField = OcrServiceImpl.class.getDeclaredField("normalizer");
        normalizerField.setAccessible(true);
        normalizerField.set(ocr, new TextNormalizationService());

        extractCinFields = OcrServiceImpl.class.getDeclaredMethod("extractCinFields", String.class);
        extractCinFields.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extract(String text) throws Exception {
        return (Map<String, String>) extractCinFields.invoke(ocr, text);
    }

    @Test
    void standardCleanCin() throws Exception {
        Map<String, String> f = extract("CIN : 08765432\nNom : BEN ALI\nPrénom : Mohamed");
        assertEquals("08765432", f.get("cinNumber"));
        assertEquals("BEN ALI", f.get("lastName"));
        assertEquals("Mohamed", f.get("firstName"));
    }

    @Test
    void spacedCinNumberIsRecovered() throws Exception {
        Map<String, String> f = extract("بطاقة التعريف\nرقم : 0 8 7 6 5 4 3 2");
        assertEquals("08765432", f.get("cinNumber"), "spaced digits must reduce to the 8-digit CIN");
    }

    @Test
    void letterConfusedCinNumberIsRecovered() throws Exception {
        // O mis-read for 0, S for 5, no clean label adjacent
        Map<String, String> f = extract("O8765432");
        assertEquals("08765432", f.get("cinNumber"), "letter↔digit confusions must be repaired");
    }

    @Test
    void dateIsNotMistakenForCin() throws Exception {
        // Only a DD/MM/YYYY date present — must NOT be returned as a CIN number.
        Map<String, String> f = extract("Date de naissance : 12/05/1990");
        assertNull(f.get("cinNumber"), "a valid date must not be picked as the CIN number");
    }

    @Test
    void namesRecoveredWhenHeaderSharesLabelLine() throws Exception {
        // Header "بطاقة التعريف" on the SAME line trips the primary guard;
        // the CIN fallback must still recover Nom + Prénom.
        Map<String, String> f = extract(
                "الجمهورية التونسية بطاقة التعريف الوطنية اللقب TURKI الاسم islame");
        assertEquals("TURKI", f.get("lastName"));
        assertEquals("islame", f.get("firstName"));
    }

    @Test
    void emptyTextReturnsEmpty() throws Exception {
        assertTrue(extract("").isEmpty());
    }
}
