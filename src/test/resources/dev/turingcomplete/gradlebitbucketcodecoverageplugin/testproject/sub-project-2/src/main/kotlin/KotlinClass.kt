class KotlinClass {

  companion object {
    val STATIC_PARAM = "Static Param"
  }

  var intParam = "23".toInt()
  var defaultValue: Any? = null

  fun ClassWithoutPackage() {
    InnerClass().innerNotCovered()
  }

  fun methodFullyCovered() {
    println("First")
    println("Second")
  }

  fun methodPartlyCovered(param: Boolean) {
    if (param) {
      println("Param set")
    }
    else {
      println("Param not set")
    }
  }

  fun methodNotCovered() {
    println("First")
    println("Second")
  }

  fun anonymousClass() {
    object : Any() {
      fun anonymousNotCovered() {
        println("First")
      }
    }
  }

  private class InnerClass {
    fun innerNotCovered() {
      println("First")
    }
  }

  class StaticInnerClass {
    fun staticInnerCovered() {
      println("First")
    }
  }
}