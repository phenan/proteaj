package proteaj.ast;

public class FieldDecl extends AST {
  public FieldDecl(String type, String name, int line) {
    super(line);
    this.modifiers = 0;
    this.type = type;
    this.name = name;
    this.body = null;
    this.bodyLine = 0;
  }

  public void setModifiers(int modifiers) {
    this.modifiers = modifiers;
  }

  public void setBody(String body, int line) {
    this.body = body;
    this.bodyLine = line;
  }

  public boolean hasBody() {
    return body != null;
  }

  public int getModifiers() {
    return modifiers;
  }

  public String getBody() {
    return body;
  }

  public int getBodyLine() {
    return bodyLine;
  }

  public final String type;
  public final String name;

  private int modifiers;
  private String body;
  private int bodyLine;
}

