/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gluten.planner.cost

import org.apache.gluten.planner.plan.GlutenPlanModel.GroupLeafExec
import org.apache.gluten.ras.{Cost, CostModel}

import org.apache.spark.sql.execution.SparkPlan

abstract class LongCostModel extends CostModel[SparkPlan] {
  private val infLongCost = Long.MaxValue

  override def costOf(node: SparkPlan): GlutenCost = node match {
    case _: GroupLeafExec => throw new IllegalStateException()
    case _ => GlutenCost(longCostOf(node))
  }

  private def longCostOf(node: SparkPlan): Long = node match {
    case n =>
      val selfCost = selfLongCostOf(n)

      // Sum with ceil to avoid overflow.
      def safeSum(a: Long, b: Long): Long = {
        assert(a >= 0)
        assert(b >= 0)
        val sum = a + b
        if (sum < a || sum < b) Long.MaxValue else sum
      }

      (n.children.map(longCostOf).toList :+ selfCost).reduce(safeSum)
  }

  def selfLongCostOf(node: SparkPlan): Long

  override def costComparator(): Ordering[Cost] = Ordering.Long.on {
    case GlutenCost(value) => value
    case _ => throw new IllegalStateException("Unexpected cost type")
  }

  override def makeInfCost(): Cost = GlutenCost(infLongCost)
}
