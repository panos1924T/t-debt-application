package gr.pants.tdebt.service;

import gr.pants.tdebt.core.exceptions.EntityAlreadyExistsException;
import gr.pants.tdebt.core.exceptions.EntityNotFoundException;
import gr.pants.tdebt.core.exceptions.PasswordMismatchException;
import gr.pants.tdebt.dto.user_dto.UserInsertDTO;
import gr.pants.tdebt.dto.user_dto.UserReadOnlyDTO;
import gr.pants.tdebt.dto.user_dto.UserUpdateDTO;
import gr.pants.tdebt.mapper.UserMapper;
import gr.pants.tdebt.model.Role;
import gr.pants.tdebt.model.User;
import gr.pants.tdebt.repository.RoleRepository;
import gr.pants.tdebt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Transactional
    @Override
    public UserReadOnlyDTO saveUser(UserInsertDTO insertDTO) {

        if (userRepository.existsUserByEmail(insertDTO.email())) {
            throw new EntityAlreadyExistsException("User", "Email " + insertDTO.email() + " already exists");
        }
        if (!insertDTO.password().equals(insertDTO.confirmPassword())) {
            throw new PasswordMismatchException("User", "Passwords do not match");
        }

        String hashedPassword = passwordEncoder.encode(insertDTO.password());
        User user = userMapper.toEntity(insertDTO, hashedPassword);
        Role role = roleRepository.findRoleByName("ROLE_USER")
                .orElseThrow(() -> new EntityNotFoundException("Role", "Role=ROLE_USER was not found"));
        user.setRole(role);

        return userMapper.toReadOnlyDTO(user);
    }

    @PreAuthorize("hasAuthority('EDIT_ONLY_USER') and #uuid == authentication.principal.uuid")
    @Transactional
    @Override
    public UserReadOnlyDTO updateUser(UUID uuid, UserUpdateDTO updateDTO) {

        User user = userRepository.findUserByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + uuid + " not found"));

        if (userRepository.existsUserByEmailAndUuidNot(updateDTO.email(), uuid)) {
            throw new EntityAlreadyExistsException("User", "Email " + updateDTO.email() + " already exists");
        }
        if (!updateDTO.password().equals(updateDTO.confirmPassword())) {
            throw new PasswordMismatchException("User", "Passwords do not match");
        }

        user.setEmail(updateDTO.email());
        user.setPassword(passwordEncoder.encode(updateDTO.password()));

        return userMapper.toReadOnlyDTO(user);
    }

    @PreAuthorize("hasAuthority('DELETE_USER') or " +
            "hasAuthority('DELETE_ONLY_USER') and #uuid == authentication.principal.uuid")
    @Transactional
    @Override
    public void deleteUser(UUID uuid) {

        User user = userRepository.findUserByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("User", "User with uuid=" + uuid + " not found"));

        user.softDelete(Instant.now());
        userRepository.save(user);
    }

    @Override
    public UserReadOnlyDTO getUserByUuid(UUID uuid) {
        return null;
    }

    @Override
    public UserReadOnlyDTO getUserByUuidDeletedFalse(UUID uuid) {
        return null;
    }

    @Override
    public Page<UserReadOnlyDTO> getAllUsers(Pageable pageable) {
        return null;
    }

    @Override
    public Page<UserReadOnlyDTO> getAllUsersDeletedFalse(Pageable pageable) {
        return null;
    }

    @Override
    public boolean isUserExists(String email) {
        return false;
    }
}
