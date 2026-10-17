package com.lostfound.model;

public enum Category {
    ELECTRONICS("Electronics"),
    BAGS_WALLETS("Bags & Wallets"),
    DOCUMENTS_IDS("Documents & IDs"),
    KEYS("Keys"),
    CLOTHING("Clothing"),
    JEWELLERY("Jewellery"),
    BOOKS_STATIONERY("Books & Stationery"),
    OTHER("Other");

    private final String label;

    Category(String label) { this.label = label; }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
