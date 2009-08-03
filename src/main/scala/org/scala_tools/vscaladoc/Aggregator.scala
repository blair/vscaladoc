package org.scala_tools.vscaladoc

/**
 * Merge several vscaladoc directory into one
 */
object Aggregator {

  import java.io.File
  import scala.xml.{XML, NodeSeq}

  // val myXML = {
  //   val f = javax.xml.parsers.SAXParserFactory.newInstance()
  //   f.setNamespaceAware(false)
  //   f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
  //   XML.withSAXParser(f.newSAXParser())
  // }

  def runOn(srcList : List[File]) {
    System.setProperty("javax.xml.parsers.SAXParserFactory", classOf[MyXMLParserFactory].getName)
    println("sax factory", System.getProperty("javax.xml.parsers.SAXParserFactory"))
    val xp =  javax.xml.parsers.SAXParserFactory.newInstance()
    println("sax paser f", xp.getClass(), xp.isValidating, xp.isNamespaceAware, xp.getFeature("http://xml.org/sax/features/validation"))
    val dest = Services.cfg.outputdir

    //copy existing content
    srcList.foreach{src =>
      IOUtils.copy(src, dest)
    }

    //merge overview.html
    //TODO extract h1 and dl in one pass
    val packagesDl = srcList.foldLeft[NodeSeq](NodeSeq.Empty){ (back, current) =>
      val f = new File(current, "overview.html")
      if (f.exists) {
        val root = XML.loadFile(f)
        val subtitle = (root\"body"\"h1").text
        back ++ <h3>{subtitle}</h3> ++ (root \ "body" \ "div" \ "dl")
      } else {
        back
      }
    }
    //XML.save(new File(dest, "overview.html").getCanonicalPath, <html><body>{overviewXml}</body></html>)
    //log.info("write page for overview")
    Services.htmlRenderer.save(dest, new Page4OverviewOnXml(Services.htmlPageHelper, packagesDl))

    //TODO extract option and li in one pass
    val (packagesOption, classesLi) = srcList.foldLeft[Tuple2[NodeSeq, NodeSeq]]((NodeSeq.Empty, NodeSeq.Empty)){ (back, current) =>
      val f = new File(current, "all-classes.html")
println("process", f, f.exists)
      if (f.exists) {
        val root = XML.loadFile(new File(current, "all-classes.html"))
        (back._1 ++ (root \ "body" \\ "option"), back._2 ++ (root \ "body" \\ "li") )
      } else {
        back
      }
    }
    Services.htmlRenderer.save(dest, new Page4AllClassesOnXml(Services.htmlPageHelper, packagesOption, classesLi))

  }
}

//import javax.xml.parsers.SAXParserFactory
//import org.apache.xerces.jaxp.SAXParserFactoryImpl
import com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl
class MyXMLParserFactory extends SAXParserFactoryImpl {
  setNamespaceAware(false)
  setXIncludeAware(false)
  setValidating(false)
  setSchema(null)
  setFeature("http://xml.org/sax/features/validation", false)
  setFeature("http://xml.org/sax/features/external-general-entities", false)
  setFeature("http://xml.org/sax/features/external-parameter-entities", false)
  setFeature("http://xml.org/sax/features/namespaces", false)
  setFeature("http://apache.org/xml/features/validation/schema", false)
  setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
  setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
}

object IOUtils {
  import java.io.{File, InputStream, OutputStream, FileInputStream, FileOutputStream, Reader, Writer, BufferedOutputStream, BufferedInputStream}

  type Closable = {def close()}

  def using[A, R <: Closable](r : R)(f : R => A) : A =
    try {
      f(r)
    } finally {
      closeQuietly(r)
    }

  def closeQuietly(r :Closable) {
    try {
      r.close()
    } catch {
    case e => ()
    }
  }

  def copy(input : File, output : File) : Long = {
    if (input.equals(output)) return output.length
    if (!input.exists) {
      throw new IllegalArgumentException("file not found :" + input)
    }
    if (input.isDirectory) {
      output.mkdirs
      input.listFiles().foldLeft(0l){(back, file) => back + copy(file, new File(output, file.getName))}
    } else {
      using (new FileInputStream(input)) { is =>
        using (new FileOutputStream(output)) { os =>
          copy(is, os)
        }
      }
    }
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   * @since Commons IO 1.3
   * @basedon commons-io (IOUtils)
   */
  def copy(input : InputStream, output : OutputStream) : Long = {
    val buffer = new Array[Byte](1024 * 4)
    var count = 0L
    var n = input.read(buffer)
    while (n != -1) {
      output.write(buffer, 0, n)
      count += n
      n = input.read(buffer)
    }
    count
  }

  def copy(input : Reader, output : Writer) : Long = {
    val buffer = new Array[Char](1024 * 4)
    var count = 0L
    var n = input.read(buffer)
    while (n != -1) {
      output.write(buffer, 0, n)
      count += n
      n = input.read(buffer)
    }
    count
  }
}
