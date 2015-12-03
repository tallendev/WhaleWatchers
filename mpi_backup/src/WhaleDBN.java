import org.apache.commons.io.FileUtils;
import org.canova.api.records.reader.RecordReader;
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
        TreeSet<Integer> whaleIds = new TreeSet<>();
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
            }
        }

        // Customizing params
        Nd4j.MAX_SLICES_TO_PRINT = -1;
        Nd4j.MAX_ELEMENTS_PER_SLICE = -1;

        final int numRows = 4;
        final int numColumns = 1;
        int outputNum = 2649;
        int batchSize = 150;
        int iterations = 5;
        int splitTrainNum = (int) (batchSize * .8);
        int seed = 123;
        int listenerFreq = 1;

        // Set path to the labeled images
        String labeledPath = "data/train/";
        List<String> labels = new ArrayList<>();
        File[] dir = new File(labeledPath).listFiles();
        for (File f : dir)
        {
            labels.add(f.getName());
        }

        log.info("Load data....");
        RecordReader recordReader = new ImageRecordReader(32, 30, true, labels);
        recordReader.initialize(new FileSplit(new File(labeledPath)));
        DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, 960, labels.size());

        DataSet next = iter.next();
        next.normalizeZeroMeanZeroUnitVariance();

        log.info("Split data....");
        SplitTestAndTrain testAndTrain = next.splitTestAndTrain(splitTrainNum, new Random(seed));
        DataSet train = testAndTrain.getTrain();
        DataSet test = testAndTrain.getTest();
        Nd4j.ENFORCE_NUMERICAL_STABILITY = true;

        log.info("Build model....");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed) // Locks in weight initialization for tuning
            .iterations(iterations) // # training iterations predict/classify & backprop
            .learningRate(1e-6f) // Optimization step size
            .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT) // Backprop to calculate gradients
            .l1(1e-1).regularization(true).l2(2e-4)
            .useDropConnect(true)
            .list(2) // # NN layers (doesn't count input layer)
          .layer(0, new RBM.Builder(RBM.HiddenUnit.RECTIFIED, RBM.VisibleUnit.GAUSSIAN)
            .nIn(numRows * numColumns) // # input nodes
            .nOut(3) // # fully connected hidden layer nodes. Add list if multiple layers.
            .weightInit(WeightInit.XAVIER) // Weight initialization
            .k(1) // # contrastive divergence iterations
            .activation("relu") // Activation function type
            .lossFunction(LossFunctions.LossFunction.RMSE_XENT) // Loss function type
            .updater(Updater.ADAGRAD)
            .dropOut(0.5)
            .build()
          ) // NN layer type
          .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MCXENT)
            .nIn(3) // # input nodes
            .nOut(outputNum) // # output nodes
            .activation("softmax")
            .build()
        ) // NN layer type
        .build();
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
//        model.setListeners(Arrays.asList(new ScoreIterationListener(listenerFreq),
//                new GradientPlotterIterationListener(listenerFreq),
//                new LossPlotterIterationListener(listenerFreq)));


        model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));
        log.info("Train model....");
        model.fit(train);

        log.info("Evaluate weights....");
        for(org.deeplearning4j.nn.api.Layer layer : model.getLayers()) {
            INDArray w = layer.getParam(DefaultParamInitializer.WEIGHT_KEY);
            log.info("Weights: " + w);
        }

        log.info("Evaluate model....");
        Evaluation eval = new Evaluation(outputNum);
        INDArray output = model.output(test.getFeatureMatrix());

        for (int i = 0; i < output.rows(); i++) {
            String actual = test.getLabels().getRow(i).toString().trim();
            String predicted = output.getRow(i).toString().trim();
            log.info("actual " + actual + " vs predicted " + predicted);
        }

        eval.eval(test.getLabels(), output);
        log.info(eval.stats());
        log.info("****************Example finished********************");


        OutputStream fos = Files.newOutputStream(Paths.get("coefficients.bin"));
        DataOutputStream dos = new DataOutputStream(fos);
        Nd4j.write(model.params(), dos);
        dos.flush();
        dos.close();
        FileUtils.writeStringToFile(new File("conf.json"), model.getLayerWiseConfigurations().toJson());

        MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(FileUtils.readFileToString(new File("conf.json")));
        DataInputStream dis = new DataInputStream(new FileInputStream("coefficients.bin"));
        INDArray newParams = Nd4j.read(dis);
        dis.close();
        MultiLayerNetwork savedNetwork = new MultiLayerNetwork(confFromJson);
        savedNetwork.init();
        savedNetwork.setParams(newParams);
        System.out.println("Original network params " + model.params());
        System.out.println(savedNetwork.params());

    }

    private static void error(String msg, int err)
    {
        System.err.print(msg);
        System.exit(err);
    }

}