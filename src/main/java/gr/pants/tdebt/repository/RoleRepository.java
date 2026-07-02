package gr.pants.tdebt.repository;

import gr.pants.tdebt.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findRoleByName(String name);

    boolean existsRoleByName(String name);
}
