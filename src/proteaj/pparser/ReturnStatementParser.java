package proteaj.pparser;

import proteaj.error.FailLog;
import proteaj.io.*;
import proteaj.ir.*;
import proteaj.ir.tast.*;

import javassist.*;

public class ReturnStatementParser extends PackratParser {
  /* ReturnStatement
   *  : "return" [ Expression ] ';'
   */
  @Override
  protected TypedAST parse(SourceStringReader reader, Environment env) {
    int pos = reader.getPos();

    if (! enable) return DISABLE;

    // "return"
    TypedAST keyword = KeywordParser.getParser("return").applyRule(reader, env);
    if(keyword.isFail()) {
      reader.setPos(pos);
      return new BadAST(keyword.getFailLog());
    }

    // [ Expression ]
    TypedAST val = null;
    if(! returnType.equals(CtClass.voidType)) {
      val = ExpressionParser.getParser(returnType).applyRule(reader, env);
      if(val.isFail()) {
        reader.setPos(pos);
        return new BadAST(val.getFailLog());
      }
    }

    // ';'
    TypedAST semicolon = KeywordParser.getParser(";").applyRule(reader, env);
    if(semicolon.isFail()) {
      reader.setPos(pos);
      return new BadAST(semicolon.getFailLog());
    }

    if(val != null) return new ReturnStatement((Expression)val);
    else return new ReturnStatement();
  }

  public void init(CtClass returnType) {
    this.returnType = returnType;
    this.enable = true;
  }

  public void disable() {
    this.enable = false;
  }

  public static final ReturnStatementParser parser = new ReturnStatementParser();

  private ReturnStatementParser() {}

  private CtClass returnType;
  private boolean enable;

  private static final BadAST DISABLE = new BadAST(new FailLog("disable parser", 0, 0));
}
