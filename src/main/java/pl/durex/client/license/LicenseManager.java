package pl.durex.client.license;

/**
 * Dummy LicenseManager - zawsze zwraca true
 * Prawdziwy LicenseManager jest w LicenseModule (ładowany przez stage loader)
 */
public class LicenseManager {
    private static final LicenseManager INSTANCE = new LicenseManager();
    
    public static LicenseManager getInstance() {
        return INSTANCE;
    }
    
    public boolean isValid() {
        return true; // Zawsze true, żeby moduły działały
    }
    
    public String getDaysLeftText() {
        return "∞"; // Nieskończoność
    }
    
    public void loadAndValidate() {
        // Dummy
    }
    
    public boolean validate(String key) {
        return true; // Dummy
    }
    
    public void delete() {
        // Dummy
    }
}
