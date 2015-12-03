import org.apache.commons.io.FileUtils;
import org.canova.api.split.FileSplit;
import org.canova.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.plot.iterationlistener.GradientPlotterIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class WhaleDBN
{

    private static Logger log = LoggerFactory.getLogger(WhaleDBN.class);

    /** No data found. */
    public static final int NDF = 200;

    /** CSV containing training data. */
    public static final File TRAIN_FILE = new File("data/train.csv");

    public static void main(String[] args) throws Exception
    {
        if (args.length == 0 || !args[0].equals("-l"))
        {
            //System.err.println(log.
            TreeSet<Integer> whaleIds = new TreeSet<>();
            HashMap<String, Integer> labelMap = new HashMap<>();
            try (Scanner in = new Scanner(TRAIN_FILE))
            {
                String line = null;
                if (in.hasNextLine())
                {
                    // Skip header.
                    in.nextLine();
                }
                else
                {
                    error("Training file empty.", NDF);
                }
                while (in.hasNextLine())
                {
                    line = in.nextLine();
                    String[] tokens = line.split(",");
                    whaleIds.add(WhaleImage.extractWhaleId(tokens[1]));
                    labelMap.put(tokens[0], WhaleImage.extractWhaleId(tokens[1]));
                }
            }

            // Customizing params
            Nd4j.MAX_SLICES_TO_PRINT = -1;
            Nd4j.MAX_ELEMENTS_PER_SLICE = -1;

            // Set path to the labeled images
            String labeledPath = "data/dbn";
            final List<String> labels = new ArrayList<>();
            File[] dir = new File(labeledPath).listFiles();

            for (File f : dir)
            {
                String name = f.getName();
                labels.add(name);
                System.err.println(name);
            }
        /*
        for (Integer id : whaleIds)
        {
            labels.add(id.toString());
        }*/


            final int numRows = 32;
            final int numColumns = 30;
            final int imgSize = numRows * numColumns;
            int outputNum = labels.size();
            int iterations = 500;
            int seed = 123;
            int batchSize = imgSize;
            int listenerFreq = 1;
            int splitTrainNum = (int) (batchSize * .8);
            final int hiddenSize = (imgSize + outputNum) / 2;
            System.err.println("###outputNum Size: " + labels.size() + " ####");

            log.info("Load data....");


            ImageRecordReader recordReader = new ImageRecordReader(numRows, numColumns, true, labels);
            System.err.println(recordReader.getLabel(labeledPath));
            recordReader.initialize(new FileSplit(new File(labeledPath)));
            DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, imgSize, -1, outputNum);

            DataSet next = iter.next();
            next.normalizeZeroMeanZeroUnitVariance();

            log.info("Split data....");
            SplitTestAndTrain testAndTrain = next.splitTestAndTrain(splitTrainNum, new Random(seed));
            DataSet train = testAndTrain.getTrain();
            DataSet test = testAndTrain.getTest();
            Nd4j.ENFORCE_NUMERICAL_STABILITY = true;


            //DataSet test = iter.next();
            //test = iter.next();
            //System.err.println(test.toString());
            //System.err.println(test.getLabels().getColumn(0).length());

            //System.exit(1);


            Nd4j.ENFORCE_NUMERICAL_STABILITY = true;

            log.info("Build model....");
            // Creating configuration for the neural net.
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(seed) // Locks in weight initialization for tuning
                    .iterations(iterations) // # training iterations predict/classify & backprop
                    .learningRate(1e-6f) // Optimization step size
                    .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT) // Backprop to calculate gradients
                    .l1(1e-1).regularization(true).l2(2e-4)
                    .useDropConnect(true)
                    .list(2) // # NN layers (doesn't count input layer)
                    .layer(0, new RBM.Builder(RBM.HiddenUnit.RECTIFIED, RBM.VisibleUnit.GAUSSIAN)
                            .nIn(imgSize) // # input nodes
                            .nOut(hiddenSize) // # fully connected hidden layer nodes. Add list if multiple layers.
                            .weightInit(WeightInit.XAVIER) // Weight initialization
                            .k(1) // # contrastive divergence iterations
                            .activation("relu") // Activation function type
                            .lossFunction(LossFunctions.LossFunction.RMSE_XENT) // Loss function type
                            .updater(Updater.ADAGRAD)
                            .dropOut(0.5)
                            .build()
                    ) // NN layer type
                    .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
                            .nIn(hiddenSize) // # input nodes
                            .nOut(outputNum) // # output nodes
                            .activation("softmax")
                            .build()
                    ) // NN layer type
                    .build();
            MultiLayerNetwork network = new MultiLayerNetwork(conf);
            network.setListeners(Arrays.<IterationListener>asList(new ScoreIterationListener(10),
                                                                  new GradientPlotterIterationListener(10)));

        /*
        // Training
        while(iter.hasNext()){
            DataSet next = iter.next();
            network.fit(next);
        }
        */

            network.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));
            log.info("Train model....");
            network.fit(train);

            log.info("Evaluate weights....");
            for (org.deeplearning4j.nn.api.Layer layer : network.getLayers())
            {
                INDArray w = layer.getParam(DefaultParamInitializer.WEIGHT_KEY);
                log.info("Weights: " + w);
            }

            log.info("Evaluate model....");
            Evaluation eval = new Evaluation(outputNum);
            INDArray output = network.output(test.getFeatureMatrix());

            for (int i = 0; i < output.rows(); i++)
            {
                String actual = test.getLabels().getRow(i).toString().trim();
                String predicted = output.getRow(i).toString().trim();
                log.info("actual " + actual + " vs predicted " + predicted);
            }

            eval.eval(test.getLabels(), output);
            log.info(eval.stats());


            OutputStream fos = Files.newOutputStream(Paths.get("coefficients.bin"));
            DataOutputStream dos = new DataOutputStream(fos);
            Nd4j.write(network.params(), dos);
            dos.flush();
            dos.close();
            FileUtils.writeStringToFile(new File("conf.json"), network.getLayerWiseConfigurations().toJson());
        }
        else
        {
            MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(
                    FileUtils.readFileToString(new File("conf.json")));
            DataInputStream dis = new DataInputStream(new FileInputStream("coefficients.bin"));
            INDArray newParams = Nd4j.read(dis);
            dis.close();
            MultiLayerNetwork savedNetwork = new MultiLayerNetwork(confFromJson);
            savedNetwork.init();
            savedNetwork.setParams(newParams);



        }
    }

    private static void error(String msg, int err)
    {
        System.err.print(msg);
        System.exit(err);
    }

}
