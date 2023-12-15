import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelStatistics {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        JSONParser parser = new JSONParser();

        try {
            // Load data from the JSON file
            JSONObject data = (JSONObject) parser.parse(new FileReader("linkvsrelu-visual.json"));

            // Extract relevant information
            String[] datatypes = {"accuracy", "sensitivity", "specificity", "f1"};
            String[] modelTypes = {"rand-32", "relu-32", "tanh-32", "linkact-32", "rand-64", "relu-64", "tanh-64", "linkact-64"};

            // Initialize a map to store the data
            List<Map<String, Object>> dataList = new ArrayList<>();

            // Populate the map with data
            for (String modelType : modelTypes) {
                for (String datatype : datatypes) {
                    List<Double> scores = (List<Double>) ((JSONObject) data.get(datatype)).get(modelType);
                    List<Double> scoresPct = new ArrayList<>();

                    // Convert scores to percentages
                    for (Double score : scores) {
                        scoresPct.add(score * 100);
                    }

                    Map<String, Object> modelData = new HashMap<>();
                    modelData.put("Model_Type", modelType);
                    modelData.put("DataType", datatype);
                    modelData.put("Scores", scoresPct);
                    dataList.add(modelData);
                }
            }

            // Display mean and standard deviation for each model type and data type
            for (String modelType : modelTypes) {
                for (String datatype : datatypes) {
                    double mean = calculateMean(dataList, modelType, datatype);
                    double std = calculateStd(dataList, modelType, datatype);

                    System.out.printf("%s %s mean (std): %.4f (%.4f)%n", modelType, datatype, mean, std);
                }
            }

            // Perform t-test for F1 scores between tanh and linkact in tiers 32 and 64
            String[] tier32 = {"tanh-32", "linkact-32"};
            String[] tier64 = {"tanh-64", "linkact-64"};

            for (String[] tier : new String[][]{tier32, tier64}) {
                System.out.print("[");
                for (String model : tier) {
                    System.out.print(model + ", ");
                }
                System.out.println("]");

                String relu = tier[0];
                String link = tier[1];

                List<Double> reluData = getF1Scores(dataList, relu);
                List<Double> linkData = getF1Scores(dataList, link);

                double[] tTestResult = performTTest(reluData, linkData);
                System.out.printf("t-statistic: %.4f, p-value: %.4f%n", tTestResult[0], tTestResult[1]);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static double calculateMean(List<Map<String, Object>> dataList, String modelType, String datatype) {
        List<Double> scores = getScores(dataList, modelType, datatype);
        double sum = 0;
        for (Double score : scores) {
            sum += score;
        }
        return sum / scores.size();
    }

    private static double calculateStd(List<Map<String, Object>> dataList, String modelType, String datatype) {
        List<Double> scores = getScores(dataList, modelType, datatype);
        double mean = calculateMean(dataList, modelType, datatype);
        double sumSquaredDiff = 0;

        for (Double score : scores) {
            sumSquaredDiff += Math.pow(score - mean, 2);
        }

        return Math.sqrt(sumSquaredDiff / scores.size());
    }

    private static List<Double> getScores(List<Map<String, Object>> dataList, String modelType, String datatype) {
        List<Double> scores = new ArrayList<>();
        for (Map<String, Object> modelData : dataList) {
            if (modelData.get("Model_Type").equals(modelType) && modelData.get("DataType").equals(datatype)) {
                scores.addAll((List<Double>) modelData.get("Scores"));
            }
        }
        return scores;
    }

    private static List<Double> getF1Scores(List<Map<String, Object>> dataList, String modelType) {
        return getScores(dataList, modelType, "f1");
    }

    private static double[] performTTest(List<Double> data1, List<Double> data2) {
        double[] result = new double[2];
        double[] array1 = data1.stream().mapToDouble(Double::doubleValue).toArray();
        double[] array2 = data2.stream().mapToDouble(Double::doubleValue).toArray();
        result[0] = TestUtils.t(array1, array2);
        result[1] = TestUtils.tTest(array1, array2);
        return result;
    }
}
