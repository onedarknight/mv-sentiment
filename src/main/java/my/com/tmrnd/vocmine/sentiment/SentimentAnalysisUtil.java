/* 
 * © 2017.Telekom Malaysia Berhad. ALL RIGHTS RESERVED. All content 
 * appearing on this site/manual/contract/page/document/book/e-book/video/
 * movie/sound track/song /etc are the exclusive property of Telekom Malaysia 
 * Berhad and are protected under the Copyright Act 1987.
 * 
 * You are respectfully reminded that any unauthorized capture or copying of these 
 * content by whatever means is an offence under the Copyright Act 1987. None of 
 * the content mentioned above may be directly or indirectly published, reproduced, 
 * copied, stored, manipulated, modified, sold, transmitted, redistributed, 
 * projected, used in any way or redistributed in any medium without the explicit 
 * permission of Telekom Malaysia Berhad.
 */
package my.com.tmrnd.vocmine.sentiment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import my.com.tmrnd.vocmine.db.entity.VmSentimentLexicon;

import my.com.tmrnd.vocmine.entity.VocEntity;
import my.com.tmrnd.vocmine.entity.VocEntitySentiment;
import my.com.tmrnd.vocmine.entity.VocSentiment;
import my.com.tmrnd.vocmine.entity.VocTextMiningResult;
import my.com.tmrnd.vocmine.lr.LangUtil;

import my.com.tmrnd.vocmine.ner.NER_RecognitionUtil;

import my.com.tmrnd.vocmine.normalization.Normalizer;
import my.com.tmrnd.vocmine.util.VocmineUtil;
import org.apache.commons.lang3.StringUtils;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Administrator
 */
