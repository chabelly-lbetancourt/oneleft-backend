package es.upm.miw.oneleft.reputation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataScoreRepository extends JpaRepository<ScoreEntity, UUID> {

    @Modifying
    @NativeQuery("""
            INSERT INTO reputation_score (user_id, score) VALUES (:userId, :points)
            ON CONFLICT (user_id) DO UPDATE SET score = reputation_score.score + EXCLUDED.score""")
    void add(@Param("userId") UUID userId, @Param("points") int points);

    /** Every score as the sum of its entries: @return how many people have entries */
    @Modifying
    @NativeQuery("""
            INSERT INTO reputation_score (user_id, score)
            SELECT user_id, SUM(points) FROM reputation_entry GROUP BY user_id
            ON CONFLICT (user_id) DO UPDATE SET score = EXCLUDED.score""")
    int rebuildFromLedger();

    /** Scores of people without entries any more (erased) go back to nothing */
    @Modifying
    @NativeQuery("""
            DELETE FROM reputation_score s
            WHERE NOT EXISTS (SELECT 1 FROM reputation_entry e WHERE e.user_id = s.user_id)""")
    int deleteWithoutEntries();
}
