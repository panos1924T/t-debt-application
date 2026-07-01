package gr.pants.tdebt.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant updatedAt;

    @PrePersist
    public void initializeUuid() {
        if (uuid == null) uuid = UUID.randomUUID();
    }
}
