package generator;

public interface Gen<Y, R> {
    Res<Y, R> next();

    static <Y, R> Gen<Y, R> yield(Y y) {
        throw new RuntimeException();
    }
    static <Y, R> Gen<Void, R> yield() {
        throw new RuntimeException();
    }
    static <Y, R> Gen<Y, R> ret(R r) {throw new RuntimeException();}
    static <Y, R> Gen<Y, Void> ret() {
        throw new RuntimeException();
    }

    static <R> R await(Gen<Void, R> gen){
        while(true){
            var res = gen.next();
            if(res instanceof Ret(R r))return r;
            Gen.yield();
        }
    }

    sealed interface Res<Y, R>{}
    record Yield<Y, R>(Y y) implements Res<Y, R>{}
    record Ret<Y, R>(R y) implements Res<Y, R>{}
}
