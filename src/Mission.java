import java.util.List;

public final class Mission {
    private final String title;
    private final MissionLocation location;
    private final Difficulty difficulty;

    private Mission(Builder builder) {
        title = builder.title;
        location = builder.location;
        difficulty = builder.difficulty;
    }

    public String getTitle() {
        return title;
    }

    public MissionLocation getLocation() {
        return location;
    }

    public Difficulty getDifficulty() { return difficulty; }
    public List<Scene> getScenes() { return location.getScenes(); }

    public static class Builder {
        private String title = "Стабилизировать временную ветку";
        private MissionLocation location;
        private Difficulty difficulty = Difficulty.STANDARD;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder location(MissionLocation location) {
            this.location = location;
            return this;
        }

        public Builder difficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Mission build() {
            if (title == null || title.isBlank() || location == null || difficulty == null) {
                throw new IllegalArgumentException("Укажите название, локацию и сложность миссии.");
            }
            if (location.getScenes().size() != 3) {
                throw new IllegalArgumentException("В миссии должно быть три сцены.");
            }
            return new Mission(this);
        }
    }
}
