package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;


public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        //it是StringIter变量啊orz  先读入所有文本
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        //返回文件结尾符号
        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        //特别说明一下，对于形如 123abc 这种输入，我们应当识别为两个 token，即 123 和 abc。
        //其实这里不用考虑这个啊  因为他们都有一个公共的it 所以就有一个公共的currentPos 当用lexUInt读完123的时候currentPos就指向a了
        //然后就会自动去lex这个a了  不用在数字里面单独考虑的
        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    //  处理无符号整数 Unsigned Int  我还以为是啥呢orz
    private Token lexUInt() throws TokenizeError {
        // 直到查看下一个字符不是数字为止:
        // -- 前进一个字符，并存储这个字符
        Pos startPosition = it.currentPos();
        long longValue = 0;
        int intValue = 0;

        while (Character.isDigit(it.peekChar())) {
            longValue *= 10;
            longValue += it.nextChar() - '0';
        }

//       如果超过了int的最大值  就出错了！直接报错结束
        if (longValue > Integer.MAX_VALUE) {
            throw new TokenizeError(ErrorCode.IntegerOverflow, it.currentPos());
        } else {
            intValue = (int) longValue;
            // 开始位置是之前记录了的，现在正运行到的位置就是结束位置了
            Token curToken = new Token(TokenType.Uint, intValue, startPosition, it.currentPos());
            return curToken; //返回这个无符号数字! over
        }

        // 解析存储的字符串为无符号整数
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        // Token 的 Value 应填写数字的值
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 直到查看下一个字符不是数字或字母为止:
        // -- 前进一个字符，并存储这个字符

        Pos startPosition = it.currentPos();
        StringBuffer res = new StringBuffer();
        while (Character.isDigit(it.peekChar()) || Character.isAlphabetic(it.peekChar())) {
            res.append(it.nextChar());
        }

        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        // Token 的 Value 应填写标识符或关键字的字符串
        Token curToken;
        switch (res.toString()) {
            case "begin":
                curToken = new Token(TokenType.Begin, res.toString(), startPosition, it.currentPos());
                return curToken;

            case "end":
                curToken = new Token(TokenType.End, res.toString(), startPosition, it.currentPos());
                return curToken;

            case "print":
                curToken = new Token(TokenType.Print, res.toString(), startPosition, it.currentPos());
                return curToken;

            case "const":
                curToken = new Token(TokenType.Const, res.toString(), startPosition, it.currentPos());
                return curToken;

            case "var":
                curToken = new Token(TokenType.Var, res.toString(), startPosition, it.currentPos());
                return curToken;

            default:
                curToken = new Token(TokenType.Ident, res.toString(), startPosition, it.currentPos());
                return curToken;
        }
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        switch (it.nextChar()) {
            case '+':
                return new Token(TokenType.Plus, '+', it.previousPos(), it.currentPos());

            case '-':
                return new Token(TokenType.Minus, '-', it.previousPos(), it.currentPos());

            case '*':
                return new Token(TokenType.Mult, '*', it.previousPos(), it.currentPos());

            case '/':
                return new Token(TokenType.Div, '/', it.previousPos(), it.currentPos());

            case '=':
                return new Token(TokenType.Equal, '=', it.previousPos(), it.currentPos());

            case ';':
                return new Token(TokenType.Semicolon, ';', it.previousPos(), it.currentPos());

            case '(':
                return new Token(TokenType.LParen, '(', it.previousPos(), it.currentPos());

            case ')':
                return new Token(TokenType.RParen, ')', it.previousPos(), it.currentPos());

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    private void skipSpaceCharacters() {
        //只要没到结尾  然后偷看下一个字符是空白 就跳过去
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }

    public static void main(String[] args) {
        System.out.println("hello");
        long ans = Integer.MAX_VALUE;
        System.out.println(ans);
    }
}
