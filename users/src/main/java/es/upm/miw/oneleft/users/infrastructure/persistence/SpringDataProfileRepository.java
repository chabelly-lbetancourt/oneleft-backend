package es.upm.miw.oneleft.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataProfileRepository extends JpaRepository<ProfileEntity, UUID> {
}
