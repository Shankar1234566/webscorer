package com.webscorer.domain;

/** The four scored groups of checks plus the overall roll-up. */
public enum Category {
    SEO("SEO", "Search visibility"),
    ACCESSIBILITY("Accessibility", "Accessibility basics"),
    TECHNICAL("Technical", "Technical HTML checks"),
    CONTENT("Content", "Content structure");

    private final String label;
    private final String hint;

    Category(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
    }
}
