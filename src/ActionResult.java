public record ActionResult(String action, int score, int threshold) {
    public boolean success() { return score >= threshold; }
}
