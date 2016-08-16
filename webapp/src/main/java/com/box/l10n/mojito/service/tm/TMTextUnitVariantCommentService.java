package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author jaurambault
 */
@Service
public class TMTextUnitVariantCommentService {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(TMTextUnitVariantCommentService.class);

    @Autowired
    TMTextUnitVariantCommentRepository tmTextUnitVariantCommentRepository;

    @Autowired
    TMTextUnitVariantRepository tmTextUnitVariantRepository;

    public void copyComments(Long sourceTmTextUnitVariantId, Long targetTMTextUnitVariantId) {

        logger.debug("Copy TMTextUnitVariantComments from source TMTextUnitVariant: {} into target TMTextUnitVariant: {}", sourceTmTextUnitVariantId, targetTMTextUnitVariantId);

        for (TMTextUnitVariantComment comment : tmTextUnitVariantCommentRepository.findAllByTmTextUnitVariant_id(sourceTmTextUnitVariantId)) {
            logger.debug("Copy comment, id: {}", comment.getId());
            addComment(targetTMTextUnitVariantId, comment.getType(), comment.getSeverity(), comment.getContent());
        }
    }

    @Transactional
    public TMTextUnitVariantComment addComment(
            Long tmTextUnitVariantId,
            TMTextUnitVariantComment.Type type,
            TMTextUnitVariantComment.Severity severity,
            String content) {

        logger.debug("Add comment for tmTextUnitVariantId: {}, type: {}, severity: {}, status: {}, content: {}", tmTextUnitVariantId, type, severity, content);
        TMTextUnitVariantComment tmTextUnitVariantComment = new TMTextUnitVariantComment();

        tmTextUnitVariantComment.setTmTextUnitVariant(tmTextUnitVariantRepository.getOne(tmTextUnitVariantId));
        tmTextUnitVariantComment.setType(type);
        tmTextUnitVariantComment.setSeverity(severity);
        tmTextUnitVariantComment.setContent(content);

        tmTextUnitVariantComment = tmTextUnitVariantCommentRepository.save(tmTextUnitVariantComment);

        logger.debug("Comment added, id: {}", tmTextUnitVariantComment.getId());

        return tmTextUnitVariantComment;
    }

    @Transactional
    public TMTextUnitVariantComment addComment(
            TMTextUnitVariant tmTextUnitVariant,
            TMTextUnitVariantComment.Type type,
            TMTextUnitVariantComment.Severity severity,
            String content) {

        TMTextUnitVariantComment addComment = addComment(tmTextUnitVariant.getId(), type, severity, content);
        tmTextUnitVariant.getTmTextUnitVariantComments().add(addComment);

        return addComment;
    }

    /**
     * Adds variant comments associated to the given TextUnitDTOs.
     * It processes the TextUnitDTOs by batch (500 at a time) to reduce the number of queries needed.
     *
     * @param textUnitDTOs List of the DTOs to enrich
     * @return List of DTOs containing their associated comments
     */
    public List<TextUnitDTOWithComments> enrichTextUnitDTOsWithComments(List<TextUnitDTO> textUnitDTOs) {

        int BATCH_SIZE = 500;

        List<TextUnitDTOWithComments> textUnitDTOsWithComments = new ArrayList<>();
        List<TextUnitDTO> textUnitDTOsForBatch = new ArrayList<>();
        List<Long> tmTextUnitVariantIdsForBatch = new ArrayList<>();
        int count = 1;

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            Long tmTextUnitVariantId = textUnitDTO.getTmTextUnitVariantId();
            textUnitDTOsForBatch.add(textUnitDTO);
            tmTextUnitVariantIdsForBatch.add(tmTextUnitVariantId);

            if (count % BATCH_SIZE == 0) {
                processTextUnitDTOsBatch(textUnitDTOsWithComments, textUnitDTOsForBatch, tmTextUnitVariantIdsForBatch);

                textUnitDTOsForBatch = new ArrayList<>();
                tmTextUnitVariantIdsForBatch = new ArrayList<>();
            }
        }

        processTextUnitDTOsBatch(textUnitDTOsWithComments, textUnitDTOsForBatch, tmTextUnitVariantIdsForBatch);

        return textUnitDTOsWithComments;
    }

    /**
     * Updates the given list of DTOs with their associated comments
     *
     * @param textUnitDTOsWithComments The list to be updated
     * @param textUnitDTOsForBatch List of the DTOs to process
     * @param tmTextUnitVariantIdsForBatch The list of {@link TMTextUnitVariant#id} to get comments for
     */
    private void processTextUnitDTOsBatch(List<TextUnitDTOWithComments> textUnitDTOsWithComments, List<TextUnitDTO> textUnitDTOsForBatch, List<Long> tmTextUnitVariantIdsForBatch) {

        Multimap<Long, TMTextUnitVariantComment> textUnitVariantCommentsMap = ArrayListMultimap.create();
        List<TMTextUnitVariantComment> textUnitVariantComments = new ArrayList<>();

        if (!tmTextUnitVariantIdsForBatch.isEmpty()) {
            textUnitVariantComments = tmTextUnitVariantCommentRepository.findByTmTextUnitVariantIdIn(tmTextUnitVariantIdsForBatch);
        }

        for (TMTextUnitVariantComment tmTextUnitVariantComment : textUnitVariantComments) {
            Long tmTextUnitVariantId = tmTextUnitVariantComment.getTmTextUnitVariant().getId();
            textUnitVariantCommentsMap.put(tmTextUnitVariantId, tmTextUnitVariantComment);
        }

        mergeCommentsIntoTextUnitDTOs(textUnitDTOsWithComments, textUnitDTOsForBatch, textUnitVariantCommentsMap);
    }

    /**
     * Adds the given comments to their associated TextUnitDTO, which should be in the given list.
     *
     * @param textUnitDTOsWithComments The list to be updated
     * @param textUnitDTOs List of the DTOs to enrich
     * @param textUnitVariantCommentsMap Map of {@link TMTextUnitVariant#id} => {@link TMTextUnitVariantComment} associated to these DTOs
     * @return The list of TextUnitDTOs containing their associated comments
     */
    private void mergeCommentsIntoTextUnitDTOs(List<TextUnitDTOWithComments> textUnitDTOsWithComments, List<TextUnitDTO> textUnitDTOs, Multimap<Long, TMTextUnitVariantComment> textUnitVariantCommentsMap) {

        for (TextUnitDTO textUnitDTO : textUnitDTOs) {
            TextUnitDTOWithComments textUnitDTOWithComments = new TextUnitDTOWithComments(textUnitDTO);

            Long tmTextUnitVariantId = textUnitDTO.getTmTextUnitVariantId();
            Collection<TMTextUnitVariantComment> variantComments = textUnitVariantCommentsMap.get(tmTextUnitVariantId);
            textUnitDTOWithComments.setTmTextUnitVariantComments(new ArrayList<>(variantComments));

            textUnitDTOsWithComments.add(textUnitDTOWithComments);
        }
    }

}
