package pl.durex.autorynek.scanner;

import pl.durex.autorynek.config.PriceEntry;

/**
 * Wynik oceny jednego slotu rynku.
 */
public class ScanResult {
    public final boolean    highlight;
    public final double     foundPrice;
    public final double     maxPrice;
    public final PriceEntry matchedEntry;

    public ScanResult(boolean highlight, double foundPrice, double maxPrice, PriceEntry matchedEntry) {
        this.highlight    = highlight;
        this.foundPrice   = foundPrice;
        this.maxPrice     = maxPrice;
        this.matchedEntry = matchedEntry;
    }

    public static ScanResult noHighlight() {
        return new ScanResult(false, -1, -1, null);
    }
}
