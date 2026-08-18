package com.pawlingo.api.vocab.service.impl;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.vocab.dto.response.TopicDetailResponse;
import com.pawlingo.api.vocab.dto.response.TopicSummaryResponse;
import com.pawlingo.api.vocab.dto.response.VocabWordResponse;
import com.pawlingo.api.vocab.entity.Topic;
import com.pawlingo.api.vocab.entity.VocabWord;
import com.pawlingo.api.vocab.repository.TopicRepository;
import com.pawlingo.api.vocab.repository.VocabWordRepository;
import com.pawlingo.api.vocab.service.VocabService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VocabServiceImpl implements VocabService {

    private final TopicRepository topicRepository;
    private final VocabWordRepository vocabWordRepository;

    public VocabServiceImpl(TopicRepository topicRepository, VocabWordRepository vocabWordRepository) {
        this.topicRepository = topicRepository;
        this.vocabWordRepository = vocabWordRepository;
    }

    @Override
    public List<TopicSummaryResponse> listTopics() {
        return topicRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(topic -> new TopicSummaryResponse(
                        topic.getCode(),
                        topic.getName(),
                        topic.getDescription(),
                        vocabWordRepository.countByTopicId(topic.getId())))
                .toList();
    }

    @Override
    public TopicDetailResponse getTopicDetail(String topicCode) {
        Topic topic = topicRepository
                .findByCode(topicCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND, "Topic not found: " + topicCode));

        List<VocabWordResponse> words = vocabWordRepository.findByTopicIdOrderByOrderIndexAsc(topic.getId()).stream()
                .map(VocabServiceImpl::toWordResponse)
                .toList();

        return new TopicDetailResponse(topic.getCode(), topic.getName(), topic.getDescription(), words);
    }

    private static VocabWordResponse toWordResponse(VocabWord word) {
        return new VocabWordResponse(
                word.getId(),
                word.getWord(),
                word.getMeaning(),
                word.getExampleSentence(),
                word.getImageUrl(),
                word.getAudioUrl(),
                word.getOrderIndex());
    }
}