public class SentimentAnalysisUtil {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisUtil.class.getName());
    public static boolean DEBUG_LOG_OUTPUT = false;
    public static boolean DEBUG_LOOP = false;
    public static boolean DEBUG_SA_RULE_SENTIMENT_NEGATION = false;
    public static boolean DEBUG_SKIP_NORMALIZE_TEXT = true;
    public static boolean DEBUG_REMOVE_DUPLICATE_VALUE = true;
    public static boolean DEBUG_FIND_SENTIMENT_ACROSS_SENTENCES = true;

    static Set<VocSentiment> setVocSentiment_template = null;
    static Set<String> setNegationWord = null;
    static Map<String, VmSentimentLexicon> mapVmSentimentLexicon = null;

    public static void setRule(SentimentRule rule) {
        setVocSentiment_template = rule.setVocSentiment_template;
        setNegationWord = rule.setNegationLexicon_template;
        mapVmSentimentLexicon = rule.mapVmSentimentLexicon;

        //Normalizer.init();
        //Normalizer.IS_USE_VOCMINE_LANGUTIL = true;
    }

    public static void initTextNormalization(DBI jdbi) {
        Normalizer.init(jdbi);
    }

    public static VocTextMiningResult runVocTextMiningProcess(String textId, String strText) {
        String strDebugTextId = "";

        System.out.println("");
        System.out.println("***********************************************************************");
        System.out.println("---- Processing textId[" + textId + "]: started  ----");
        System.out.println("***********************************************************************");

        String strNormalizedText = "";

        if (!DEBUG_SKIP_NORMALIZE_TEXT) {
            System.out.println("");
            System.out.println("--- Text Normalization Module (start) ---");
            strNormalizedText = Normalizer.normalizeText(strText);
            System.out.println("--- Text Normalization Module (end) ---");
//            System.out.println("");
        } else {
            //log.info("DEBUG_SKIP_NORMALIZE_TEXT = true, skip normalization module");
            System.out.println("DEBUG_SKIP_NORMALIZE_TEXT = true, skip normalization module");
            strNormalizedText = strText;
        }
        
        String strLanguage = LangUtil.detectLanguage(strNormalizedText);


        
        Set<VocEntity> setNER_currentText = NER_RecognitionUtil.getNamedEntityRecognition(strNormalizedText);

        Set<String> setSentimentPositive_currentText = new HashSet<>();
        Set<String> setSentimentNegative_currentText = new HashSet<>();
        Set<VocSentiment> setVocSentiment_currentText = new HashSet<>();
        String[] strSplitSentenece = strNormalizedText.toLowerCase().split("\\.");

        //String strNegationWord[] = {"cannot ", "no ", "not ", "never ", "didnt ", "didn`t", "can't ", "tidak ", "dis", "can not "};
        //-------identify sentiment positive
        if (DEBUG_LOG_OUTPUT) {
            System.out.println("");
            System.out.println("----------------------------------------------");
            System.out.println("--- Indentify sentiment text in input text ---");
            System.out.println("----------------------------------------------");
        }
        for (VocSentiment vocSentiment : setVocSentiment_template) {
            String strSentiment = vocSentiment.getSentimentText();
            String strSentimentType = vocSentiment.getPolarity();
            //2 may 2017 - bugfix: check by looping sentence
            if (VocmineUtil.isTextContains(strNormalizedText.toLowerCase(), strSentiment.toLowerCase())) {
                if (DEBUG_LOG_OUTPUT) {
                    System.out.println("\nFound strSentiment[" + strSentiment + "] in text before sentence split [" + strNormalizedText.toLowerCase() + "]");
                }
                for (String strSentence : strSplitSentenece) {
                    if (DEBUG_LOG_OUTPUT) {
                        System.out.println("\n\tloop each strSentence[" + strSentence + "]");
                    }

                    if (DEBUG_SA_RULE_SENTIMENT_NEGATION) {
                        addSentimentToResult_WithNegationChecking(strSentence, vocSentiment, strSentimentType, strSentiment,
                                setSentimentPositive_currentText, setSentimentNegative_currentText, setVocSentiment_currentText
                        );
                    } else {
                        addSentimentToResult(vocSentiment, strSentimentType, strSentiment,
                                setSentimentPositive_currentText, setSentimentNegative_currentText, setVocSentiment_currentText
                        );
                    }

                }
            }

        }
        
        VocTextMiningResult vocTextMiningResult = new VocTextMiningResult();
        vocTextMiningResult.id = textId;
        vocTextMiningResult.originalText = strText;
        vocTextMiningResult.normalizedText = strNormalizedText;
        vocTextMiningResult.detectedLanguage = strLanguage;
        vocTextMiningResult.setNER = setNER_currentText;
        vocTextMiningResult.setSentimentPositive = setSentimentPositive_currentText;
        vocTextMiningResult.setSentimentNegative = setSentimentNegative_currentText;
        vocTextMiningResult.setSentimentAll = setVocSentiment_currentText;

        if (DEBUG_REMOVE_DUPLICATE_VALUE) {
            removeDuplicateValue(vocTextMiningResult);
        }

        removeSentimentOverlappingWithEntity(vocTextMiningResult);

        analyzeEntitySentimentRelationship(vocTextMiningResult);

        System.out.println("");
        System.out.println("--------------------------------------------------");
        System.out.println("--- Engine result: vocTextMiningResult (start) ---");
        System.out.println("--------------------------------------------------");
        System.out.println(vocTextMiningResult.toString());
        System.out.println("--- Engine result: vocTextMiningResult (end) ---");
//        System.out.println("");
        System.out.println("---- Processing textId[" + textId + "]: completed ----");

        return vocTextMiningResult;
    }

    public static void addSentimentToResult(
            VocSentiment vocSentiment, String strSentimentType, String strSentiment,
            Set<String> setSentimentPositive_currentText,
            Set<String> setSentimentNegative_currentText,
            Set<VocSentiment> setVocSentiment_currentText
    ) {
        if (strSentimentType.equals("Positive")) {
            setSentimentPositive_currentText.add(strSentiment);
        } else {
            setSentimentNegative_currentText.add(strSentiment);
        }

        VocSentiment newVocSentiment = new VocSentiment();
        newVocSentiment.setPolarity(strSentimentType);
        newVocSentiment.setSentimentText(strSentiment);
        newVocSentiment.setPolarityConfidence(getSentimentPolarity(strSentiment));
        setVocSentiment_currentText.add(vocSentiment);
    }

    public static void addSentimentToResult_WithNegationChecking(String strSentence,
            VocSentiment vocSentiment, String strSentimentType, String strSentiment,
            Set<String> setSentimentPositive_currentText,
            Set<String> setSentimentNegative_currentText,
            Set<VocSentiment> setVocSentiment_currentText
    ) {
        String strCurrentSentence = strSentence.toLowerCase();
        String strSentimentText = strSentiment.toLowerCase();
        List<Integer> listLocationIdxSentiment = getAllStringLocationIdx(strCurrentSentence, strSentimentText);

        //System.out.println(listLocationIdxSentiment);        
        for (int locationIdxSentiment : listLocationIdxSentiment) {
            int sentimentIdx = getWordLocationIdx(strCurrentSentence.toLowerCase(), locationIdxSentiment, strSentimentText.toLowerCase());
            System.out.println("\tFound sentiment[" + strSentimentText + "] at[" + sentimentIdx + "]");

            //run 2nd time combined with negation word
            boolean isNegationFound = false;
            for (String strNegationWord : setNegationWord) {
                boolean isCurrentNegationWordFound_beforeSentiment = false;
                boolean isCurrentNegationWordFound_afterSentiment = false;
                String negationWordWithSentiment = strNegationWord.trim().toLowerCase() + " " + strSentiment.toLowerCase();
//                DISABLED BY NIZAM
//                String sentimentWithNegationWord = strSentiment.toLowerCase() + " " + strNegationWord.trim().toLowerCase();

                if (VocmineUtil.isTextContains(strSentence, strSentiment.toLowerCase())) {
                    String foundSentimentWithNegationWord = "";
                    if (VocmineUtil.isTextContains(strSentence, negationWordWithSentiment)) {
                        isCurrentNegationWordFound_beforeSentiment = true;
                        foundSentimentWithNegationWord = negationWordWithSentiment;
                    } 
//                    DISABLED BY NIZAM
//                    else if (VocmineUtil.isTextContains(strSentence, sentimentWithNegationWord)) {
//                        isCurrentNegationWordFound_afterSentiment = true;
//                        foundSentimentWithNegationWord = sentimentWithNegationWord;
//                    }

                    if (isCurrentNegationWordFound_beforeSentiment == true || isCurrentNegationWordFound_afterSentiment == true) {
                        if (DEBUG_LOG_OUTPUT) {
                            System.out.println("\t\tisCurrentNegationWordFound=true... negation sentimenTex[" + foundSentimentWithNegationWord + "]");
                        }
                        List<Integer> listLocationIdxSentimentWithNegation = getAllStringLocationIdx(strCurrentSentence, foundSentimentWithNegationWord);
                        System.out.println(listLocationIdxSentimentWithNegation);
                        for (int locationIdxSentimentWithNegation : listLocationIdxSentimentWithNegation) {
                            int sentimentWithNegationIdx = getWordLocationIdx(strCurrentSentence.toLowerCase(), locationIdxSentimentWithNegation, foundSentimentWithNegationWord);
                            System.out.println("\t\tCheck if negation sentiment[" + foundSentimentWithNegationWord + "] at[" + sentimentWithNegationIdx + "] is beside existing sentimentIdx[" + sentimentIdx + "]");
                            if (isCurrentNegationWordFound_beforeSentiment) {
                                if ((sentimentIdx - sentimentWithNegationIdx) == 1) {
                                    isNegationFound = true;
                                }
                            } else if (isCurrentNegationWordFound_afterSentiment) {
                                if ((sentimentIdx - sentimentWithNegationIdx) == 0) {
                                    isNegationFound = true;
                                }
                            }

                            if (isNegationFound) {
                                System.out.println("\t\tAdd current found negation sentiment[" + foundSentimentWithNegationWord + "] at[" + sentimentWithNegationIdx + "] to sentiment result");
                                VocSentiment newVocSentiment = addInverseSentimentToResult(strSentimentType,
                                        strSentiment, foundSentimentWithNegationWord,
                                        setSentimentPositive_currentText, setSentimentNegative_currentText, setVocSentiment_currentText
                                );
                            } else {
                                System.out.println("\t\tIgnore found negation sentiment, not beside current sentiment idx");
                            }
                        }

                    }
                }

            }
            if (isNegationFound == false
                    && VocmineUtil.isTextContains(strSentence, strSentiment.toLowerCase())) {
                if (DEBUG_LOG_OUTPUT) {
                    System.out.println("\tisNegationFound=false... add sentimenTex[" + strSentiment + "]");
                }
                addSentimentToResult(vocSentiment, strSentimentType, strSentiment,
                        setSentimentPositive_currentText, setSentimentNegative_currentText, setVocSentiment_currentText
                );
            }
        }
    }

    public static VocSentiment addInverseSentimentToResult(String strSentimentType,
            String strSentiment, String negationWordWithSentiment,
            Set<String> setSentimentPositive_currentText,
            Set<String> setSentimentNegative_currentText,
            Set<VocSentiment> setVocSentiment_currentText
    ) {
        VocSentiment newVocSentiment = new VocSentiment();
        //inverse sentiment type
        if (strSentimentType.equals("Positive")) {
            setSentimentNegative_currentText.add(negationWordWithSentiment);
            newVocSentiment.setPolarity("Negative");
        } else {
            setSentimentPositive_currentText.add(negationWordWithSentiment);
            newVocSentiment.setPolarity("Positive");
        }
        newVocSentiment.setSentimentText(negationWordWithSentiment);
        //newVocSentiment.setPolarityConfidence(getSentimentPolarity(negationWordWithSentiment));
        newVocSentiment.setPolarityConfidence(getSentimentPolarity(strSentiment.toLowerCase())); //use original word to get confidence level

        setVocSentiment_currentText.add(newVocSentiment);
        return newVocSentiment;
    }

    public static void removeSentimentOverlappingWithEntity(VocTextMiningResult vocTextMiningResult) {

        if (DEBUG_LOG_OUTPUT) {
            System.out.println("");
            System.out.println("------------------------------------------------");
            System.out.println("--- Remove Sentiment Overlapping with Entity ---");
            System.out.println("------------------------------------------------");
        }

        Set<VocEntity> setNER_copy = new HashSet<>();
        Set<VocSentiment> setSentimentAll_copy = new HashSet<>();
        setNER_copy.addAll(vocTextMiningResult.setNER);
        setSentimentAll_copy.addAll(vocTextMiningResult.setSentimentAll);

        //eg: setNER_copy = { "A B", "A", "C" }
        //target result = { "A B", "C" } , remove "A"        
        for (VocEntity vocEntity : setNER_copy) {
            //"A B"
            //compare entity with all other sentiment to find overlapping sentiment with entity
            for (VocSentiment vocSentimentAll_copy : setSentimentAll_copy) {
                //"A B" vs "A B" 
                //"A B" vs "A"
                //"A B" vs "C"
                //eg if loop #2, "A B" contains "A"
                System.out.println("Find overlapping entity[" + vocEntity.getEntity() + "] vs sentiment[" + vocSentimentAll_copy.getSentimentText() + "]");
                if (vocEntity.getEntity().contains(vocSentimentAll_copy.getSentimentText())) {
                    if (vocEntity.getEntity().length()
                            > vocSentimentAll_copy.getSentimentText().length()) {

                        boolean isConfirmOverlapping = true;

                        //--double checking with word position
                        //TM baik pulih adalah baik
                        // 1  2    3     4      5
                        //remove sentiment[baik] only at wordPosition=2, but remains sentiment 5
                        String strCurrentSentence = vocTextMiningResult.normalizedText.toLowerCase();
                        String strEntity = vocEntity.getEntity().toLowerCase();
                        String strSentimentText = vocSentimentAll_copy.sentimentText.toLowerCase();
                        List<Integer> listLocationIdxEntity = getAllStringLocationIdx(strCurrentSentence, strEntity);
                        List<Integer> listLocationIdxSentiment = getAllStringLocationIdx(strCurrentSentence, strSentimentText);
                        //System.out.println(listLocationIdxSentiment);        
                        for (int locationIdxEntity : listLocationIdxEntity) {
                            int entityWordIdx = getWordLocationIdx(strCurrentSentence, locationIdxEntity, strEntity);
                            System.out.println("\tLoop entity[" + strEntity + "] at[" + entityWordIdx + "]");
                            for (int locationIdxSentiment : listLocationIdxSentiment) {
                                int sentimentWordIdx = getWordLocationIdx(strCurrentSentence, locationIdxSentiment, strSentimentText);
                                System.out.println("\t\tLoop sentiment[" + strSentimentText + "] at[" + sentimentWordIdx + "]");
                                int entityWordCount = getWordsCount(strEntity);
                                int sentimentWordCount = getWordsCount(strSentimentText);
                                int entityWordIdxEnd = entityWordIdx + (entityWordCount - 1);
                                int sentimentWordIdxEnd = sentimentWordIdx + (sentimentWordCount - 1);
                                System.out.println("\t\t\tentity[" + strEntity + "] startEndIdx[" + entityWordIdx + "," + entityWordIdxEnd + "]");
                                System.out.println("\t\t\tsentiment[" + strSentimentText + "] startEndIdx[" + sentimentWordIdx + "," + sentimentWordIdxEnd + "]");
                                if (sentimentWordIdx >= entityWordIdx && sentimentWordIdx <= entityWordIdxEnd) {
                                    System.out.println("\t\t\toverlapping found...");
                                } else {
                                    isConfirmOverlapping = false; //found word which not overlapping
                                }
                            }
                        }

                        if (isConfirmOverlapping) {
                            System.out.println("\tResult: overlapping found.. remove sentiment[" + vocSentimentAll_copy.getSentimentText() + "]");
                            //"A B" length > "A" length than remove "A"
                            //found similar entity with longest text, remove entity with shortest text                            
                            vocTextMiningResult.setSentimentAll.remove(vocSentimentAll_copy);
                            vocTextMiningResult.setSentimentNegative.remove(vocSentimentAll_copy.getSentimentText());
                            vocTextMiningResult.setSentimentPositive.remove(vocSentimentAll_copy.getSentimentText());
                        }

                    }
                }
//                }
            }
        }
    }

    public static void removeDuplicateValue(VocTextMiningResult vocTextMiningResult) {
        Set<VocEntity> setNER_copy = new HashSet<>();
        Set<VocSentiment> setSentimentAll_copy = new HashSet<>();
        setNER_copy.addAll(vocTextMiningResult.setNER);
        setSentimentAll_copy.addAll(vocTextMiningResult.setSentimentAll);

        //eg: setNER_copy = { "A B", "A", "C" }
        //target result = { "A B", "C" } , remove "A"        
        for (VocEntity vocEntity : setNER_copy) {
            //"A B"
            //compare entity with all other entity to find duplicate entity
            for (VocEntity vocEntity_copy : setNER_copy) {
                //"A B" vs "A B" 
                //"A B" vs "A"
                //"A B" vs "C"
                //eg if loop #2, "A B" contains "A"
//                if (vocEntity.getNER().equals(vocEntity_copy.getNER())) {
                if (vocEntity.getEntity().contains(vocEntity_copy.getEntity())) {
                    if (vocEntity.getEntity().length()
                            > vocEntity_copy.getEntity().length()) {
                        //"A B" length > "A" length than remove "A"
                        //found similar entity with longest text, remove entity with shortest text                            
                        vocTextMiningResult.setNER.remove(vocEntity_copy);
                    }
                }
//                }
            }
        }

        for (VocSentiment vocSentiment : setSentimentAll_copy) {
            for (VocSentiment vocSentiment_copy : setSentimentAll_copy) {
                if (vocSentiment.getSentimentText().contains(vocSentiment_copy.getSentimentText())) {
                    if (vocSentiment.getSentimentText().length()
                            > vocSentiment_copy.getSentimentText().length()) {
                        //bugfix: check inverse negation word before removing any sentiment
                        //skip remove sentiment if another sentiment is actualy a different sentiment 
                        String strLongerText = vocSentiment.getSentimentText();
                        String strShorterText = vocSentiment_copy.getSentimentText();
                        String strLongerTextMinusShorterText = strLongerText.replaceAll(strShorterText, "");
                        if (!setNegationWord.contains(strLongerTextMinusShorterText)) {
                            vocTextMiningResult.setSentimentAll.remove(vocSentiment_copy);
                            vocTextMiningResult.setSentimentNegative.remove(vocSentiment_copy.getSentimentText());
                            vocTextMiningResult.setSentimentPositive.remove(vocSentiment_copy.getSentimentText());
                        }
                    }
                }
//                }
            }
        }
    }

    public static boolean analyzeSentenceEntitySentimentRelationship(
            VocTextMiningResult vocTextMiningResult,
            List<VocEntitySentiment> listVocEntitySentiment,
            VocEntitySentiment vocClosestEntitySentiment,
            VocEntity vocEntity,
            String strCurrentSentence, String strEntity,
            VocSentiment vocSentiment) {
        boolean foundClosestEntitySentiment = false;

        String strSentimentText = vocSentiment.getSentimentText();
        String strSentimentPolarity = vocSentiment.getPolarity();
        double sentimentPolarityConf = vocSentiment.getPolarityConfidence();

        if (VocmineUtil.isTextContains(strCurrentSentence, strEntity)
                && VocmineUtil.isTextContains(strCurrentSentence, strSentimentText)) {
//                        log.info("\t\tsentenceNo[" + j + "]=" + strCurrentSentence);
            //int entityIdx = strCurrentSentence.toLowerCase().indexOf(strEntity.toLowerCase());
            //int sentimentIdx = strCurrentSentence.toLowerCase().indexOf(strSentimentText.toLowerCase());
            //TODO: loop multiple entities
            //1st find all entities and save in list
            //2nd find all sentiment and save in list
            //loop each entity location
            //  loop each sentiment location
            //    --continue existing logic on checking
            //  end
            //end

            List<Integer> listLocationIdxEntity = getAllStringLocationIdx(strCurrentSentence, strEntity);
            List<Integer> listLocationIdxSentiment = getAllStringLocationIdx(strCurrentSentence, strSentimentText);

            for (int locationIdxEntity : listLocationIdxEntity) {
                System.out.println("\nloop locationIdxEntity[" + strEntity + "]=" + locationIdxEntity);
                for (int locationIdxSentiment : listLocationIdxSentiment) {
                    System.out.println("\tloop locationIdxSentiment[" + strSentimentText + "]=" + locationIdxSentiment);
                    System.out.println("\tstrCurrentSentence.substring(locationIdxSentiment)=" + strCurrentSentence.substring(locationIdxSentiment));

                    System.out.println("\twordLocation Entity[" + strEntity + "]=" + getWordLocationIdx(strCurrentSentence, locationIdxEntity, strEntity));
                    System.out.println("\twordLocation Sentiment[" + strSentimentText + "]=" + getWordLocationIdx(strCurrentSentence, locationIdxSentiment, strSentimentText));
                    System.out.println("\twordEndLocation Sentiment[" + strSentimentText + "]=" + getWordLocationEndIdx(strCurrentSentence, locationIdxSentiment, strSentimentText));

                    //int entityIdx = getWordLocationIdx(strCurrentSentence.toLowerCase(), strEntity.toLowerCase());
                    //int sentimentIdx = getWordLocationIdx(strCurrentSentence.toLowerCase(), strSentimentText.toLowerCase());
                    int entityIdx = getWordLocationIdx(strCurrentSentence.toLowerCase(), locationIdxEntity, strEntity.toLowerCase());
                    int sentimentStartIdx = getWordLocationIdx(strCurrentSentence.toLowerCase(), locationIdxSentiment, strSentimentText.toLowerCase());
                    int sentimentEndIdx = getWordLocationEndIdx(strCurrentSentence.toLowerCase(), locationIdxSentiment, strSentimentText.toLowerCase());
                    if (sentimentStartIdx != -1) {
                        int distance = 0;
                        if (sentimentStartIdx > entityIdx)
                            distance = Math.abs(sentimentStartIdx - entityIdx);
                        else if (sentimentStartIdx < entityIdx)
                            distance = Math.abs(entityIdx - sentimentEndIdx);
                        
                        //int distance = Math.abs(sentimentIdx - entityIdx);
//                        log.info("\t\tfound entity at:" + entityIdx);
//                        log.info("\t\tfound sentiment at:" + sentimentIdx);
//                        log.info("\t\tdistance entity and sentiment:" + distance);
                        if (vocClosestEntitySentiment.getDistance() == 0
                                || distance < vocClosestEntitySentiment.getDistance()) {
                            vocClosestEntitySentiment.setNER(vocEntity.getNER());
                            vocClosestEntitySentiment.setEntity(vocEntity.getEntity());
                            vocClosestEntitySentiment.setSentimentPolarity(strSentimentPolarity);
                            vocClosestEntitySentiment.setSentimentPolarityConfidence(sentimentPolarityConf);
                            vocClosestEntitySentiment.setSentimentText(strSentimentText);
                            vocClosestEntitySentiment.setEntityIdx(entityIdx);
                            vocClosestEntitySentiment.setSentimentIdx(sentimentStartIdx);
                            vocClosestEntitySentiment.setDistance(distance);
                            //System.out.println("Found new distance=" + distance);
                            vocClosestEntitySentiment.appendNewDistanceAndCalculateConfidence(distance);
                            foundClosestEntitySentiment = true;
                        }
                        else if (distance == vocClosestEntitySentiment.getDistance() && 
                                strSentimentText.length() > vocClosestEntitySentiment.getSentimentText().length()) {
                            vocClosestEntitySentiment.setNER(vocEntity.getNER());
                            vocClosestEntitySentiment.setEntity(vocEntity.getEntity());
                            vocClosestEntitySentiment.setSentimentPolarity(strSentimentPolarity);
                            vocClosestEntitySentiment.setSentimentPolarityConfidence(sentimentPolarityConf);
                            vocClosestEntitySentiment.setSentimentText(strSentimentText);
                            vocClosestEntitySentiment.setEntityIdx(entityIdx);
                            vocClosestEntitySentiment.setSentimentIdx(sentimentStartIdx);
                            vocClosestEntitySentiment.setDistance(distance);
                            //System.out.println("Found new distance=" + distance);
                            vocClosestEntitySentiment.appendNewDistanceAndCalculateConfidence(distance);
                            foundClosestEntitySentiment = true;
                        }
                        VocEntitySentiment vocEntitySentiment = new VocEntitySentiment();
                        vocEntitySentiment.setNER(vocEntity.getNER());
                        vocEntitySentiment.setEntity(vocEntity.getEntity());
                        vocEntitySentiment.setSentimentPolarity(strSentimentPolarity);
                        vocEntitySentiment.setSentimentPolarityConfidence(sentimentPolarityConf);
                        vocEntitySentiment.setSentimentText(strSentimentText);
                        vocEntitySentiment.setEntityIdx(entityIdx);
                        vocEntitySentiment.setSentimentIdx(sentimentStartIdx);
                        vocEntitySentiment.setDistance(distance);
                        listVocEntitySentiment.add(vocEntitySentiment);
                        vocTextMiningResult.listAllEntitySentiment.add(vocEntitySentiment);
                    }
                }
            }

        }
        return foundClosestEntitySentiment;
    }

    public static VocTextMiningResult analyzeEntitySentimentRelationship(VocTextMiningResult vocTextMiningResult) {
//        String[] normalizedTextSplitSentence = vocTextMiningResult.normalizedText.split("\\.|,");
        String[] normalizedTextSplitSentence = vocTextMiningResult.normalizedText.toLowerCase().split("\\.");

        if (DEBUG_LOG_OUTPUT) {
            System.out.println("");
            System.out.println("---------------------------------------------");
            System.out.println("--- Analyze Entity Sentiment Relationship ---");
            System.out.println("---------------------------------------------");
        }

        List<VocEntitySentiment> listVocEntitySentiment = new ArrayList();

        if (DEBUG_LOG_OUTPUT) {
            System.out.println("loop entity, find closest sentiment...");
        }
        for (VocEntity vocEntity : vocTextMiningResult.setNER) {
            String strEntity = vocEntity.getEntity();
            if (DEBUG_LOG_OUTPUT) {
                System.out.println("\nvocEntity[" + vocEntity + "]");
            }

            VocEntitySentiment vocClosestEntitySentiment = new VocEntitySentiment();
            boolean foundClosestEntitySentiment = false;

            for (VocSentiment vocSentiment : vocTextMiningResult.setSentimentAll) {
                if (DEBUG_LOG_OUTPUT) {
                    System.out.println("\n\tloop vocSentiment=" + vocSentiment.toString());
                }

                //loop sentence
                for (int j = 0; j < normalizedTextSplitSentence.length; j++) {
                    String strCurrentSentence = normalizedTextSplitSentence[j];
                    if (DEBUG_LOG_OUTPUT) {
                        System.out.println("\n\t\tLoop sentenceNo[" + j + "]=" + strCurrentSentence);
                    }

                    if (analyzeSentenceEntitySentimentRelationship(
                            vocTextMiningResult,
                            listVocEntitySentiment,
                            vocClosestEntitySentiment,
                            vocEntity,
                            strCurrentSentence, strEntity, vocSentiment)) {
                        foundClosestEntitySentiment = true;
                        if (DEBUG_LOG_OUTPUT) {
                            System.out.println("\t\tfoundClosestEntitySentiment = true");
                        }
                    }

                }

            }
            if (foundClosestEntitySentiment) {
                if (DEBUG_LOG_OUTPUT) {
                    System.out.println("\n\tAdd ClosestEntitySentiment for vocEntity[" + vocEntity + "] = " + vocClosestEntitySentiment);
                }
                vocTextMiningResult.listClosestEntitySentiment.add(vocClosestEntitySentiment);
            } else //try run without sentence split
             if (DEBUG_FIND_SENTIMENT_ACROSS_SENTENCES) {
                    //----------TODO: duplicate code ; kena refactor jadi function
                    //                for (String strSentiment : vocTextMiningResult.setSentimentNegative) {
                    for (VocSentiment vocSentiment : vocTextMiningResult.setSentimentAll) {
                        //                    log.info("\t" + strSentiment);

                        String strCurrentSentence = vocTextMiningResult.normalizedText;

                        if (analyzeSentenceEntitySentimentRelationship(
                                vocTextMiningResult,
                                listVocEntitySentiment,
                                vocClosestEntitySentiment,
                                vocEntity,
                                strCurrentSentence, strEntity, vocSentiment)) {
                            foundClosestEntitySentiment = true;
                        }

                    }
                    if (foundClosestEntitySentiment) {
                        //            log.info("vocClosestEntitySentiment=" + vocClosestEntitySentiment);
                        vocTextMiningResult.listClosestEntitySentiment.add(vocClosestEntitySentiment);
                    }
                    //---------
                }
        }

        return vocTextMiningResult;
    }

    public static double getSentimentPolarity(String sentiment) {
        VmSentimentLexicon vmSentimentLexicon = mapVmSentimentLexicon.get(sentiment);
        if (vmSentimentLexicon != null && vmSentimentLexicon.getConfScore() != null) {
            BigDecimal bdPolarityConf = vmSentimentLexicon.getConfScore();
            if (bdPolarityConf != null) {
                return bdPolarityConf.doubleValue();
            }
        }
        return 0.0;
    }

    public static int getWordsCount(String s) {
        //ref: https://stackoverflow.com/questions/5864159/count-words-in-a-string-method
        int wordCount = 0;

        boolean word = false;
        int endOfLine = s.length() - 1;

        for (int i = 0; i < s.length(); i++) {
            // if the char is a letter, word = true.
            if (Character.isLetter(s.charAt(i)) && i != endOfLine) {
                word = true;
                // if char isn't a letter and there have been letters before,
                // counter goes up.
            } else if (!Character.isLetter(s.charAt(i)) && word) {
                wordCount++;
                word = false;
                // last word of String; if it doesn't end with a non letter, it
                // wouldn't count without this.
            } else if (Character.isLetter(s.charAt(i)) && i == endOfLine) {
                wordCount++;
            }
        }
        return wordCount;
    }

    public static int getWordLocationIdx(String strText, int startIdx, String wordToFind) {
        //9 June 2017 - change text to lowercase, ignore case comparison
        strText = strText.toLowerCase();
        wordToFind = wordToFind.toLowerCase();
        
        int wordLocation = -1;
        try {
            strText = strText.trim().replaceAll(" +", " "); //remove duplicate space

            int wordIdx = strText.indexOf(wordToFind, startIdx);
            if (wordIdx >= 0) {
                //System.out.println("wordIdx=" + wordIdx);
                if (wordIdx == 0) {
                    wordLocation = 1;
                } else {
                    String strTextSubstr = strText.substring(0, wordIdx);
                    //System.out.println(strTextSubstr);

                    wordLocation = StringUtils.countMatches(strTextSubstr + " ", " ");
                    //String strTextSubstrSplit[] = strTextSubstr.split(" ");
                    //wordLocation = strTextSubstrSplit.length + 1;
                }
            }
        } catch (Exception ex) {
            //
        }
        return wordLocation;
    }
    
    public static int getWordLocationEndIdx(String strText, int startIdx, String wordToFind) {
        //9 June 2017 - change text to lowercase, ignore case comparison
        strText = strText.toLowerCase();
        wordToFind = wordToFind.toLowerCase();
        
        int numWords = StringUtils.countMatches(wordToFind + " ", " ");

        int wordEndLocation = -1;
        try {
            strText = strText.trim().replaceAll(" +", " "); //remove duplicate space

            int wordIdx = strText.indexOf(wordToFind, startIdx);
            if (wordIdx >= 0) {
                //System.out.println("wordEndIdx=" + wordEndIdx);                
                
                if (wordIdx == 0) {
                    wordEndLocation = 1;
                    if (numWords > 1) wordEndLocation = numWords - 1;
                    
                } else {
                    String strTextSubstr = strText.substring(0, wordIdx);
                    //System.out.println(strTextSubstr);

                    wordEndLocation = StringUtils.countMatches(strTextSubstr + " ", " ");
                    if (numWords > 1) wordEndLocation += numWords - 1;
                }
            }
        } catch (Exception ex) {
            //
        }
        return wordEndLocation;
    }

    public static List<Integer> getAllStringLocationIdx(String str, String wordToFind) {
        //9 June 2017 - change text to lowercase, ignore case comparison
        str = str.toLowerCase();
        wordToFind = wordToFind.toLowerCase();

        str = str.trim().replaceAll(" +", " "); //remove duplicate space        

        List<Integer> listStringLocationIdx = new ArrayList();
        int index = str.indexOf(wordToFind);
        while (index >= 0) {
            //System.out.println(index);
            listStringLocationIdx.add(index);

            //find next index 
            index = str.indexOf(wordToFind, index + 1);

        }
        return listStringLocationIdx;
    }
}
