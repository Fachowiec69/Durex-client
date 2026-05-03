package pl.durex.autorynek.config;

public class PriceEntry {
    public String name;
    public String material;
    public String lore;
    public String enchants;
    public double maxPrice;
    public double sellPrice;
    public int componentCount;
    public Integer customModelData;
    public Integer requiredCount = 1;
    public Integer sellCount = 1;
    public Boolean buyEnabled = true;
    public Boolean sellEnabled = false;
    public String excludedEnchants = "";
    /** Minimalna liczba dni do wygaśnięcia oferty (null = bez limitu) */
    public Integer minDaysLeft = null;

    public PriceEntry() {}

    public PriceEntry(String name, double maxPrice, double sellPrice) {
        this.name = name;
        this.maxPrice = maxPrice;
        this.sellPrice = sellPrice;
        this.buyEnabled = true;
        this.sellEnabled = false;
    }
}
