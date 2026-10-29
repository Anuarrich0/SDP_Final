import java.util.Objects;

public record Scene(String title, String story, String hint,
                    int diplomacyModifier, int magicModifier, int combatModifier) {
    public Scene {
        Objects.requireNonNull(title);
        Objects.requireNonNull(story);
        Objects.requireNonNull(hint);
    }
}
