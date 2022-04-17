import org.junit.jupiter.api.Test;
import firstPackage.secondPackage.ClassWithPackage;

class CoverageTest {
    @Test
    void test() {
        var classWithoutPackage = new ClassWithoutPackage();
        classWithoutPackage.methodFullyCovered();
        classWithoutPackage.methodPartlyCovered(true);
        classWithoutPackage.anonymousClass();
        new ClassWithoutPackage.StaticInnerClass().staticInnerCovered();

        var classWithPackage = new ClassWithPackage();
        classWithPackage.methodFullyCovered();
        classWithPackage.methodPartlyCovered(true);
        classWithPackage.anonymousClass();
        new ClassWithoutPackage.StaticInnerClass().staticInnerCovered();
    }
}