package gr.pants.tdebt.repository;

import gr.pants.tdebt.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findUserByEmailAndDeletedFalse(String email);

    Optional<User> findUserByEmail(String email);

    Optional<User> findUserByUuidAndDeletedFalse(UUID uuid);

    Optional<User> findUserByUuid(UUID uuid);

    Page<User> findAllByDeletedFalse(Pageable pageable);

    boolean existsUserByRole(String name);

    boolean existsUserByEmail(String email);

    boolean existsUserByEmailAndUuidNot(String email, UUID uuid);
}
