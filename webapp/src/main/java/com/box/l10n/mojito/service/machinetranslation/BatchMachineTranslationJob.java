//package com.box.l10n.mojito.service.machinetranslation;
//
//import com.box.l10n.mojito.quartz.QuartzPollableJob;
//import com.box.l10n.mojito.rest.machinetranslation.BatchTranslationRequestDTO;
//import org.apache.commons.lang3.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
///**
// * Class to process a batch of strings for machine translation against a set of target languages.
// *
// * @author garion
// */
//@Component
//public class BatchMachineTranslationJob extends QuartzPollableJob<BatchTranslationRequestDTO, TranslationsResponseDTO> {
//    static Logger logger = LoggerFactory.getLogger(BatchMachineTranslationJob.class);
//
//    @Autowired
//    MachineTranslationService machineTranslationService;
//
//    @Override
//    public TranslationsResponseDTO call(BatchTranslationRequestDTO batchTranslationRequestDTO) throws Exception {
//        logger.debug("Machine translating batch with sources = {}, and target locales = {}",
//                StringUtils.join(batchTranslationRequestDTO.getTextSources(), ", "),
//                StringUtils.join(batchTranslationRequestDTO.getTargetBcp47Tags(), ", "));
//
//        return machineTranslationService.getTranslations(
//                batchTranslationRequestDTO.getTextSources(),
//                batchTranslationRequestDTO.getSourceBcp47Tag(),
//                batchTranslationRequestDTO.getTargetBcp47Tags(),
//                batchTranslationRequestDTO.isSkipFunctionalProtection(),
//                batchTranslationRequestDTO.isSkipLeveraging(),
//                batchTranslationRequestDTO.getRepositoryIds(),
//                batchTranslationRequestDTO.getRepositoryNames()
//        );
//    }
//}
