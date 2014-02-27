package proteaj.tast;

import javassist.*;
import proteaj.tast.util.ExpressionVisitor;

public class StaticFieldAccess extends Expression {
  public StaticFieldAccess(CtField field) throws NotFoundException {
    super(field.getType());
    this.field = field;
  }

  @Override
  public String toJavassistCode() {
    return field.getDeclaringClass().getName() + '.' + field.getName();
  }

  @Override
  public <T> T accept(ExpressionVisitor<T> visitor, T t) {
    return visitor.visit(this, t);
  }

  public final CtField field;
}

