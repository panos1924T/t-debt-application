package gr.pants.tdebt.repository;

import gr.pants.tdebt.model.Capability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CapabilityRepository extends JpaRepository<Capability, Long> {

    Optional<Capability> findCapabilityByName(String name);

    boolean existsCapabilityByName(String name);
}
