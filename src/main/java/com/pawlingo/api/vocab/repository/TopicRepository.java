package com.pawlingo.api.vocab.repository;

import com.pawlingo.api.vocab.entity.Topic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findByCode(String code);

    List<Topic> findAllByOrderByOrderIndexAsc();
}
