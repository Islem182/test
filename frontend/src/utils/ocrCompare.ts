/**
 * Shared comparison logic between "Valeur saisie" (values entered in
 * Nouvelle immatriculation) and "Valeur OCR" (values extracted from the
 * uploaded documents).
 *
 * Used by:
 *  - OcrResultsViewer.tsx   (per-document field table)
 *  - NewApplication.tsx     (cross-document comparison table)
 */

export type FieldMatchStatus = 'MATCH' | 'MISMATCH' | 'NOT_FOUND';

export interface FieldCompareResult {
  status: FieldMatchStatus;
  score: number; // 0-100
}

// ── Normalization ────────────────────────────────────────────────────────

/** Strip accents, collapse whitespace, uppercase — for free-text fields (names, addresses, labels). */
export function normalizeText(input?: string | null): string {
  if (!input) return '';
  return input
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // strip accents (é → e, ç → c, ...)
    .trim()
    .toUpperCase()
    .replace(/\s+/g, ' ');
}

/** Strip separators, uppercase — for document numbers (CIN, passeport). Exact match only. */
export function normalizeNumber(input?: string | null): string {
  if (!input) return '';
  return input.trim().toUpperCase().replace(/[\s\-./]/g, '');
}

/** Normalize a date to YYYY-MM-DD, accepting DD/MM/YYYY, DD-MM-YYYY, or already-ISO input. */
export function normalizeDateValue(input?: string | null): string {
  if (!input) return '';
  const raw = input.trim();
  const dmy = raw.match(/^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})$/);
  if (dmy) return `${dmy[3]}-${dmy[2].padStart(2, '0')}-${dmy[1].padStart(2, '0')}`;
  return raw;
}

// ── Similarity ───────────────────────────────────────────────────────────

function levenshtein(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  if (m === 0) return n;
  if (n === 0) return m;

  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));
  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      dp[i][j] = a[i - 1] === b[j - 1]
        ? dp[i - 1][j - 1]
        : 1 + Math.min(dp[i - 1][j - 1], dp[i - 1][j], dp[i][j - 1]);
    }
  }
  return dp[m][n];
}

/** Normalized similarity 0-100 between two already-normalized strings. */
function similarity(a: string, b: string): number {
  if (!a && !b) return 100;
  if (!a || !b) return 0;
  const dist = levenshtein(a, b);
  const maxLen = Math.max(a.length, b.length);
  return Math.round(100 * (1 - dist / maxLen));
}

// ── Field comparators ───────────────────────────────────────────────────

/** Document number fields (N° CIN, N° Passeport) — exact match after normalization, no fuzziness. */
export function compareNumber(userValue?: string | null, ocrValue?: string | null): FieldCompareResult {
  const u = normalizeNumber(userValue);
  const o = normalizeNumber(ocrValue);
  if (!u || !o) return { status: 'NOT_FOUND', score: 0 };
  return u === o ? { status: 'MATCH', score: 100 } : { status: 'MISMATCH', score: 0 };
}

/**
 * Free-text fields (names, company info, addresses) — tolerant of accents,
 * casing and minor OCR noise via Levenshtein similarity.
 * Default threshold 90 is strict (good for names); pass a lower threshold
 * for looser fields like addresses.
 */
export function compareText(
  userValue?: string | null,
  ocrValue?: string | null,
  threshold = 90,
): FieldCompareResult {
  const u = normalizeText(userValue);
  const o = normalizeText(ocrValue);
  if (!u || !o) return { status: 'NOT_FOUND', score: 0 };
  const score = similarity(u, o);
  return { status: score >= threshold ? 'MATCH' : 'MISMATCH', score };
}

/** Date fields — normalize common formats, then exact compare. */
export function compareDate(userValue?: string | null, ocrValue?: string | null): FieldCompareResult {
  const u = normalizeDateValue(userValue);
  const o = normalizeDateValue(ocrValue);
  if (!u || !o) return { status: 'NOT_FOUND', score: 0 };
  return u === o ? { status: 'MATCH', score: 100 } : { status: 'MISMATCH', score: 0 };
}