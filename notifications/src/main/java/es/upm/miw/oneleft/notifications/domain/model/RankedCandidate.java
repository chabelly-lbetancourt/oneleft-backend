package es.upm.miw.oneleft.notifications.domain.model;

/** A candidate with the probability that they take a spot (or, with the distance rule, a score that only orders). */
public record RankedCandidate(Candidate candidate, double score) {
}
