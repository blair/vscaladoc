package org.scala_tools.vscaladoc

import scala.tools.nsc._

import java.util.zip.ZipFile
import java.io.File
import java.net.{URI, URL}

import scala.collection.jcl
import symtab.Flags._
import scala.xml._
import scala.io.Source

/**
 * <b>fake</b> comments (for test)
 *
 * @author David Bernard
 * @version 0.1
 */
abstract class DocDriver extends ModelExtractor {
  /** comments for settings, settings is set/injected by Main (override)*/
  def settings: VSettings
  /** comments for outdir */
  val outdir      = settings.outdir.value
  val sourcedir   = settings.sourcepath.value

  def init() {
    Services.cfg.pageFooter =  DocUtil.load(settings.pagebottom.value)
    Services.cfg.pageHeader =  DocUtil.load(settings.pagetop.value)
    Services.cfg.windowTitle = settings.windowtitle.value
    Services.cfg.overviewTitle = DocUtil.load(settings.doctitle.value) //load
    Services.cfg.sourcedir = new File(settings.sourcepath.value)
    Services.cfg.outputdir = new File(settings.outdir.value)
    Services.cfg.format = if (settings.formatMarkdown.value) {
      MarkdownFormat
    } else if (settings.formatTextile.value) {
      TextileFormat
    } else {
      HtmlFormat
    }
    Services.cfg.global = global

    val dir = new File(outdir)
    //System.out.println("delete :" + dir)
    Services.fileHelper.deleteDirectory(dir)
    dir.mkdirs()
  }

  //import global._
//  object additions extends jcl.LinkedHashSet[Symbol]
//  object additions0 extends ModelAdditions(global) {
//    override def addition(sym: global.Symbol) = {
//      super.addition(sym)
//      sym match {
//        case sym : global.ClassSymbol  => additions += sym.asInstanceOf[Symbol]
//        case sym : global.ModuleSymbol => additions += sym.asInstanceOf[Symbol]
//        case sym : global.TypeSymbol   => additions += sym.asInstanceOf[Symbol]
//        case _ =>
//      }
//    }
//    def init {}
//  }

  /**
   * fake comments for the method process
   * @param units comments for units parameter
   * @throws no-exception
   */
  def process(units: Iterator[global.CompilationUnit]) {
    assert(global.definitions != null)
    var allPackages : Set[Package] = Set.empty
    var allClasses : List[ClassOrObject] = Nil

    def g(pkg: Package, clazz: ClassOrObject) {
      if (isAccessible(clazz.sym)) {
        allClasses =  clazz :: allClasses
        allPackages = allPackages + pkg
        clazz.decls.map(_._2).foreach {
          //case clazz : ClassOrObject => g(pkg, clazz)
          case clazz : Clazz => g(pkg, clazz)
          case _ =>
        }
      }
    }

    def f(pkg: Package, tree: global.Tree) {
      if (tree != global.EmptyTree && tree.hasSymbol) {
        val sym = tree.symbol
        if (sym != global.NoSymbol && !sym.hasFlag(symtab.Flags.PRIVATE)) tree match {
          case tree : global.PackageDef =>
            val pkg1 = new Package(sym.asInstanceOf[global.ModuleSymbol])
            tree.stats.foreach(stat => f(pkg1, stat))
          case tree : global.ClassDef =>
            assert(pkg != null)
            g(pkg, new TopLevelClass(sym.asInstanceOf[global.ClassSymbol]))
          case tree : global.ModuleDef =>
            assert(pkg != null)
            g(pkg, new TopLevelObject(sym.asInstanceOf[global.ModuleSymbol]))
          case _ =>
        }
      }
    }
    try {
      loadPackageLinkDefs()

      //println("extract model")
      units.foreach(unit => f(null, unit.body))
      //println("render start")
      //println("nb of Classes : " + allClasses.size)
      //println("nb of Packages: " + allPackages.size)

      allPackages.foreach(pkg => Services.linkHelper.addSitePackage(pkg.fullName('.')))
      Services.modelHelper.updateSubClasses(allClasses)
      Services.htmlRenderer.render(allPackages, allClasses)
      //println("render end")
    } catch {
      case e => e.printStackTrace()
    }
  }

  def loadPackageLinkDefs() {
    def loadFromURL(url: URL) {
      try {
        Source.fromURL(url).getLines.foreach{ l =>
          val a = l.split("=")
          Services.linkHelper.addRemotePackage(a(0), new URI(a.last.trim))
        }
      } catch {
        case e : Exception => System.err.println("failed to load PackageLinkDefs(" + url +") :" + e.getMessage)
      }
    }
    loadFromURL(this.getClass.getResource("/org/scala_tools/vscaladoc/remotePkg.properties"))
    val arg = System.getProperty("packageLinkDefs")
    if (arg != null) {
      val url = new URL(arg)
      System.out.println("load packageLinkDefs from :" + url)
      loadFromURL(url)
    }
    //println("resource = " + in)
  }
}
