public class NegotiateStrategy implements ActionStrategy {
    public ActionResult execute(LokiVariant hero, Scene scene, Difficulty difficulty) {
        return new ActionResult("Переговоры", hero.getDiplomacy() + scene.diplomacyModifier(),
                difficulty.getThreshold());
    }

    public String toString() { return "Переговоры"; }
}
