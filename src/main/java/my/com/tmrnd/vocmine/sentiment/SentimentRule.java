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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import my.com.tmrnd.vocmine.db.entity.VmNegationLexicon;
import my.com.tmrnd.vocmine.db.entity.VmSentimentLexicon;
import my.com.tmrnd.vocmine.entity.VocSentiment;
import my.com.tmrnd.vocmine.util.DBUtil;
import my.com.tmrnd.vocmine.util.FileUtil;
import my.com.tmrnd.vocmine.util.IOUtil;


/**
 *
 * @author Administrator
 */
public class SentimentRule {

    public SentimentRule() {

    }

    public Map<String, VmSentimentLexicon> mapVmSentimentLexicon = new HashMap<>();
    public Set<String> setSentimentPositive_template = new HashSet<>();
    public Set<String> setSentimentNegative_template = new HashSet<>();
    public Set<VocSentiment> setVocSentiment_template = new HashSet<>();
    public Set<String> setNegationLexicon_template = new HashSet<>();
    public boolean DEBUG_INIT_RULE_FROM_DB_VM_TAGGED_LEXICON = false;

    public void insertRuleInDB() {

        for (VocSentiment vocSentiment : setVocSentiment_template) {
            DBUtil.vmSentimentLexiconDbService.insertSentimentRule(vocSentiment.getPolarity(), vocSentiment.getSentimentText());
        }

    }

    public void loadRuleFromDB() {

        List<VmSentimentLexicon> listVmSentimentLexicon = null;
        if (DEBUG_INIT_RULE_FROM_DB_VM_TAGGED_LEXICON) {
            listVmSentimentLexicon = DBUtil.vmSentimentLexiconDbService.loadSentimentLexicon_debugFromVmTaggedLexicon();
        } else {
            listVmSentimentLexicon = DBUtil.vmSentimentLexiconDbService.loadSentimentLexicon();
        }
        for (int i = 0; i < listVmSentimentLexicon.size(); i++) {

            String strSentimentType = listVmSentimentLexicon.get(i).getPolarity();
            String strSentimentText = listVmSentimentLexicon.get(i).getText();
            BigDecimal bdConfScore = listVmSentimentLexicon.get(i).getConfScore();
            VocSentiment vocSentiment = new VocSentiment();
            vocSentiment.setSentimentText(strSentimentText);
            vocSentiment.setPolarity(strSentimentType);
            if (bdConfScore != null) {
                vocSentiment.setPolarityConfidence(bdConfScore.doubleValue());
            }

            setVocSentiment_template.add(vocSentiment);
            mapVmSentimentLexicon.put(strSentimentText, listVmSentimentLexicon.get(i));

            if (strSentimentType.equals("Positive")) {
                setSentimentPositive_template.add(strSentimentText);
            } else {
                if (strSentimentType.equals("Negative")) {
                    setSentimentNegative_template.add(strSentimentText);
                } else {
                    System.out.println("Error");
                }
            }
        }
        List<VmNegationLexicon> listVmSaRuleNegationLexicon = DBUtil.vmNegationLexiconDbService.loadNegationLexicon();
        for (int i = 0; i < listVmSaRuleNegationLexicon.size(); i++) {
            String strNegationWord = listVmSaRuleNegationLexicon.get(i).getText();
            setNegationLexicon_template.add(strNegationWord);
        }

        System.out.println("Template: Total mapVmSentimentLexicon = " + mapVmSentimentLexicon.size());
        System.out.println("Template: Total setSentimentNegative  = " + setSentimentNegative_template.size());
        System.out.println("Template: Total setSentimentPositive  = " + setSentimentPositive_template.size());
        System.out.println("Template: Total setSentimentAll       = " + setVocSentiment_template.size());

    }

    public void saveRuleToFile() {

        StringBuilder sbSentiment = new StringBuilder();
        for (VocSentiment vocSentiment : setVocSentiment_template) {
            sbSentiment.append(vocSentiment.getPolarity()).append(",")
                    .append(vocSentiment.getSentimentText()).append("\n");
        }
        Path path3 = FileSystems.getDefault().getPath("", "setVocSentiment_template.txt");
        FileUtil.writeSmallFile(path3, sbSentiment.toString());
    }

    public void loadRuleFromClassResource() {
        loadRuleFromFileOrResource("CLASS_RESOURCE");
    }

    public void loadRuleFromFile() {
        loadRuleFromFileOrResource("FILE");
    }

    public void loadRuleFromFileOrResource(String sourceType) {

        String strSetVocSentiment_template = "";

        if (sourceType.equals("FILE")) {

            Path path3 = FileSystems.getDefault().getPath("", "setVocSentiment_template.txt");
            strSetVocSentiment_template = FileUtil.readSmallFile(path3);
        } else {
            strSetVocSentiment_template = IOUtil.getFileContentFromClassResource(SentimentRule.class, "setVocSentiment_template.txt");
        }

        setVocSentiment_template.clear();
        setSentimentPositive_template.clear();
        setSentimentNegative_template.clear();
        String[] listVmSaRuleSentiment = strSetVocSentiment_template.split("\n");
        for (int i = 0; i < listVmSaRuleSentiment.length; i++) {
            String strVmSaRuleSentiment[] = listVmSaRuleSentiment[i].split(",");
            String strSentimentType = strVmSaRuleSentiment[0];
            String strSentimentText = strVmSaRuleSentiment[1];
            VocSentiment vocSentiment = new VocSentiment();
            vocSentiment.setSentimentText(strSentimentText);
            vocSentiment.setPolarity(strSentimentType);
            vocSentiment.setPolarityConfidence(getSentimentPolarity(strSentimentText));

            setVocSentiment_template.add(vocSentiment);
            if (strSentimentType.equals("Positive")) {
                setSentimentPositive_template.add(strSentimentText);
            } else {
                if (strSentimentType.equals("Negative")) {
                    setSentimentNegative_template.add(strSentimentText);
                } else {
                    System.out.println("Error");
                }
            }
        }

        System.out.println("Template: Total setSentimentNegative = " + setSentimentNegative_template.size());
        System.out.println("Template: Total setSentimentPositive = " + setSentimentPositive_template.size());
        System.out.println("Template: Total setSentimentAll = " + setVocSentiment_template.size());
    }

    public double getSentimentPolarity(String sentiment) {
        VmSentimentLexicon vmSentimentLexicon = mapVmSentimentLexicon.get(sentiment);
        if (vmSentimentLexicon != null && vmSentimentLexicon.getConfScore() != null) {
            BigDecimal bdPolarityConf = vmSentimentLexicon.getConfScore();
            if (bdPolarityConf != null) {
                return bdPolarityConf.doubleValue();
            }
        }
        return 0.0;
    }

}
