package gr.pants.tdebt.model;


import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@Getter @Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity extends AbstractAuditEntity{

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMPTZ")
    private Instant deletedAt;

    public void softDelete(Instant now) {
        this.deleted = true;
        this.deletedAt = now;
    }
}
