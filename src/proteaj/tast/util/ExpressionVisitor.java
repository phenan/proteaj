package proteaj.tast.util;

import proteaj.tast.*;

public interface ExpressionVisitor<T> {
  public abstract T visit (Operation operation, T t);
  public abstract T visit (LocalsDecl localsDecl, T t);
  public abstract T visit (ExpressionList list, T t);
  public abstract T visit (AssignExpression assign, T t);
  public abstract T visit (TernaryIfExpression tif, T t);
  public abstract T visit (MethodCall methodCall, T t);
  public abstract T visit (StaticMethodCall methodCall, T t);
  public abstract T visit (FieldAccess fieldAccess, T t);
  public abstract T visit (StaticFieldAccess fieldAccess, T t);
  public abstract T visit (NewExpression newExpression, T t);
  public abstract T visit (NewArrayExpression newArray, T t);
  public abstract T visit (ArrayAccess arrayAccess, T t);
  public abstract T visit (ArrayInitializer arrayInitializer, T t);
  public abstract T visit (ArrayLength arrayLength, T t);
  public abstract T visit (ThisExpression thisExpr, T t);
  public abstract T visit (SuperExpression superExpr, T t);
  public abstract T visit (ParamAccess paramAccess, T t);
  public abstract T visit (LocalVariable local, T t);
  public abstract T visit (CastExpression castExpr, T t);
  public abstract T visit (VariableArguments varArgs, T t);

  public abstract T visit (StringLiteral stringLiteral, T t);
  public abstract T visit (CharLiteral charLiteral, T t);
  public abstract T visit (IntLiteral intLiteral, T t);
  public abstract T visit (FloatLiteral floatLiteral, T t);
  public abstract T visit (DoubleLiteral doubleLiteral, T t);
  public abstract T visit (BooleanLiteral booleanLiteral, T t);
  public abstract T visit (ClassLiteral classLiteral, T t);
  public abstract T visit (TypeLiteral typeLiteral, T t);
  public abstract T visit (NullLiteral nullLiteral, T t);
}
