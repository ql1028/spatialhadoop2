package edu.umn.cs.spatialHadoop;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.spatial.CellInfo;
import org.apache.hadoop.spatial.Rectangle;
import org.apache.hadoop.spatial.SpatialSite;
import org.apache.hadoop.util.Progressable;

public class ImageOutputFormat extends FileOutputFormat<CellInfo, ImageWritable> {
  
  /**MBR of the input file*/
  private static final String InputFileMBR = "plot.file_mbr";
  
  /**Configuration line for image width*/
  private static final String ImageWidth = "plot.image_width";

  /**Configuration line for image height*/
  private static final String ImageHeight = "plot.image_height";

  /**Used to indicate the progress*/
  private Progressable progress;
  
  class ImageRecordWriter implements RecordWriter<CellInfo, ImageWritable> {

    private final FileSystem outFs;
    private final Path out;
    private final int image_width;
    private final Rectangle fileMbr;
    private final int image_height;
    
    private BufferedImage image;

    ImageRecordWriter(Path out, FileSystem outFs,
        int width, int height, Rectangle fileMbr) {
      this.out = out;
      this.outFs = outFs;
      this.image_width = width;
      this.image_height = height;
      this.fileMbr = fileMbr;
      this.image = new BufferedImage(image_width, image_height,
          BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void write(CellInfo cell, ImageWritable value) throws IOException {
      progress.progress();
      int tile_x = (int) ((cell.x - fileMbr.x) * image_width / fileMbr.width);
      int tile_y = (int) ((cell.y - fileMbr.y) * image_height / fileMbr.height);
      Graphics graphics = image.getGraphics();
      graphics.drawImage(value.getImage(), tile_x, tile_y, null);
      graphics.dispose();
    }

    @Override
    public void close(Reporter reporter) throws IOException {
      OutputStream output = outFs.create(out);
      ImageIO.write(image, "png", output);
      output.close();
    }
    
  }


  @Override
  public RecordWriter<CellInfo, ImageWritable> getRecordWriter(
      FileSystem ignored, JobConf job, String name, Progressable progress)
      throws IOException {
    this.progress = progress;
    Path file = FileOutputFormat.getTaskOutputPath(job, name);
    FileSystem fs = file.getFileSystem(job);
    Rectangle fileMbr = getFileMBR(job);
    int imageWidth = getImageWidth(job);
    int imageHeight = getImageHeight(job);
    

    return new ImageRecordWriter(file, fs, imageWidth, imageHeight, fileMbr);
  }

  public static void setImageWidth(Configuration conf, int width) {
    conf.setInt(ImageWidth, width);
  }

  public static void setImageHeight(Configuration conf, int height) {
    conf.setInt(ImageHeight, height);
  }
  
  public static void setFileMBR(Configuration conf, Rectangle mbr) {
    SpatialSite.setShape(conf, InputFileMBR, mbr);
  }
  
  public static int getImageWidth(Configuration conf) {
    return conf.getInt(ImageWidth, 1000);
  }

  public static int getImageHeight(Configuration conf) {
    return conf.getInt(ImageHeight, 1000);
  }
  
  public static Rectangle getFileMBR(Configuration conf) {
    return (Rectangle) SpatialSite.getShape(conf, InputFileMBR);
  }
}
