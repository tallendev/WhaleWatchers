import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

/**
 * Neural network abstraction for identifying right whales.
 *
 * @author Tyler Allen
 * @version 11/15/15
 * @created 11/15/15
 */
public class WhaleImageNeuralNetwork implements Serializable
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
    private HashMap<Integer, Perceptron> outputMap;

    public WhaleImageNeuralNetwork (int output_size, Integer[] ids)
    {
        input = new Perceptron[INPUTS];
        output = new Perceptron[output_size];
        outputMap = new HashMap<>();
        hidden = new ArrayList<>();
        hidden.add(new Perceptron[(INPUTS + output.length) / 2]);

        for (int i = 0; i < input.length; i++)
        {
            input[i] = new Perceptron();
        }

        for (int i = 0; i < output.length; i++)
        {
            output[i] = new Perceptron();
            outputMap.put(ids[i], output[i]);
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
        for (Perceptron aMHidden : mHidden)
        {
            for (Perceptron anOutput : output)
            {
                connect(aMHidden, anOutput);
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

    public void runData(java.util.List<WhaleImage> data, FileOutputStream outFile)
    {
        PrintWriter outWriter = new PrintWriter(outFile);
        StringBuilder str = new StringBuilder("Image,");
        Formatter formatter = new Formatter(str, Locale.US);
        for (int id : outputMap.keySet())
        {
            formatter.format("whale_%05d,", id);
        }
        str.deleteCharAt(str.length() - 1);
        str.append("\n");

        int counter = 0;
        for (WhaleImage anInput : data)
        {
            System.err.println("Starting data image #" + counter++);
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

            for (int i = 0; i < hidden.get(0).length; i++)
            {
                Perceptron p = hidden.get(0)[i];
                p.activate();
            }
            // Stores all output node results for backprop.
            int max = 0;
            double[] out = new double[output.length];
            for (int i = 0; i < out.length; i++)
            {
                double o_k = output[i].activate();
                out[i] = o_k;
                if (o_k > out[max])
                {
                    max = i;
                }
            }
            str.append(anInput.getFile()).append(",");
            for (int i = 0; i < out.length; i++)
            {
                int val = max == i ? 1 : 0;
                formatter.format("%d,", val);
            }
            str.deleteCharAt(str.length() - 1);
            str.append("\n");
            System.err.println("File# " + counter);
        }
        outWriter.println(str.toString());
        outWriter.close();
    }

    public void runEpoch(java.util.List<WhaleImage> training)
    {
        int counter = 0;
        for (WhaleImage anInput : training)
        {
            System.err.println("Starting training image #" + counter++);
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

            double[] hidden_out = new double[hidden.get(0).length];
            for (int i = 0; i < hidden.get(0).length; i++)
            {
                Perceptron p = hidden.get(0)[i];
                hidden_out[i] = p.activate();
            }
            // Stores all output node results for backprop.
            double[] out = new double[output.length];
            double[] outErr = new double[output.length];
            for (int i = 0; i < out.length; i++)
            {
                double o_k = output[i].activate();
                if (outputMap.get(anInput.getWhaleId()) == output[i])
                {
                    outErr[i] = o_k * (1 - o_k) * (.9 - o_k);
                }
                else
                {
                    outErr[i] = o_k * (1 - o_k) * (.1 - o_k);
                }
            }

            // backprop for hidden...
            double[] hiddenErr = new double[hidden.get(0).length];
            for (int i = 0; i < hidden.get(0).length; i++)
            {
                Perceptron p = hidden.get(0)[i];
                double o_h = hidden_out[i];
                double wSum = 0;
                for (int j = 0; j < p.connections.size(); j++)
                {
                    wSum += outErr[j] * p.connections.get(j).getWeight(p);
                }
                hiddenErr[i] = o_h * (1 - o_h) * wSum;
            }

            // Apply weight change.
            for (int i = 0; i < output.length; i++)
            {
                double temp = N * outErr[i];
                for (Perceptron p : output[i].weights.keySet())
                {
                    double cw = output[i].weights.get(p);
                    double dw = temp * output[i].in.get(p);
                    output[i].weights.put(p, cw + dw);
                }
            }

            for (int i = 0; i < hidden.get(0).length; i++)
            {
                double temp = N * hiddenErr[i];
                Perceptron current = hidden.get(0)[i];
                for (Perceptron p : current.weights.keySet())
                {
                    double cw = current.weights.get(p);
                    double dw = temp * current.in.get(p);
                    current.weights.put(p, cw + dw);
                }
            }
        }
    }

    public class Perceptron implements Serializable
    {
        protected HashMap<Perceptron, Double> weights;
        protected HashMap<Perceptron, Double> in;
        protected ArrayList<Perceptron> connections;

        // Collect weighted input
        protected double sum;

        public Perceptron ()
        {
            weights = new HashMap<>();
            in = new HashMap<>();
            connections = new ArrayList<>();
            sum = 0;
        }

        public double getWeight(Perceptron p)
        {
            return weights.get(p);
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
            in.put(p, val);
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
            for (Perceptron p : weights.keySet())
            {
                // Random weights between -.05 and .05
                weights.put(p, (Math.random() * .10) - .05);
            }
        }

        public String toString()
        {
            String str = "";
            for (Perceptron key : weights.keySet())
            {
                str += weights.get(key) + ", ";
            }
            str = str.substring(0, str.length() - 2);
            return str;
        }
    }

    private static void connect(Perceptron a, Perceptron b)
    {
        b.addWeight(a);
        a.addConnection(b);
    }

    public String toString()
    {
        String str = "Output Layer: ";
        for (Perceptron p : output)
        {
            //System.err.println("Output weights#: " + p.weights.size());
            str += "[" + p.toString() + "], ";
        }
        str = str.substring(str.length() - 2);
        str += "Hidden Layer: ";
        for (Perceptron p : hidden.get(0))
        {
            str += "[" + p.toString() + "], ";
        }
        str = str.substring(str.length() - 2);
        return str;
    }
}