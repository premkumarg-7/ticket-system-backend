package e2d.auth.service;

import e2d.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Boolean checkUserEmailExists(String email) {
        return userRepository.findByEmail(email) != null;
    }
}
