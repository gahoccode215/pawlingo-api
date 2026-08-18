package com.pawlingo.api.progress.repository;

import com.pawlingo.api.progress.entity.Progress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressRepository extends JpaRepository<Progress, UUID> {}
