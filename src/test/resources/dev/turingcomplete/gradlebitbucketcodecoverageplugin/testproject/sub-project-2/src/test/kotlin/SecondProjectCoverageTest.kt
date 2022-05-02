import org.junit.jupiter.api.Test

class SecondProjectCoverageTest {

  @Test
  fun test() {
    val classWithoutPackage = KotlinClass()
    classWithoutPackage.methodFullyCovered()
    classWithoutPackage.methodPartlyCovered(true)
    classWithoutPackage.anonymousClass()
    KotlinClass.StaticInnerClass().staticInnerCovered()
  }
}