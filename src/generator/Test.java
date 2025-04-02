package generator;

public class Test {
//    public static Gen<String, Void> gen() {
//        Gen.yield("1");
//        Gen.yield("2");
//        Gen.yield("3");
//        Gen.yield("4");
//        return Gen.ret();
//    }

    public static Gen<String, Void> parse(String str){
        {
            String meow = "10";
            meow += "11";
            Gen.yield(meow);
        }
        for(var split : str.split(" ")){
            Gen.yield(split);
        }
        {
            var str2 = str;
            while(str2.length()>10){
                var len = str2.length();
                Gen.yield(len+" length");
                str2 = str2.substring(1);
            }
        }

        while(str.length()>10){
            var len = str.length();
            Gen.yield(len+" length");
            str = str.substring(1);
        }
        return Gen.ret();
    }

//    public static Gen<String, Void> gen(int times, double mul) {
//        mul -= 0.5;
//        for (int i = 0; i < times; i ++) {
//            Gen.yield("iteration number: " + i*mul);
//        }
//        return Gen.ret();
//    }
//    public static Gen<String, Void> gen(int times, double mul) {
//        mul -= 0.5;
//        for (int i = 0; i < times; i++) {
//            Gen.yield("iteration number: " + i*mul);
//        }
//        return Gen.ret();
//    }
}
