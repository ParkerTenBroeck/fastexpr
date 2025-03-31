package generator;

public interface Gen<Y, R> {
    Res<Y, R> next();

    default void yield(Y y) {
        throw new RuntimeException();
    }
    default Res<Y, R> ret(R r) {
        throw new RuntimeException();
    }
    default Res<Y, Void> ret() {
        throw new RuntimeException();
    }

    sealed interface Res<Y, R>{}
    record Yield<Y, R>(Y y) implements Res<Y, R>{}
    record Ret<Y, R>(R y) implements Res<Y, R>{}
}
