package generator;

public class Test {
//    public static Gen<String, Void> gen() {
//        Gen.yield("1");
//        Gen.yield("2");
//        Gen.yield("3");
//        Gen.yield("4");
//        return Gen.ret();
//    }

    public static Gen<String, Void> gen(int times, double mul) {
        mul -= 0.5;
        for (; times > 0; times -= (int) mul) {
            Gen.yield("iteration number: " + times*mul);
        }
        return Gen.ret();
    }
//    public static Gen<String, Void> gen(int times, double mul) {
//        mul -= 0.5;
//        for (int i = 0; i < times; i++) {
//            Gen.yield("iteration number: " + i*mul);
//        }
//        return Gen.ret();
//    }
}
