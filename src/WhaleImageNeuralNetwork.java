import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Neural network abstraction for identifying right whales.
 *
 * @author Tyler Allen
 * @version 11/15/15
 * @created 11/15/15
 */
public class WhaleImageNeuralNetwork
{
    public static final int WIDTH = 32;
    public static final int HEIGHT = 30;
    /**
     * Currently the resolution of photos, to change maybe.
     */
    public static final int INPUTS = WIDTH * HEIGHT;
    /**
     * Learning rate.
     */
    public static final double N = .01;

    private Perceptron[] input;
    private Perceptron[] output;
    private ArrayList<Perceptron[]> hidden;

    public WhaleImageNeuralNetwork (int output_size)
    {
        input = new Perceptron[INPUTS];
        output = new Perceptron[output_size];
        hidden = new ArrayList<>();
        hidden.add(new Perceptron[(INPUTS + output.length) / 2]);

        for (int i = 0; i < input.length; i++)
        {
            input[i] = new Perceptron();
        }

        for (int i = 0; i < output.length; i++)
        {
            output[i] = new Perceptron();
        }
        // Assuming fixed hidden length for now...
        Perceptron[] mHidden = hidden.get(0);
        for (int i = 0; i < mHidden.length; i++)
        {
            mHidden[i] = new Perceptron();
        }
        // connections
        for (Perceptron anInput1 : input)
        {
            for (Perceptron aMHidden : mHidden)
            {
                connect(anInput1, aMHidden);
            }
        }
        for (int i = 0; i < mHidden.length; i++)
        {
            for (int j = 0; j < output.length; j++)
            {
                connect(input[i], mHidden[j]);
            }
        }
        /**
        for (Perceptron anInput : input)
        {
            anInput.avgWeights();
        }
         **/
        for (Perceptron anOutput: output)
        {
            anOutput.randWeights();
        }
        for (Perceptron anHidden: mHidden)
        {
            anHidden.randWeights();
        }
    }

    public void runEpoch(java.util.List<WhaleImage> training)
    {
        for (WhaleImage anInput : training)
        {
            BufferedImage image;
            int[] colorBuffer;
            try
            {
                // get the BufferedImage, using the ImageIO class
                image = ImageIO.read(anInput.getFile());
            }
            catch (IOException e)
            {
                System.err.println(e.getMessage());
                continue;
            }
            colorBuffer = image.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
            Color color;
            for (int i = 0; i < INPUTS; i++)
            {
                color = new Color(colorBuffer[i]);
                int gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                this.input[i].receiveInput(gray);
            }
            for (Perceptron p : input)
            {
                p.activate();
            }
            for (Perceptron p : hidden.get(0))
            {
                p.activate();
            }
            // Stores all output node results. Not necessary for decision, but good for stats.
            double[] out = new double[output.length];
            for (int i = 0; i < out.length; i++)
            {
                out[i] = output[i].activate();
            }
            
        }
    }

    public class Perceptron
    {
        protected HashMap<Perceptron, Double> weights;
        protected ArrayList<Perceptron> connections;

        // Collect weighted input
        protected double sum;

        public Perceptron ()
        {
            weights = new HashMap<>();
            connections = new ArrayList<>();
            sum = 0;
        }

        public void addConnection (Perceptron p)
        {
            connections.add(p);
        }

        protected void addWeight (Perceptron b)
        {
            weights.put(b, 0.0);
        }

        public void receiveInput(double val)
        {
            sum = val;
        }

        /**
         * Receive a new input value from an input node.
         * @param p the node sending the input.
         * @param val the value to be received.
         */
        private void receive (Perceptron p, double val)
        {
            sum += val * weights.get(p);
        }

        /**
         * Pass value forward to all nodes.
         * @param val the value to pass forward
         */
        public void feedforward(double val)
        {
            for (Perceptron p : connections)
            {
                p.receive(this, val);
            }
        }

        public double activate()
        {
            // Currently implemented as sigmoid function
            double val = 1 / (1 + Math.exp(-sum));
            feedforward(val);

            sum = 0;
            return val;
        }

        protected void avgWeights ()
        {
            double avg = 1.0 / weights.size();
            for (Perceptron p : weights.keySet())
            {
                weights.put(p, avg);
            }
        }

        protected void randWeights ()
        {
            double sum = 0;
            double rand;
            // random weight
            for (Perceptron p : weights.keySet())
            {
                rand = Math.random();
                sum += rand;
                weights.put(p, rand);
            }
            double avg = sum / weights.size();
            // normalize weights
            for (Perceptron p : weights.keySet())
            {
                weights.put(p, weights.get(p) / avg);
            }
        }
    }

    private static void connect(Perceptron a, Perceptron b)
    {
        b.addWeight(a);
        a.addConnection(b);
    }
}