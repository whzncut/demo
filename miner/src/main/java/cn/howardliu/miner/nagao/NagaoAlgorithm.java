package cn.howardliu.miner.nagao;

import cn.howardliu.miner.StringUtils;

import java.io.*;
import java.util.*;

public class NagaoAlgorithm {
    private int N = 5;//default N = 5
    private List<String> leftPTable = new ArrayList<>();
    private int[] leftLTable;
    private List<String> rightPTable = new ArrayList<>();
    private int[] rightLTable;
    private double wordNumber;
    private Map<String, TFNeighbor> wordTFNeighbor = new HashMap<>();
    private final static String stopwords = "的很了么呢是嘛个都也比还这于不与才上用就好在和对挺去后没说";

    private NagaoAlgorithm() {
    }

    //add substring of line to pTable
    private void add2PTable(String line) {
        //split line according to consecutive none Chinese character
        String[] phrases = line.split("[^\u4E00-\u9FA5]+|[" + stopwords + "]");
        for (String phrase : phrases) {
            for (int i = 0; i < phrase.length(); i++) {
                rightPTable.add(phrase.substring(i));
            }
            String reversePhrase = StringUtils.reverse(phrase);
            for (int i = 0; i < reversePhrase.length(); i++) {
                leftPTable.add(reversePhrase.substring(i));
            }
            wordNumber += phrase.length();
        }
    }

    //count lTable
    private void countLTable() {
        Collections.sort(rightPTable);
        rightLTable = new int[rightPTable.size()];
        for (int i = 1; i < rightPTable.size(); i++) {
            rightLTable[i] = StringUtils.coPrefixLength(rightPTable.get(i - 1), rightPTable.get(i));
        }

        Collections.sort(leftPTable);
        leftLTable = new int[leftPTable.size()];
        for (int i = 1; i < leftPTable.size(); i++) {
            leftLTable[i] = StringUtils.coPrefixLength(leftPTable.get(i - 1), leftPTable.get(i));
        }

        System.out.println("Info: [Nagao Algorithm Step 2]: having sorted PTable and counted left and right LTable");
    }

