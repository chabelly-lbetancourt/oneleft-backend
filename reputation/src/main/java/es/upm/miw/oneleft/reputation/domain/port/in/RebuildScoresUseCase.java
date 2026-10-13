package es.upm.miw.oneleft.reputation.domain.port.in;

/** Computes every score again from the ledger: the scores are a projection of it. */
public interface RebuildScoresUseCase {

    /** @return how many people have a score */
    int rebuildScores();
}
