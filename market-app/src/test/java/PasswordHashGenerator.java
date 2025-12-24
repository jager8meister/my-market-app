import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("Generating BCrypt hashes...");
        System.out.println("password1: " + encoder.encode("password1"));
        System.out.println("password2: " + encoder.encode("password2"));
        System.out.println("password3: " + encoder.encode("password3"));

        System.out.println("\nTesting existing hashes:");
        String hash3 = "$2a$10$k5/lQN9Ljr6TQs8pRKj5UuB3oQ7YPDJvQkPRF0qPRYuKVJ4uIXJzy";
        System.out.println("password3 matches existing hash: " + encoder.matches("password3", hash3));
    }
}
