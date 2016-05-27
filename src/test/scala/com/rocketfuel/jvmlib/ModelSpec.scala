package com.rocketfuel.jvmlib

import com.google.common.jimfs.{Configuration, Jimfs}
import com.rocketfuel.mool
import java.nio.file._
import org.apache.commons.io.IOUtils
import org.scalatest.{BeforeAndAfterEach, FunSuite}
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
            Vector("java", "0RELCFG") -> Model(
              identifier = Some(Model.Identifier("test_group", "test_artifact", "0.0")),
              configurations = Map(
                "main" -> Model.Configuration(dependencies = Set(), files = Set(root.resolve("java/00.java"), root.resolve("java/01.java"), root.resolve("java/10.java"), root.resolve("java/11.java"))),
                "test" -> Model.Configuration(dependencies = Set(), files = Set(root.resolve("java/00Test.java"), root.resolve("java/01Test.java"), root.resolve("java/10Test.java"), root.resolve("java/11Test.java")))
              )
            )
          ),
        moolModel = moolModel,
        moolRoot = root
      )

    assertResult(expectedModels.models(Vector("java", "0RELCFG")).configurations("main"), "main configurations differ")(actualModels.models(Vector("java", "0RELCFG")).configurations("main"))
    assertResult(expectedModels.models(Vector("java", "0RELCFG")).configurations("test"), "test configurations differ")(actualModels.models(Vector("java", "0RELCFG")).configurations("test"))
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
