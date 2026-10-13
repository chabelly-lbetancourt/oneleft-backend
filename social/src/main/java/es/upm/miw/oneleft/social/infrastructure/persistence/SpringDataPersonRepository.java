package es.upm.miw.oneleft.social.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataPersonRepository extends JpaRepository<PersonEntity, UUID> {
}
