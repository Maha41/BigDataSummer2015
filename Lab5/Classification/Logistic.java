

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.mahout.classifier.sgd.CsvRecordFactory;
import org.apache.mahout.classifier.sgd.LogisticModelParameters;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.classifier.sgd.RecordFactory;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;

import com.google.common.base.Charsets;

/*
This code is referred from book - Learning Apache Mahout 2nd edition by T Chandramani.

In this code we will be trying to run Logistic Regression on Mahout.

 */
public class Logistic {
    private static double predictorWeight(OnlineLogisticRegression lr, int row, RecordFactory csv, String predictor) {
        double weight = 0;
        for (Integer column : csv.getTraceDictionary().get(predictor)) {
            weight += lr.getBeta().get(row, column);
        }
        return weight;
    }
    public static void main(String[] args) throws IOException
    {
        //Setup IO path
        String inputFile = "input_bank_data.csv";
        String outputFile = "model";
        //Define the column name and types.
        List<String> predictorList =Arrays.asList("age","job","marital","education","default",
                "housing","loan","contact","month","day_of_week","duration","campaign","pdays","previous","poutcome",
                "emp.var.rate","cons.price.idx","cons.conf.idx","euribor3m","nr.employed");
        List<String> typeList = Arrays.asList("n", "w", "w", "w", "w", "w", "w", "w", "w", "w", "n", "n", "n", "n",
                "w", "n", "n", "n", "n", "n");
        // Setup Parameter for helper class
        LogisticModelParameters lmp = new LogisticModelParameters();
        lmp.setTargetVariable("y");
        lmp.setMaxTargetCategories(2);
        lmp.setNumFeatures(20);
        lmp.setUseBias(false);
        lmp.setTypeMap(predictorList,typeList);
        lmp.setLearningRate(0.5);

        //Get regression and RecordFactory instance
        int passes = 50;
        OnlineLogisticRegression lr;
        CsvRecordFactory csv = lmp.getCsvRecordFactory();
        lr = lmp.createRegression();

        int k = 0;
        //Iterate number of times as specified in pass.
        for (int pass = 0; pass < passes; pass++) {
            //Read the file and setup the first line in RecordFactory
            BufferedReader in = new BufferedReader(new FileReader(inputFile));
            String line1 = in.readLine();
            System.out.print(line1);
            csv.firstLine(line1);
            //Read the lines and convert them into vector
            String line = in.readLine();
            int lineCount = 2;
            while (line != null) {

                Vector input = new RandomAccessSparseVector(lmp.getNumFeatures());
                int targetValue = csv.processLine(line, input);

                // update model
                lr.train(targetValue, input);
                k++;

                line = in.readLine();
                lineCount++;
            }
            in.close();
        }
        //Save the model
        OutputStream modelOutput = new FileOutputStream(outputFile);
        try {
            lmp.saveTo(modelOutput);
        } finally {
            modelOutput.close();
        }
        PrintWriter output=new PrintWriter(new OutputStreamWriter(System.out, Charsets.UTF_8), true);
        output.println(lmp.getNumFeatures());
        output.println(lmp.getTargetVariable() + " ~ ");
        String sep = "";
        for (String v : csv.getTraceDictionary().keySet()) {
            double weight = predictorWeight(lr, 0, csv, v);
            if (weight != 0) {
                output.printf(Locale.ENGLISH, "%s%.3f*%s", sep, weight, v);
                sep = " + ";
            }
        }
        output.printf("%n");
        for (int row = 0; row < lr.getBeta().numRows(); row++) {
            for (String key : csv.getTraceDictionary().keySet()) {
                double weight = predictorWeight(lr, row, csv, key);
                if (weight != 0) {
                    output.printf(Locale.ENGLISH, "%20s %.5f%n", key, weight);
                }
            }
            for (int column = 0; column < lr.getBeta().numCols(); column++) {
                output.printf(Locale.ENGLISH, "%15.9f ", lr.getBeta().get(row, column));
            }
            output.println();
        }
    }
}
