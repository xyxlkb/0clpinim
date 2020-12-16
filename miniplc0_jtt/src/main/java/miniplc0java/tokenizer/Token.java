package miniplc0java.tokenizer;

import miniplc0java.error.TokenizeError;
import miniplc0java.util.Pos;

import java.util.Objects;

public class Token {
    private TokenType tokenType;
    private Object value;
    private Pos startPos;
    private Pos endPos;

    //    构造函数 类型，值，开始位置，结束位置
    public Token(TokenType tokenType, Object value, Pos startPos, Pos endPos) {
        this.tokenType = tokenType;
        this.value = value;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    //    复制构造的构造函数
    public Token(Token token) {
        this.tokenType = token.tokenType;
        this.value = token.value;
        this.startPos = token.startPos;
        this.endPos = token.endPos;
    }

    //    重载 判断是否相等
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        // 先判断他们是同一种类型 再强制类型转换成Token来比较内容
        Token token = (Token) o;
        // 反正就是每一个值都相等才行 他都没有this.
        return tokenType == token.tokenType && Objects.equals(value, token.value)
                && Objects.equals(startPos, token.startPos) && Objects.equals(endPos, token.endPos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, value, startPos, endPos);
    }

    // 返回string类型的value
    public String getValueString() {
        if (value instanceof Integer || value instanceof String || value instanceof Character) {
            return value.toString();
        }
        throw new Error("No suitable cast for token value.");
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Pos getStartPos() {
        return startPos;
    }

    public void setStartPos(Pos startPos) {
        this.startPos = startPos;
    }

    public Pos getEndPos() {
        return endPos;
    }

    public void setEndPos(Pos endPos) {
        this.endPos = endPos;
    }

    // 重写tostring
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Line: ").append(this.startPos.row).append(' ');  //返回的是开始的位置
        sb.append("Column: ").append(this.startPos.col).append(' ');
        sb.append("Type: ").append(this.tokenType).append(' ');
        sb.append("Value: ").append(this.value);
        return sb.toString();
    }

    public String toStringAlt() {
        return new StringBuilder().append("Token(").append(this.tokenType).append(", value: ").append(value)
                .append(" at: ").append(this.startPos).toString();
    }

    public static void main(String[] args) {
        Pos startPos = new Pos(2,3);
        Pos endPos = new Pos(3,3);
        Token jttToken = new Token(TokenType.Ident,"value",startPos,endPos);
        System.out.println(jttToken.toStringAlt());
    }
}
