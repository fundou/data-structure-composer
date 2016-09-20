package tests

import implementationSearcher._
import org.scalatest.FunSpec


/**
  * Created by buck on 9/18/16.
  */
class UnfreeImplSpec extends FunSpec {
  describe("bindToContext") {
    //    it("correctly infers times in the simple case") {
    //    }

    val emptyLhs = ImplLhs("whatever")
    val emptySearchResults = SearchResult(Map())

    it("does simple constant time test") {
      val unfreeImpl = UnfreeImpl("f <- 1")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f"), emptyLhs, emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("1"))
    }

    it("does simple linear time test") {
      val unfreeImpl = UnfreeImpl("f <- n")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f"), emptyLhs, emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("n"))
    }

    it("correctly passes parameters through") {
      val unfreeImpl = UnfreeImpl("f[x] <- n * x")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f[y]"), ImplLhs("what", List("y")), emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("n * y"))
    }

    it("correctly deals with inapplicable anonymous functions") {
      val unfreeImpl = UnfreeImpl("f[x] <- x")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f[_]"), emptyLhs, emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("1"))
    }

    it("correctly deals with applicable anonymous functions") {
      val unfreeImpl = UnfreeImpl("f[x] if x.foo <- x")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f[_{foo}]"), emptyLhs, emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("1"))
    }

    it("returns sums") {
      val unfreeImpl = UnfreeImpl("f[x] <- x + log(n)")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("g[y]"), ImplLhs("what", List("y")), emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("y + log(n)"))
    }

    it("notices when anonymous functions don't match the impl conditions") {
      val unfreeImpl = UnfreeImpl("f[x] if x.foo <- x")
      val res = unfreeImpl.bindToContext(MethodExpr.parse("f[_]"), emptyLhs, emptySearchResults)

      assert(res.isEmpty)
    }

    it("reports impl conditions for named args") {
      val unfreeImpl = UnfreeImpl("f[x] if x.foo <- x")
      val Some((conditions, rhs)) = unfreeImpl.bindToContext(MethodExpr.parse("f[y]"), ImplLhs("what", List("y")), emptySearchResults)

      assert(conditions == ImplPredicateMap(Map("y" -> Set("foo"))))
      assert(rhs == UnfreeImpl.rhs("y"))
    }

    it ("doesn't screw up an obvious thing (regression test)") {
      val unfreeImpl = UnfreeImpl("getFirstBy[f]  <- n * f + n")
      val Some((conditions, rhs)) = unfreeImpl.bindToContext(MethodExpr.parse("getFirstBy[y]"), ImplLhs("what", List("y")), emptySearchResults)

      assert(conditions.isEmpty)
      assert(rhs == UnfreeImpl.rhs("n * y + n"))
    }
  }
}
