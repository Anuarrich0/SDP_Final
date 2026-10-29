public record TurnResult(int sceneNumber, String sceneTitle, ActionResult action,
                         int before, int after) {
    public String describe() {
        return "Сцена " + sceneNumber + ": " + sceneTitle + "\n"
                + action.action() + ": сила " + action.score() + ", порог " + action.threshold()
                + " — " + (action.success() ? "успех" : "неудача")
                + "\nНестабильность: " + before + " → " + after;
    }
}
