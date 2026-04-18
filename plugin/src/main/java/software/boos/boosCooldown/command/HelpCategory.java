package software.boos.boosCooldown.command;

public enum HelpCategory {
    PLAYER("Player commands"),
    ADMIN("Administration"),
    DIAGNOSTICS("Diagnostics"),
    INTERNAL("");

    private final String title;

    HelpCategory(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
