public class IllusionStrategy implements ActionStrategy {
    public ActionResult execute(LokiVariant hero, Scene scene, Difficulty difficulty) {
        return new ActionResult("Иллюзия", hero.getMagicPower() + scene.magicModifier(),
                difficulty.getThreshold());
    }

    public String toString() { return "Иллюзия"; }
}
