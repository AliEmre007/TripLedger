package com.tripledger.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByOrganisationIdAndIdentitySubject(UUID organisationId, String identitySubject);
}
