package proteaj.pparser;

import proteaj.util.Pair;

import java.util.*;
import java.util.stream.Stream;

public class PackratReader {
  public PackratReader(String source, String filePath, int line) {
    this.source = source;
    this.filePath = filePath;
    this.current = 0;
    this.state = new PackratParserState();

    this.lines = createLinesMap(line);
  }

  public int getLine() {
    return getLine(current);
  }

  public int getLine(int pos) {
    return lines.lowerEntry(pos).getValue();
  }

  public boolean hasNext() {
    return current < source.length();
  }

  public boolean hasNext(int i) {
    return current + i < source.length();
  }

  public int lookahead() {
    if(hasNext()) return source.charAt(current);
    else return -1;
  }

  public int lookahead(int i) {
    if(hasNext(i)) return source.charAt(current + i);
    else return -1;
  }

  public String untilNextWhitespace () {
    StringBuilder buf = new StringBuilder();

    int index = 0;

    while (Character.isWhitespace(lookahead(index))) index++;

    while (hasNext(index) && ! Character.isWhitespace(lookahead(index))) {
      buf.appendCodePoint(lookahead(index++));
    }

    return buf.toString();
  }

  public String getSourceFrom (int pos) {
    return source.substring(pos, current);
  }

  public char next() {
    assert hasNext();
    return source.charAt(current++);
  }

  public int getPos() {
    return current;
  }

  public void setPos(int pos) {
    current = pos;
  }

  public Stream<Failure<?>> getAllFailures() { return state.getAllFailures(); }

  public <T> MemoTable<T> memos (PackratParser<T> parser) {
    return state.getMemoTable(parser);
  }

  private TreeMap<Integer, Integer> createLinesMap(int line) {
    TreeMap<Integer, Integer> map = new TreeMap<Integer, Integer>();

    map.put(-1, line);
    int length = source.length();

    for(int i = 0; i < length; i++) {
      char ch = source.charAt(i);
      if(ch == '\n') map.put(i, ++line);
    }

    return map;
  }

  private int current;

  private final String source;
  private final TreeMap<Integer, Integer> lines;

  public final String filePath;

  public final PackratParserState state;
}

class PackratParserState {
  public <T> void push (PackratParser<T> parser) { lrStack.push(new LR<T>(parser)); }
  public LR head () { return lrStack.peek(); }
  public LR pop () { return lrStack.pop(); }
  public LinkedList<LR> lrList () { return lrStack; }

  public <T> MemoTable<T> getMemoTable (PackratParser<T> parser) {
    if (! memoTables.containsKey(parser)) memoTables.put(parser, new MemoTable<>());
    return (MemoTable<T>) memoTables.get(parser);
  }

  public Stream<Failure<?>> getAllFailures() {
    return memoTables.values().stream().flatMap(MemoTable::getAllFailures);
  }

  Map<Integer, Head> heads = new HashMap<>();

  private LinkedList<LR> lrStack = new LinkedList<>();
  private Map<PackratParser<?>, MemoTable<?>> memoTables = new HashMap<>();
}

class MemoTable<T> {
  public MemoTable() {
    memos = new HashMap<>();
  }

  public ParseResult<T> memoize(int bPos, ParseResult<T> ast, Integer ePos) {
    if (memos.containsKey(bPos)) {
      ParseResult<T> memo = memos.get(bPos)._1;

      if (! (memo instanceof LR) && ! memo.isFail() && ast.isFail()) return memo;
    }

    memos.put(bPos, Pair.make(ast, ePos));
    return ast;
  }

  public Stream<Failure<?>> getAllFailures() {
    return memos.values().stream().map(pair -> pair._1).filter(result -> result instanceof Failure).map(result -> (Failure<?>)result);
  }

  public boolean contains(int pos) {
    return memos.containsKey(pos);
  }

  public Pair<ParseResult<T>, Integer> lookup(int pos) {
    return memos.get(pos);
  }

  private Map<Integer, Pair<ParseResult<T>, Integer>> memos;
}