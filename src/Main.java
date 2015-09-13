import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Main operating file for WhaleWatchers project.
 *
 * @author Tyler Allen
 * @version 9/13/15
 * @created 9/13/15
 */


public class Main
{
    /** File not found exception. */
    public static final int FNF = 100;
    /** No data found. */
    public static final int NDF = 200;

    /** Directory of image files.*/
    public static final File DATA_DIR = new File("data/imgs");
    /** CSV containing training data. */
    public static final File TRAIN_FILE = new File("data/train.csv");

    private static ArrayList<Image> trainData = new ArrayList<>();
    private static ArrayList<Image> testData = new ArrayList<>();

    public static void main(String[] args)
    {
        File[] images = DATA_DIR.listFiles();
        if (images.length == 0)
        {
            error("Image directory empty.", NDF);
        }

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
                Image img = new Image(new File(DATA_DIR, tokens[0]), tokens[1]);
                testData.add(img);
            }
            if (testData.size() == 0)
            {
                error("No training data in file.", NDF);
            }
        }
        catch (FileNotFoundException fnf)
        {
            error("Training file not found.", FNF);
        }
        // Check all image files
        for (File file : images)
        {
            boolean isTrain = false;
            // See if image file is in test data
            for (Image test : testData)
            {
                if (file.getName().equals(test.getFile().getName()))
                {
                    isTrain = true;
                    break;
                }
            }
            // if it's not training data, add it to the test list.
            if (!isTrain)
            {
                trainData.add(new Image(file));
            }
        }

        log();
    }

    private static void error(String msg, int err)
    {
        System.err.print(msg);
        System.exit(err);
    }

    private static void log()
    {
        System.err.println("Training data:");
        for (Image img : trainData)
        {
            System.err.println("File: " + img.getFile().getAbsolutePath());
            System.err.println("Whale: " + img.getWhaleId());
        }
        System.err.println("Test Data: ");
        for (Image img : testData)
        {
            System.err.println("File: " + img.getFile().getAbsolutePath());
            System.err.println("Whale: " + img.getWhaleId());
        }
    }
}
