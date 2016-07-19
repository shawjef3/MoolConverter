package com.rocketfuel.build

class GroupByKeysOp[K, V](val values: Traversable[(K, V)]) extends AnyVal {
  /**
    * Perform groupBy, and then get rid of the keys in the values.
    * @return
    */
  def groupByKeys: Map[K, Set[V]] = {
    for {
      (k, v) <- values.groupBy(_._1)
    } yield k -> v.map(_._2).toSet
  }
}

trait GroupByKeys {

  implicit def toGroupByKeys[K, V](values: Traversable[(K, V)]): GroupByKeysOp[K, V] =
    new GroupByKeysOp(values)

}

object GroupByKeys extends GroupByKeys
