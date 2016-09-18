package implementationSearcher
import org.scalatest.FunSpec
import shared.{LogTime, ConstantTime}


/**
  * Created by buck on 9/18/16.
  */
class UnfreeImplSpec extends FunSpec {
  describe("bindToContext") {
    //    it("correctly infers times in the simple case") {
    //    }

    it("does simple constant time test") {
      val unfreeImpl = UnfreeImpl("f <- 1")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f"), ImplPredicateMap.empty)

      assert(conditions == ImplPredicateMap.empty)
      assert(rhs == UnfreeImpl.rhs("1"))
    }

    it("does simple linear time test") {
      val unfreeImpl = UnfreeImpl("f <- n")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f"), ImplPredicateMap.empty)

      assert(conditions == ImplPredicateMap.empty)
      assert(rhs == UnfreeImpl.rhs("n"))
    }

    it("correctly passes parameters through") {
      val unfreeImpl = UnfreeImpl("f[x] <- x * n")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f[y]"), ImplPredicateMap.empty)

      assert(conditions == ImplPredicateMap(Map("y" -> Set())))
      assert(rhs == UnfreeImpl.rhs("y * n"))
    }

    it("correctly deals with anonymous functions") {
      val unfreeImpl = UnfreeImpl("f[x] <- x")
      val Some((conditions, rhs)) =
        unfreeImpl.bindToContext(MethodExpr.parse("f[_]"), ImplPredicateMap.empty)

      assert(conditions == ImplPredicateMap.empty)
      assert(rhs == UnfreeImpl.rhs("1"))
    }
  }
}
