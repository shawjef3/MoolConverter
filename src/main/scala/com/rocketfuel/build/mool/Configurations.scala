package com.rocketfuel.build.mool

import java.nio.file.Path
import scalaz.Scalaz._
import scalaz._

case class Configurations[A](
  main: Set[A] = Set.empty[A],
  test: Set[A] = Set.empty[A]
) {
  def withMains(mains: A*): Configurations[A] =
    Configurations(
      main = main ++ mains,
      test = test -- mains
    )

  def withTests(tests: A*): Configurations[A] =
    copy(test = test ++ (tests.toSet -- main))

  def union(other: Configurations[A]): Configurations[A] =
    withMains(other.main.toSeq: _*).withTests(other.test.toSeq: _*)

  def ++(other: Configurations[A]): Configurations[A] = union(other)

  def isEmpty: Boolean = main.isEmpty && test.isEmpty
}

object Configurations {
  implicit def show[A](implicit inner: Show[A]): Show[Configurations[A]] =
    new Show[Configurations[A]] {
      override def show(f: Configurations[A]): Cord = {
        ("Configurations(Set(" +: Cord(f.main.toVector.map(_.show).intercalate(Cord.stringToCord(",")))) ++
          ("," +: Cord(f.test.toVector.map(_.show).intercalate(Cord.stringToCord(","))) :+ "))")
      }
    }

  implicit val showPath: Show[Path] =
    Show.showFromToString[Path]
}
