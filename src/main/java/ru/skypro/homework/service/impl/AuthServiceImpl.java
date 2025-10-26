package ru.skypro.homework.service.impl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.skypro.homework.dto.Register;
import ru.skypro.homework.repository.UserRepository;
import ru.skypro.homework.service.AuthService;
import ru.skypro.homework.entity.User;
import ru.skypro.homework.dto.Role;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Override
    public boolean login(String userName, String password) {
        User user = userRepository.findByUsername(userName).orElse(null);
        if (user == null) return false;
        return encoder.matches(password, user.getPassword());
    }

    @Override
    public boolean register(Register register) {
        if (userRepository.findByUsername(register.getUsername()).isPresent()) {
            return false;
        }
        User newUser = new User();
        newUser.setUsername(register.getUsername());
        newUser.setPassword(encoder.encode(register.getPassword()));
        newUser.setFirstName(register.getFirstName());
        newUser.setLastName(register.getLastName());
        newUser.setPhone(register.getPhone());
        newUser.setRole(register.getRole() != null ? register.getRole() : Role.USER);
        userRepository.save(newUser);
        return true;
    }
}
