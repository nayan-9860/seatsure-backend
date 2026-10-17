package Seatsure.Backend.config;

import Seatsure.Backend.entity.User;
import Seatsure.Backend.enums.Role;
import Seatsure.Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.seed-admin.enabled",
        havingValue = "true"
)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@seatsure.com";

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .name("SeatSure Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        System.out.println("Default admin user created.");
    }
}
