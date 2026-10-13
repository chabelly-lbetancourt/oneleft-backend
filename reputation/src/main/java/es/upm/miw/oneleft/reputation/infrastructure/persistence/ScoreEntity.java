package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** The score of a person, a projection of the ledger: changed only with SQL. */
@Entity
@Table(name = "reputation_score")
public class ScoreEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private int score;

    protected ScoreEntity() {
    }

    UUID userId() {
        return userId;
    }

    int score() {
        return score;
    }
}
