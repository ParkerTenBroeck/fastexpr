package fastexpr.util;

public class Peekable<T> implements Iterator<T>{
    final Iterator<T> iter;
    T lookahead;

    public Peekable(Iterator<T> iter){
        this.iter = iter;
    }

    @Override
    public T next(){
        if (this.lookahead != null) {
            T tmp = this.lookahead;
            this.lookahead = null;
            return tmp;
        }else{
            return iter.next();
        }
    }

    public T peek(){
        if (this.lookahead == null) this.lookahead = iter.next();
        return this.lookahead;
    }
}
