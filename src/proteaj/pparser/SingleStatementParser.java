package proteaj.pparser;

public class SingleStatementParser extends ComposedParser_Alternative {
  /* SingleStatement
   *  : Block
   *  | ControlFlow
   *  | ExpressionStatement
   */
  private SingleStatementParser() {
    super("SingleStatementParser");
  }

  @Override
  protected PackratParser[] getParsers() {
    return new PackratParser[] { BlockParser.parser, ControlFlowParser.parser, ExpressionStatementParser.parser };
  }

  public static final SingleStatementParser parser = new SingleStatementParser();
}