    //according to pTable and lTable, count statistical result: TF, neighbor distribution
    private void countTFNeighbor() {
        //get TF and right neighbor
        for (int pIndex = 0; pIndex < rightPTable.size(); pIndex++) {
            String phrase = rightPTable.get(pIndex);
            for (int length = 1 + rightLTable[pIndex]; length <= N && length <= phrase.length(); length++) {
                String word = phrase.substring(0, length);
                TFNeighbor tfNeighbor = new TFNeighbor();
                tfNeighbor.incrementTF();
                if (phrase.length() > length) {
                    tfNeighbor.addToRightNeighbor(phrase.charAt(length));
                }
                for (int lIndex = pIndex + 1; lIndex < rightLTable.length; lIndex++) {
                    if (rightLTable[lIndex] >= length) {
                        tfNeighbor.incrementTF();
                        String coPhrase = rightPTable.get(lIndex);
                        if (coPhrase.length() > length) {
                            tfNeighbor.addToRightNeighbor(coPhrase.charAt(length));
                        }
                    } else {
                        break;
                    }
                }
                wordTFNeighbor.put(word, tfNeighbor);
            }
        }
        //get left neighbor
        for (int pIndex = 0; pIndex < leftPTable.size(); pIndex++) {
            String phrase = leftPTable.get(pIndex);
            for (int length = 1 + leftLTable[pIndex]; length <= N && length <= phrase.length(); length++) {
                String word = StringUtils.reverse(phrase.substring(0, length));
                TFNeighbor tfNeighbor = wordTFNeighbor.get(word);
                if (phrase.length() > length) {
                    tfNeighbor.addToLeftNeighbor(phrase.charAt(length));
                }
                for (int lIndex = pIndex + 1; lIndex < leftLTable.length; lIndex++) {
                    if (leftLTable[lIndex] >= length) {
                        String coPhrase = leftPTable.get(lIndex);
                        if (coPhrase.length() > length) {
                            tfNeighbor.addToLeftNeighbor(coPhrase.charAt(length));
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        System.out.println("Info: [Nagao Algorithm Step 3]: having counted TF and Neighbor");
    }

    //according to wordTFNeighbor, count MI of word
    private double countMI(String word) {
        if (word.length() <= 1) {
            return 0;
        }
        double coProbability = wordTFNeighbor.get(word).getTF() / wordNumber;
        List<Double> mi = new ArrayList<>(word.length());
        for (int pos = 1; pos < word.length(); pos++) {
            String leftPart = word.substring(0, pos);
            String rightPart = word.substring(pos);
            double leftProbability = wordTFNeighbor.get(leftPart).getTF() / wordNumber;
            double rightProbability = wordTFNeighbor.get(rightPart).getTF() / wordNumber;
            mi.add(coProbability / (leftProbability * rightProbability));
        }
        return Collections.min(mi);
    }

    //save TF, (left and right) neighbor number, neighbor entropy, mutual information
    private void saveTFNeighborInfoMI(String out, String stopList, String[] threshold) {
        try {
            //read stop words file
            Set<String> stopWords = new HashSet<>();
            BufferedReader br = new BufferedReader(new FileReader(stopList));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() > 1) {
                    stopWords.add(line);
                }
            }
            br.close();
            //output words TF, neighbor info, MI
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));
            for (Map.Entry<String, TFNeighbor> entry : wordTFNeighbor.entrySet()) {
                if (entry.getKey().length() <= 1 || stopWords.contains(entry.getKey())) {
                    continue;
                }
                TFNeighbor tfNeighbor = entry.getValue();


                int tf, leftNeighborNumber, rightNeighborNumber;
                double mi;
                tf = tfNeighbor.getTF();
                leftNeighborNumber = tfNeighbor.getLeftNeighborNumber();
                rightNeighborNumber = tfNeighbor.getRightNeighborNumber();
                mi = countMI(entry.getKey());
                if (tf > Integer.parseInt(threshold[0]) && leftNeighborNumber > Integer.parseInt(threshold[1]) &&
                        rightNeighborNumber > Integer.parseInt(threshold[2]) && mi > Integer.parseInt(threshold[3])) {
                    bw.write(entry.getKey() + ","
                            + tf + ","
                            + leftNeighborNumber + ","
                            + rightNeighborNumber + ","
                            + tfNeighbor.getLeftNeighborEntropy() + ","
                            + tfNeighbor.getRightNeighborEntropy() + ","
                            + mi + "\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Info: [Nagao Algorithm Step 4]: having saved to file");
    }

    //apply nagao algorithm to input file
    public static void applyNagao(String[] inputs, String out, String stopList) {
        NagaoAlgorithm nagao = new NagaoAlgorithm();
        //step 1: add phrases to PTable
        String line;
        for (String in : inputs) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(in));
                while ((line = br.readLine()) != null) {
                    nagao.add2PTable(line);
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
        System.out.println("Info: [Nagao Algorithm Step 1]: having added all left and right substrings to PTable");
        //step 2: sort PTable and count LTable
        nagao.countLTable();
        //step3: count TF and Neighbor
        nagao.countTFNeighbor();
        //step4: save TF NeighborInfo and MI
        nagao.saveTFNeighborInfoMI(out, stopList, "20,3,3,5".split(","));
    }

    public static void applyNagao(String[] inputs, String out, String stopList, int n, String filter) {
        NagaoAlgorithm nagao = new NagaoAlgorithm();
        nagao.setN(n);
        String[] threshold = filter.split(",");
        if (threshold.length != 4) {
            System.out.println("ERROR: filter must have 4 numbers, seperated with ',' ");
            return;
        }
        //step 1: add phrases to PTable
        String line;
        for (String in : inputs) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(in));
                while ((line = br.readLine()) != null) {
                    nagao.add2PTable(line);
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
        System.out.println("Info: [Nagao Algorithm Step 1]: having added all left and right substrings to PTable");
        //step 2: sort PTable and count LTable
        nagao.countLTable();
        //step3: count TF and Neighbor
        nagao.countTFNeighbor();
        //step4: save TF NeighborInfo and MI
        nagao.saveTFNeighborInfoMI(out, stopList, threshold);
    }

    private void setN(int n) {
        N = n;
    }

    public static void main(String[] args) {
        String[] ins = {"E://test//ganfen.txt"};
        applyNagao(ins, "E://test//out.txt", "E://test//stoplist.txt");
    }

}