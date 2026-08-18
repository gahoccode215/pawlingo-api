package com.pawlingo.api.vocab.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pawlingo.api.common.exception.BusinessException;
import com.pawlingo.api.common.exception.ErrorCode;
import com.pawlingo.api.vocab.dto.response.TopicDetailResponse;
import com.pawlingo.api.vocab.dto.response.TopicSummaryResponse;
import com.pawlingo.api.vocab.entity.Topic;
import com.pawlingo.api.vocab.entity.VocabWord;
import com.pawlingo.api.vocab.repository.TopicRepository;
import com.pawlingo.api.vocab.repository.VocabWordRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VocabServiceImplTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private VocabWordRepository vocabWordRepository;

    private VocabServiceImpl vocabService;

    @BeforeEach
    void setUp() {
        vocabService = new VocabServiceImpl(topicRepository, vocabWordRepository);
    }

    @Test
    void listTopics_returnsSummariesWithWordCount() {
        Topic topic = Topic.builder()
                .id(UUID.randomUUID())
                .code("animals")
                .name("Animals")
                .description("Common animals")
                .orderIndex(1)
                .build();
        when(topicRepository.findAllByOrderByOrderIndexAsc()).thenReturn(List.of(topic));
        when(vocabWordRepository.countByTopicId(topic.getId())).thenReturn(15L);

        List<TopicSummaryResponse> result = vocabService.listTopics();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("animals");
        assertThat(result.get(0).wordCount()).isEqualTo(15L);
    }

    @Test
    void getTopicDetail_existingCode_returnsTopicWithWords() {
        UUID topicId = UUID.randomUUID();
        Topic topic = Topic.builder()
                .id(topicId)
                .code("animals")
                .name("Animals")
                .description("Common animals")
                .orderIndex(1)
                .build();
        VocabWord word = VocabWord.builder()
                .id(UUID.randomUUID())
                .topic(topic)
                .word("dog")
                .meaning("con chó")
                .exampleSentence("The dog is running.")
                .orderIndex(1)
                .build();
        when(topicRepository.findByCode("animals")).thenReturn(Optional.of(topic));
        when(vocabWordRepository.findByTopicIdOrderByOrderIndexAsc(topicId)).thenReturn(List.of(word));

        TopicDetailResponse result = vocabService.getTopicDetail("animals");

        assertThat(result.code()).isEqualTo("animals");
        assertThat(result.words()).hasSize(1);
        assertThat(result.words().get(0).word()).isEqualTo("dog");
    }

    @Test
    void getTopicDetail_unknownCode_throwsBusinessExceptionWithTopicNotFoundCode() {
        when(topicRepository.findByCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vocabService.getTopicDetail("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);
    }
}
