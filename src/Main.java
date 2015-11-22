import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeSet;

/**
 * Main operating file for WhaleWatchers project.
 *
 * @author Tyler Allen
 * @version 9/13/15
 * @created 9/13/15
 */


public class Main
{
    public static final int TRAINING_ITERATIONS = 1;

    /** File not found exception. */
    public static final int FNF = 100;
    /** No data found. */
    public static final int NDF = 200;

    /** Directory of image files. */
    public static final File DATA_DIR = new File("data/imgs");
    /** Directory of training data. */
    public static final File TRAIN_DIR = new File("data/train");
    /** CSV containing training data. */
    public static final File TRAIN_FILE = new File("data/train.csv");

    private static ArrayList<WhaleImage> trainData = new ArrayList<>();
    private static ArrayList<WhaleImage> testData = new ArrayList<>();

    public static void main(String[] args)
    {
        System.err.println("Data dir: " + DATA_DIR);
        File[] images = DATA_DIR.listFiles(new FilenameFilter() {
            @Override
            public boolean accept (File dir, String name)
            {
                return name.toLowerCase().endsWith(".jpg");
            }
        });
        if (images.length == 0)
        {
            error("Image directory empty.", NDF);
        }
        // Check all image files
        for (File file : images)
        {
            testData.add(new WhaleImage(file));
        }

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
                // tokens[0] = path, tokens[1] = whale id
                WhaleImage img = new WhaleImage(new File(TRAIN_DIR, tokens[0]), tokens[1]);
                trainData.add(img);
                whaleIds.add(img.getWhaleId());
            }
            if (trainData.size() == 0)
            {
                error("No training data in file.", NDF);
            }
        }
        catch (FileNotFoundException fnf)
        {
            error("Training file not found.", FNF);
        }

        //log();

        WhaleImageNeuralNetwork ann = new WhaleImageNeuralNetwork(whaleIds.size(),
                                                                  whaleIds.toArray(new Integer[whaleIds.size()]));
        for (int i = 0; i < TRAINING_ITERATIONS; i++)
        {
            ann.runEpoch(testData);
            System.err.println("Epoch " + i + " complete.");
        }
        System.err.println("Done");
    }

    private static void error(String msg, int err)
    {
        System.err.print(msg);
        System.exit(err);
    }

    private static void log()
    {
        System.err.println("Training data:");
        for (WhaleImage img : trainData)
        {
            System.err.println("File: " + img.getFile().getAbsolutePath());
            System.err.println("Whale: " + img.getWhaleId());
        }
        System.err.println("\nTest Data: ");
        for (WhaleImage img : testData)
        {
            System.err.println("File: " + img.getFile().getAbsolutePath());
            System.err.println("Whale: " + img.getWhaleId());
        }
    }
}
