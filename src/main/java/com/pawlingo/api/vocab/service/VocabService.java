package com.pawlingo.api.vocab.service;

import com.pawlingo.api.vocab.dto.response.TopicDetailResponse;
import com.pawlingo.api.vocab.dto.response.TopicSummaryResponse;
import java.util.List;

public interface VocabService {

    List<TopicSummaryResponse> listTopics();

    TopicDetailResponse getTopicDetail(String topicCode);
}
