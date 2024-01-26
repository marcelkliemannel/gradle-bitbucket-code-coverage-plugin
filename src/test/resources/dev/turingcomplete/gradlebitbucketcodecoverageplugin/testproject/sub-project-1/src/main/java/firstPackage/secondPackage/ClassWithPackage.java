package firstPackage.secondPackage;

public class ClassWithPackage implements InterfaceWithoutDefaults {

    public static final String STATIC_PARAM = "Static Param";

    public int intParam = Integer.parseInt("23");
    public Object defaultValue = null;

    public ClassWithPackage() {
        new ClassWithPackage.InnerClass().innerNotCovered();
    }

    public void methodFullyCovered() {
        System.out.println("First");
        System.out.println("Second");
    }

    public void methodPartlyCovered(boolean param) {
        if (param) {
            System.out.println("Param set");
        }
        else {
            System.out.println("Param not set");
        }
    }

    public void methodNotCovered() {
        System.out.println("First");
        System.out.println("Second");
    }

    public void anonymousClass() {
        new Object() {
            public void anonymousNotCovered() {
                System.out.println("First");
            }
        };
    }

    private class InnerClass {
        public void innerNotCovered() {
            System.out.println("First");
        }
    }

    public static class StaticInnerClass {
        public void staticInnerCovered() {
            System.out.println("First");
        }
    }
}