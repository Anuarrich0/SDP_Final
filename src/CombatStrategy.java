public class CombatStrategy implements ActionStrategy {
    public ActionResult execute(LokiVariant hero, Scene scene, Difficulty difficulty) {
        return new ActionResult("Бой", hero.getCombatPower() + scene.combatModifier(),
                difficulty.getThreshold());
    }

    public String toString() { return "Бой"; }
}
