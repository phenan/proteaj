package proteaj.pparser;

import proteaj.ir.Environment;
import proteaj.ir.tast.*;

import javassist.*;

public class ExpressionStatementParser extends ComposedParser_Sequential {
  /* ExpressionStatement
   *  : Expression ';'
   */
  private ExpressionStatementParser() {
    super("ExpressionStatementParser");
  }

  @Override
  protected PackratParser[] getParsers(Environment env) {
    return new PackratParser[] { ExpressionParser.getParser(CtClass.voidType, env), KeywordParser.getParser(";") };
  }

  @Override
  protected TypedAST makeAST(int pos, int line, String file, TypedAST... as) {
    return new ExpressionStatement((Expression)as[0]);
  }

  public static final ExpressionStatementParser parser = new ExpressionStatementParser();
}

