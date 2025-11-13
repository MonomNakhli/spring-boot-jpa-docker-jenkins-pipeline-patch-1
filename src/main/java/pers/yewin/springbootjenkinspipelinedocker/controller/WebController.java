// Dans WebController.java - Ajoutez ces méthodes
@RestController
public class WebController {
    
    // ❌ VULNÉRABILITÉ: Injection SQL
    @GetMapping("/unsafe/user")
    public String getUser(@RequestParam String id) {
        return "User: " + id; // Pas de validation
    }
    
    // ❌ VULNÉRABILITÉ: Mot de passe en dur
    private String adminPassword = "admin123";
    
    // ❌ VULNÉRABILITÉ: Log sensible
    @GetMapping("/login")
    public String login(@RequestParam String username) {
        System.out.println("Login attempt: " + username); // Log sensible
        return "Login attempt logged";
    }
    
    // ❌ VULNÉRABILITÉ: Faible cryptographie
    @GetMapping("/hash")
    public String weakHash(@RequestParam String data) {
        return Integer.toString(data.hashCode()); // Hash faible
    }
}