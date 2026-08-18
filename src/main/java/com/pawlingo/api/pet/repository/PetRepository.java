package com.pawlingo.api.pet.repository;

import com.pawlingo.api.pet.entity.Pet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    Optional<Pet> findByUserId(UUID userId);
}
