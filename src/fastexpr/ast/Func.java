package fastexpr.ast;

import java.util.List;

public record Func(String name, List<Expr> args) implements Expr {
}
