package com.rocketfuel.build.jvmlib

import com.google.common.jimfs.{Configuration, Jimfs}
import com.rocketfuel.build.mool
import java.nio.file._
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import scalaz.Tree
import sext._

class ModelSpec
  extends FunSuite
  with BeforeAndAfterEach {

  var fs: FileSystem = null

  var root: Path = null

  test("copies") {
    val moolModel = mool.Model.ofRepository(root)
    val actualModels = Models.ofMoolRepository(root)

    val expectedModels =
      Models(
        models =
          Map(
            Vector("java", "RELCFG0") -> Model(
              identifier = Some(Model.Identifier("test_group", "test_artifact0", "0.0")),
              configurations = Map(
                "main" -> Model.Configuration(dependencies = Set(), files = Set(root.resolve("java/00.java"), root.resolve("java/01.java"), root.resolve("java/10.java"), root.resolve("java/11.java"))),
                "test" -> Model.Configuration(dependencies = Set(mool.Dependency.Maven("commons-collections", "commons-collections", "3.2.1")), files = Set(root.resolve("java/00Test.java"), root.resolve("java/01Test.java"), root.resolve("java/10Test.java"), root.resolve("java/11Test.java")))
              )
            ),
            //This second RELCFG is use to test that source files don't get included in downstream projects.
            Vector("java", "RELCFG1") -> Model(
              identifier = Some(Model.Identifier("test_group", "test_artifact0", "0.0")),
              configurations = Map(
                "main" -> Model.Configuration(dependencies = Set(mool.Dependency.RelCfg(Vector("java", "RELCFG0"))), files = Set(root.resolve("java/20.java")))
              )
            )
          ),
        moolModel = moolModel,
        moolRoot = root
      )

    assertResult(expectedModels.models(Vector("java", "RELCFG0")).configurations("main"), "main configurations differ")(actualModels.models(Vector("java", "RELCFG0")).configurations("main"))
    assertResult(expectedModels.models(Vector("java", "RELCFG0")).configurations("test"), "test configurations differ")(actualModels.models(Vector("java", "RELCFG0")).configurations("test"))

    //Test some properties of RELCFG dependencies.
    assertResult(Set(mool.Dependency.RelCfg(Vector("java", "RELCFG0"))), "Model dependencies don't reflect their RELCFG's BLD dependencies.")(actualModels.models(Vector("java", "RELCFG1")).configurations("main").dependencies)
    assertResult(Set(root.resolve("java/20.java")), "Model source files aren't blocked by another RELCFG.")(actualModels.models(Vector("java", "RELCFG1")).configurations("main").files)

    assertResult(expectedModels.models(Vector("java", "RELCFG1")).configurations("main"), "main configurations differ")(actualModels.models(Vector("java", "RELCFG1")).configurations("main"))

    assertResult(expectedModels.moolModel, "mool models differ")(actualModels.moolModel)
    assertResult(expectedModels.moolRoot, "mool roots differ")(actualModels.moolRoot)

    assertResult(expectedModels)(actualModels)

    val copiesRoot = root.resolve("copies")

    val expectedCopies =
      Map(
        root.resolve("java/00.java") -> copiesRoot.resolve("0RELCFG/src/main/java/00.java"),
        root.resolve("java/00Test.java") -> copiesRoot.resolve("0RELCFG/src/test/java/00Test.java"),
        root.resolve("java/01.java") -> copiesRoot.resolve("0RELCFG/src/main/java/01.java"),
        root.resolve("java/01Test.java") -> copiesRoot.resolve("0RELCFG/src/test/java/01Test.java"),
        root.resolve("java/10.java") -> copiesRoot.resolve("0RELCFG/src/main/java/10.java"),
        root.resolve("java/10Test.java") -> copiesRoot.resolve("0RELCFG/src/test/java/10Test.java"),
        root.resolve("java/11.java") -> copiesRoot.resolve("0RELCFG/src/main/java/11.java"),
        root.resolve("java/11Test.java") -> copiesRoot.resolve("0RELCFG/src/test/java/11Test.java")
      )

    val actualCopies =
      actualModels.copies(copiesRoot)

    for ((expectedCopy, actualCopy) <- expectedCopies.zip(actualCopies)) {
      assertResult(expectedCopy)(actualCopy)
    }
  }

  test("DependencyTree") {
    import com.rocketfuel.build.mool.{Dependency, DependencyTree}
    val moolModel = mool.Model.ofRepository(root)

    val actual = DependencyTree.ofRelCfgs(moolModel)

    val expected = Stream[scalaz.Tree[Dependency]](
      Tree.node(Dependency.RelCfg(Vector("java", "RELCFG0")), Stream(Tree.node(Dependency.Bld(Vector("java", "0")), Stream(Tree.leaf(Dependency.Bld(Vector("java", "1"))))))),
      Tree.node(Dependency.RelCfg(Vector("java", "RELCFG1")), Stream(Tree.node(Dependency.Bld(Vector("java", "2")), Stream(Tree.leaf(Dependency.Bld(Vector("java", "1")))))))
    )

    val expectedString =
      expected.map(_.drawTree).toVector.mkString("\n")

    val actualString =
      actual.map(_.map(_._2).drawTree).toVector.mkString("\n")

    assertResult(expectedString)(actualString)
  }

  override protected def beforeEach(): Unit = {
    fs = Jimfs.newFileSystem(Configuration.unix())
    root = fs.getPath("/")
    Files.createDirectories(root.resolve("java"))

    for (file <- Vector("RELCFG", "BLD")) {
      val in = getClass.getResourceAsStream(file)
      val out = Files.newOutputStream(root.resolve("java").resolve(file))
      IOUtils.copy(in, out)
      out.close()
      in.close()
    }
  }

  override protected def afterEach(): Unit = {
    fs.close()
  }

}
