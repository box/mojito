//package com.box.l10n.mojito.service.machinetranslation;
//
//import com.google.common.collect.ImmutableList;
//import com.google.common.collect.ImmutableMap;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = {TranslationMerger.class})
//class TranslationMergerTest {
//
//    @Autowired
//    TranslationMerger translationMerger;
//
//    @Test
//    void getMergedTranslationsBySourceText() {
//        TranslationDTO leveragedTranslation1 = new TranslationDTO();
//        leveragedTranslation1.setTranslationSource(TranslationSource.MOJITO_TM_LEVERAGE);
//        leveragedTranslation1.setBcp47Tag("fr");
//
//        TranslationDTO leveragedTranslation2 = new TranslationDTO();
//        leveragedTranslation2.setTranslationSource(TranslationSource.MOJITO_TM_LEVERAGE);
//        leveragedTranslation2.setBcp47Tag("de");
//
//        // This translation should not be in the final list
//        TranslationDTO mtTranslation1 = new TranslationDTO();
//        mtTranslation1.setTranslationSource(TranslationSource.MICROSOFT_MT);
//        mtTranslation1.setBcp47Tag("de");
//
//        TranslationDTO mtTranslation2 = new TranslationDTO();
//        mtTranslation2.setTranslationSource(TranslationSource.MICROSOFT_MT);
//        mtTranslation2.setBcp47Tag("ro");
//
//        String sourceText = "sourceText";
//        ImmutableMap<String, ImmutableList<TranslationDTO>> result = translationMerger.getMergedTranslationsBySourceText(
//                ImmutableMap.of(sourceText,
//                        ImmutableList.of(leveragedTranslation1, leveragedTranslation2)),
//                ImmutableMap.of(sourceText,
//                        ImmutableList.of(mtTranslation1, mtTranslation2)));
//
//        Assertions.assertEquals(result.keySet().size(), 1);
//        Assertions.assertEquals(result.get(sourceText).size(), 3);
//        Assertions.assertTrue(result.get(sourceText).contains(leveragedTranslation1));
//        Assertions.assertTrue(result.get(sourceText).contains(leveragedTranslation2));
//        Assertions.assertFalse(result.get(sourceText).contains(mtTranslation1));
//        Assertions.assertTrue(result.get(sourceText).contains(mtTranslation2));
//    }
//
//    @Test
//    void testGetPriorityTranslationWorks() {
//        TranslationDTO highPriorityTranslation = new TranslationDTO();
//        TranslationDTO lowPriorityTranslation = new TranslationDTO();
//
//        highPriorityTranslation.setTranslationSource(TranslationSource.MOJITO_TM_LEVERAGE);
//        lowPriorityTranslation.setTranslationSource(TranslationSource.MICROSOFT_MT);
//
//        TranslationDTO pickedTranslation = translationMerger.getPriorityTranslation(highPriorityTranslation, lowPriorityTranslation);
//
//        Assertions.assertEquals(highPriorityTranslation, pickedTranslation);
//    }
//
//    @Test
//    void testGetPriorityTranslationNullTranslationFirst() {
//        TranslationDTO translation = new TranslationDTO();
//
//        TranslationDTO pickedTranslation = translationMerger.getPriorityTranslation(
//                null, translation);
//
//        Assertions.assertEquals(translation, pickedTranslation);
//    }
//
//    @Test
//    void testGetPriorityTranslationNullTranslationSecond() {
//        TranslationDTO translation = new TranslationDTO();
//
//        TranslationDTO pickedTranslation = translationMerger.getPriorityTranslation(
//                null, translation);
//
//        Assertions.assertEquals(translation, pickedTranslation);
//    }
//}