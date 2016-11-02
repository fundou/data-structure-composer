package tests

/**
  * Created by buck on 9/12/16.
  */

import cli.DataStructureChooserCli
import implementationSearcher._
import org.scalatest.FunSpec
import parsers.MainParser
import shared._

class StandardLibraryChoosingSpec extends FunSpec {
  val minStackAdt = MainParser.nakedAdt.parse("""
    adt MinStack {
      insertLast! -> 1
      deleteLast! -> 1
      getByIndex -> 1
      getMinimum -> 1
    }""".trim()).get.value

  val impls = DataStructureChooserCli.impls
  val decls = DataStructureChooserCli.decls
  val structures = DataStructureChooserCli.dataStructures
  val library = ImplLibrary(impls, decls, structures)

  describe("regressions") {
    it("knows about how reduce works") {
      val res = Chooser.getRelevantTimesForDataStructures(library, Set(library.structures("VectorList")))
      assert(res.getNamed("reduce").contains(Impl("reduce[f] <- n * f + n")))
    }

    it("knows some things about getFirstBy") {
      val impl = Impl("getLastBy[f] <- reduce[_{commutative} <- f]")
      val unfreeImplSet = Chooser.getRelevantTimesForDataStructures(library, Set(library.structures("InvertibleReductionMemoizer")))
      val res = impl.bindToAllOptions(unfreeImplSet)

      assert(unfreeImplSet.impls(MethodName("reduce")).implsWhichMatchMethodExpr(
        MethodExpr.parse("reduce[_{commutative} <- f]"),
        unfreeImplSet,
        ParameterList.apply(ImplPredicateMap.empty, List(MethodName("f")))) == Set())

      assert(unfreeImplSet.getNamed("getSum") == Set(Impl("getSum <- 1")))
      assert(unfreeImplSet.getNamed("getLastBy") == Set())
    }
  }

  describe("simple choosing with stdlib impls") {
    describe("with min stack") {
      it("can correctly evaluate the performance of a generic heap and vector list") {
        val res = Chooser.getRelevantTimesForDataStructures(
          library,
          Set(structures("Heap"), structures("VectorList"))
        )

        assert(res.getNamed("getMinimum") == Set(Impl("getMinimum <- 1")))
        assert(res.getNamed("insertLast!") == Set(Impl("insertLast! <- log(n)")))
      }

      it("can correctly evaluate the performance of a stack min memoizer and vector list") {
        val res = Chooser.getRelevantTimesForDataStructures(
          library,
          Set(structures("DequeMemoizer"), structures("VectorList"))
        )

        assert(res.getNamed("getMinimum") == Set(Impl("getMinimum <- 1")))
        assert(res.getNamed("insertLast!") == Set(Impl("insertLast! <- 1")))
      }
    }
  }

  describe("data structure analysis") {
    it("can do a stack") {
      val adt = MainParser.nakedAdt.parse("""
        adt Stack {
          insertLast! -> 1
          deleteLast! -> 1
          getByIndex -> 1
          updateNode! -> 1
        }""".trim()).get.value

      val res = DataStructureChooserCli.chooseDataStructures(adt)

      assert(res.items.head.choices == Set("VectorList"))
    }

    it("can do a list") {
      val adt = MainParser.nakedAdt.parse("""
        adt List {
          insertAtIndex! -> 1
          getByIndex -> 1
          updateNode! -> 1
        }""".trim()).get.value

      val res = DataStructureChooserCli.chooseDataStructures(adt)

      assert(res.items.map(_.choices).contains(Set("RedBlackOrderStatisticTreeList")))
    }

    it("can do a min-stack") {
      val res = DataStructureChooserCli.chooseDataStructures(minStackAdt)

      DataStructureChooserCli.printResults(res)

      assert(res.items.head.choices == Set("DequeMemoizer", "VectorList"))
    }

    it("can do a stack with contains") {
      val res = DataStructureChooserCli.chooseDataStructures(MainParser.nakedAdt.parse("""
        adt Stack {
          insertLast! -> 1
          deleteLast! -> 1
          getByIndex -> 1
          updateNode! -> 1
          contains -> 1
        }""".trim()).get.value)

        DataStructureChooserCli.printResults(res)

        assert(res.items.head.choices == Set("HistogramHashMap", "VectorList"))
    }

    it("can do a set which you never delete from") {
      val res = DataStructureChooserCli.chooseDataStructures(AbstractDataType.parse("""
        adt NeverDeletedSet {
          insertLast! -> 1
          contains -> 1
        }""".trim()))

      DataStructureChooserCli.printResults(res)

      assert(res.items.head.choices == Set("HistogramHashMap"))
    }

    it("knows how to use parameterized data structures") {
      val adt = MainParser.nakedAdt.parse("""
        adt RmqList {
          insertAtIndex! -> 1
          getByIndex -> 1
          rangeMinimumQuery -> 1
        }""".trim()).get.value

      val res = DataStructureChooserCli.chooseDataStructures(adt)

      DataStructureChooserCli.printResults(res)

      assert(res.items.head.choices == Set("AugmentedRedBlackOrderStatisticTreeList"))
    }

    it("knows how to use RMQ") {
      val adt = MainParser.nakedAdt.parse("""
        adt RmqList {
          insertLast! -> 1
          getByIndex -> 1
          rangeMinimumQuery -> n
        }""".trim()).get.value

      val res = DataStructureChooserCli.chooseDataStructures(adt)

      DataStructureChooserCli.printResults(res)

      assert(res.items.head.choices == Set("RangeMinQueryLinearithmicArrayThing", "VectorList"))
    }

    it("can solve RMQ and count") {
      val adt = MainParser.nakedAdt.parse("""
        adt RmqList {
          insertLast! -> 1
          getByIndex -> 1
          rangeMinimumQuery -> 1
          count -> 1
        }""".trim()).get.value

      val res = DataStructureChooserCli.chooseDataStructures(adt)

      DataStructureChooserCli.printResults(res)

      assert(res.items.head.choices == Set("RangeMinQueryLinearithmicArrayThing", "HistogramHashMap", "VectorList"))
    }

    it("can do sum stack with random modification") {
      val adt = MainParser.nakedAdt.parse("""
        adt RandomlyAccessibleSumStack {
          insertLast! -> 1
          deleteLast! -> 1
          getSum -> 1
          getByIndex -> 1
          updateNode! -> 1
        }""".trim()).get.value

//      val res2 = Chooser.getRelevantTimesForDataStructures(library,
//        Set(structures("InvertibleReductionMemoizer"), structures("VectorList"))).filterToAdt(adt)

      val res = DataStructureChooserCli.chooseDataStructures(adt)

      DataStructureChooserCli.printResults(res)

      assert(res.items.head.choices == Set("VectorList", "InvertibleReductionMemoizer"))
    }

    it("can do min stack with random modification") {
      val adt = MainParser.nakedAdt.parse("""
        adt MinStackWithRandomModification {
          insertLast! -> 1
          deleteLast! -> 1
          getMinimum -> 1
          getByIndex -> 1
          updateNode! -> 1
        }""".trim()).get.value

      val res2 = Chooser.getRelevantTimesForDataStructures(library,
              Set(structures("InvertibleReductionMemoizer"), structures("VectorList"))).filterToAdt(adt)

      assert(res2.getNamed("getMinimum") == Set(Impl("getMinimum <- n")))
      val res = DataStructureChooserCli.chooseDataStructures(adt)

      DataStructureChooserCli.printResults(res)

      assert(res.items.map(_.choices) == Set(Set("VectorList", "OrderedRedBlackTree")))
    }
  }
}
