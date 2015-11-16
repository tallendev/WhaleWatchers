import java.io.File;
import java.util.Scanner;

/**
 * Representation of an image file.
 *
 * @author Tyler Allen
 * @version 9/13/15
 * @created 9/13/15
 */

public class WhaleImage
{
    public static int NO_ID = -1;

    private File imgFile;
    private int whaleId;

    /* CONSTRUCTORS */

    protected WhaleImage(String path)
    {
        this(path, null);
    }

    protected WhaleImage(File path)
    {
        this(path, null);
    }

    protected WhaleImage(String path, String whale)
    {
        this(new File(path), whale);
    }

    protected WhaleImage(File imgFile, String whale)
    {
        this.imgFile = imgFile;
        if (whale != null)
        {
            this.whaleId = extractWhaleId(whale);
        }
        else
        {
            this.whaleId = NO_ID;
        }
    }

    /* METHODS*/

    /**
     *
     * @param whaleId
     */
    protected void setWhaleId(String whaleId)
    {
        this.whaleId = extractWhaleId(whaleId);
    }

    protected void setWhaleId(int whaleId)
    {
        whaleId = whaleId;
    }

    /**
     * Parses whale ID from string.
     */
    protected int extractWhaleId(String whale)
    {
        Scanner in = new Scanner(whale).useDelimiter("[^0-9]+");
        return in.nextInt();
    }

    public int getWhaleId()
    {
        return whaleId;
    }

    public File getFile()
    {
        return imgFile;
    }
}
