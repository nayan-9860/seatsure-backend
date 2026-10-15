package Seatsure.Backend.service;

import Seatsure.Backend.dto.LoginRequestDto;
import Seatsure.Backend.dto.RegisterRequestDto;
import Seatsure.Backend.entity.User;
import Seatsure.Backend.enums.Role;
import Seatsure.Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(RegisterRequestDto request){

        //check whether email is already registered or not
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("email is already registered");
        }

        //create User
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }


    public String login(LoginRequestDto request) {

        Authentication authentication =
                authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        return jwtService.generateToken(authentication);
    }

}
