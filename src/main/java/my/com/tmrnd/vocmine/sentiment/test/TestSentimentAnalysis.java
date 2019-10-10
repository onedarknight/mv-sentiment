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
package my.com.tmrnd.vocmine.sentiment.test;

import my.com.tmrnd.vocmine.sentiment.SentimentRule;
import my.com.tmrnd.vocmine.sentiment.SentimentAnalysisUtil;
import my.com.tmrnd.vocmine.entity.VocTextMiningResult;
import com.cybozu.labs.langdetect.LangDetectException;
import java.io.IOException;
import my.com.tmrnd.vocmine.db.services.VmSentimentLexiconDbService;
import my.com.tmrnd.vocmine.ner.NER_RecognitionUtil;
import my.com.tmrnd.vocmine.ner.NER_Rule;
import my.com.tmrnd.vocmine.util.DBUtil;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Administrator
 */
public class TestSentimentAnalysis {

    private static final Logger log = LoggerFactory.getLogger(TestSentimentAnalysis.class.getName());

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws LangDetectException, IOException {
        BasicConfigurator.configure();

        //----------------------------------------------------------------------
        // 1. Init NER_RecognitionUtil
        //----------------------------------------------------------------------
        NER_Rule nerRule = new NER_Rule();
        DBUtil.createDbConnection();
        nerRule.loadRuleFromDB();
        NER_RecognitionUtil.setNER_Lexicon(nerRule.getNERLexicon());

        //----------------------------------------------------------------------
        // 2. Init SentimentAnalysisUtil
        //----------------------------------------------------------------------
        SentimentAnalysisUtil.DEBUG_SKIP_NORMALIZE_TEXT = true;
        SentimentAnalysisUtil.DEBUG_SA_RULE_SENTIMENT_NEGATION = true;
        VmSentimentLexiconDbService.DEBUG_USE_SA_RULE_SENTIMENT_WORDNET = false;
//        DEBUG_LOOP = true;

        SentimentRule sentimentRule = new SentimentRule();

        //option1
//        sentimentRule.loadRuleFromFile();
        //option2
//        sentimentRule.loadRuleFromClassResource();
        //option3
        DBUtil.createDbConnection();
        sentimentRule.loadRuleFromDB();

//        SentimentAnalysisUtil.DEBUG_USE_NEW_NORMALIZATION_ALGO = false;  //default:true      
        SentimentAnalysisUtil.setRule(sentimentRule);

        //----------------------------------------------------------------------
//        String strText = "0 semalam adalah temujanji pemasangan yang ke-empat  yang tidak ditetapi. Agen pemasangan yang di SMS kepada saya memberitahu dia bukan agen di Kawasan saya.";
//        String strText = "0 semalam adalah temujanji pemasangan yang ke-empat  yang tidak ditetapi.";
//        String strText = "0 semasa saya talipon saya dimaklumkan sudah ada 56 pengguna di kawasan saya yang mengalami masalah connection seperti saya. Mengapa perlu tunggu saya ia itu yg";
//        String strText = "\"0\" sebab Mereka kata cable rosak dan TM tidak baikinya dan kami perlu upah kontraktor persendirian walaupun kami tidak menyebabkan atau bertanggungjawab ke atas k";
//        String strText = "0 Sampai sekarang belum settle. Internet slow tapi kenapa dalam report no dial tone. Mcm nie ke service? Charge mahal. Kena bar pun charge. Haram ambil d uit or";
//        String strText = "0 sangat mengecewakan kerana aduan tersebut tiada kaitan dengan saya, no telefon 0360342019 adalah di rawang sedangkn saya tinggal di telok panglima garang banti ng. Ma";
//        String strText = "TM tidak baik"; //test negation word
//        String strText = "only \"done\" after "; //test symbol encloser
//        String strText = "pekerja tm"; //test duplicate entities
//        String strText = "slow speed"; //test duplicate sentiment
//        String strText = "0 lambat bertindak. Report lapor pada  masa 2/10/16 sampai sekarang masih belum pulih. Saya berharap dapat menuntut kerugian saya secara potongan bill bu lanan."; 
//        String strText = "10 pemasangan bagus.cuma sistem customer servis tm sangat tidak bagus.";
//        String strText = "tidak ada wifi"; 
//        String strText = "service banyak terok!!! 25/9/15 buat report. tunggu tunggu fone pun banyak kali baru teknician awak mari 30/9/15. masa sekarang it age, tidak ada wifi";
        //String strText = "tm bagus dan baik"; //test ner confidence score
        //String strText = "\"0\" sbb Mereka kata cable rosak dan TM tak baikinya dan kami perlu upah kontraktor persendirian walaupun kami tidak menyebabkan atau bertanggungjawab ke  atas k"; //test ner confidence score
        //String strText = "\"0\" sbb Mereka kata cable rosak dan TM tak baikinya dan kami perlu upah kontraktor persendirian walaupun kami tidak menyebabkan atau bertanggungjawab ke atas k"; //test ner confidence score
        //String strText = "Technician tidak bagus"; //test ner confidence score
        //String strText = "technician come late"; //test negation late
        //String strText = "kurang memuaskan kerana mereka hanya menarik wayar tetapi tidak menyambung kembali wayar yg terputus"; //test multiple entity vs multiple sentiment
        //String strText = "2.	5 kurang memuaskan kerana mereka hnya menarik wayar tetapi tidak menyambung kembali wayar yg terputus..sebaliknya mereka meminta bayaran utk membuat samb ungan"; //test multiple entity vs multiple sentiment
        //String strText = "5 kurang memuaskan kerana mereka hnya menarik wayar tetapi tidak menyambung kembali wayar yg terputus.. sebaliknya mereka meminta bayaran utk membuat samb ungan"; //test multiple entity vs multiple sentiment
        //String strText = " .. sebaliknya mereka meminta bayaran utk membuat samb ungan"; //test multiple entity vs multiple sentiment
        //String strText = "sangat bagus"; //test sentiment without entity
        //String strText = "ok"; //test sentiment without entity
        //String strText = "TM"; //test entity without sentiment
        //String strText = ""; //test empty null string
        //String strText = null; //test empty null string        
        //String strText = "talian tm sangat baik"; //test missing sentiment
        //String strText = "buat masa ini internet ok tetapi talian telefon tidak ok seyap"; //test two sentiment & inverse sentiment in one sentence
        //String strText = "internet ok tetapi telefon tidak ok"; //test two sentiment & inverse sentiment in one sentence
        //String strText = "internet tidak tidak baik"; //test two sentiment & inverse sentiment in one sentence
        //String strText = "TM baik pulih telah tidak pulih"; //test entity overlapping with sentiment        
        //String strText = "baik pulih belum siap"; //test entity overlapping with sentiment
        //String strText = "saya tidak suka tm tapi saya suka technician";
        String strText = "saya suka tm tapi saya tidak suka technician";
        SentimentAnalysisUtil.DEBUG_LOG_OUTPUT = true;
        SentimentAnalysisUtil.DEBUG_FIND_SENTIMENT_ACROSS_SENTENCES = false;
        VocTextMiningResult vocTextMiningResult = SentimentAnalysisUtil.runVocTextMiningProcess("id1", strText);
//        log.info(vocTextMiningResult.toString());
        //----------------------------------------------------------------------
    }

}
