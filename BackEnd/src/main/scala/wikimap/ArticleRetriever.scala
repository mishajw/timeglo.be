package wikimap

import java.io._
import java.nio.channels.Channels

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
  * Created by misha on 24/12/15.
  */
class ArticleRetriever {
  def getRange(start: Int, end: Int): Unit = {
    val file = new RandomAccessFile(new File("res/enwiki-20151201-pages-articles.xml.bz2"), "r")
    file.seek(start)
    val cis = new CompressorStreamFactory().createCompressorInputStream(Channels.newInputStream(file.getChannel))
    val cis2 = new BZip2CompressorInputStream(cis)

    var bytes: Array[Byte] = Array()

    var curLine = 0

    var i = 0

    while ((curLine = cis2.read()) != null && i < 100) {
      println(curLine)
      i += 1
    }
  }

//  public static BufferedReader getBufferedReaderForCompressedFile(String fileIn) throws FileNotFoundException, CompressorException {
//    FileInputStream fin = new FileInputStream(fileIn);
//    BufferedInputStream bis = new BufferedInputStream(fin);
//    CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
//    BufferedReader br2 = new BufferedReader(new InputStreamReader(input));
//    return br2;
//  }
}
