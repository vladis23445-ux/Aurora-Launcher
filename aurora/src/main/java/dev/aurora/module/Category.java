package dev.aurora.module;

public enum Category {
    COMBAT("Combat"),
    RENDER("Render"),
    HUD("HUD"),
    MISC("Misc");

    private final String title;

    Category(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
