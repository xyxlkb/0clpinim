package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions; //语法分析返回的就是一个操作列表

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     * @throws TokenizeError
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return
     * @throws TokenizeError
     */
    private Token next() throws TokenizeError {
        // 因为peek的时候就会把token的指针往后移动一个，所以如果peek过了直接拿来用 这也是为什么要用一个peekedtoken变量记录peek了的东西
        // 没peek过才能继续往后移动指针看下一个token
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt
     * @return
     * @throws TokenizeError
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * expect是期待 所以就是期待错误就报错的意思  不是check
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     * 
     * @return
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        // 如果符号表里已经有了那就是重定义嘛  所以直接报错
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        // 给它赋值 结果发现他根本没被定义过
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    private void analyseProgram() throws CompileError {
        // 程序 -> 'begin' 主过程 'end'
        // 示例函数，示例如何调用子程序
        // 'begin'
        expect(TokenType.Begin);

        analyseMain();

        // 'end'
        expect(TokenType.End);
        expect(TokenType.EOF);
    }

    private void analyseMain() throws CompileError {
        // 主过程 -> 常量声明 变量声明 语句序列
        analyseConstantDeclaration();
        analyseVariableDeclaration();
        analyseStatementSequence();
    }

    // 示例函数，示例如何解析常量声明
    private void analyseConstantDeclaration() throws CompileError {
        // 常量声明 -> 常量声明语句*

        // 如果下一个 token 是 const 就继续
        while (nextIf(TokenType.Const) != null) {
            // 常量声明语句 -> 'const' 变量名 '=' 常表达式 ';'

            // 变量名
            var nameToken = expect(TokenType.Ident);

            // 加入符号表
            String name = (String) nameToken.getValue();
            addSymbol(name, true, true, nameToken.getStartPos());

            // 等于号
            expect(TokenType.Equal);

            // 常表达式
            var value = analyseConstantExpression();

            // 分号
            expect(TokenType.Semicolon);

            // 这里把常量值直接放进栈里，位置和符号表记录的一样。
            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值，
            // 我们这里就先不这么干了。
            instructions.add(new Instruction(Operation.LIT, value));
        }
    }

    private void analyseVariableDeclaration() throws CompileError {
        // 变量声明 -> 变量声明语句*

        // 如果下一个 token 是 var 就继续
        while (nextIf(TokenType.Var) != null) {
            // 变量声明语句 -> 'var' 变量名 ('=' 表达式)? ';'

            // 变量名
            var nameToken = expect(TokenType.Ident);

            // 变量初始化了吗
            boolean initialized = false;

            // 下个 token 是等于号吗？如果是的话分析初始化
            int value = 0;
            if(check(TokenType.Equal)){
                next(); //check是不会往后跳一格的
                analyseExpression();
                initialized = true;
            }
            // 分析初始化的表达式

            // 分号
            expect(TokenType.Semicolon);

            // 加入符号表，请填写名字和当前位置（报错用）
            String name = (String) nameToken.getValue();
            addSymbol(name, initialized, false, nameToken.getStartPos());

            // 如果没有初始化的话在栈里推入一个初始值  value初始化为0就行
            if (!initialized) {
                instructions.add(new Instruction(Operation.LIT, value));
            }
            else{
                instructions.add(new Instruction(Operation.LIT, value));
            }
        }
    }

    private void analyseStatementSequence() throws CompileError {
        // 语句序列 -> 语句*
        // 语句 -> 赋值语句 | 输出语句 | 空语句

        while (true) {
            // 如果下一个 token 是……
            Token peekedToken = peek();
            if (peekedToken.getTokenType() == TokenType.Ident) {
                //如果偷看到是赋值语句：直接调用赋值语句函数啊
                analyseAssignmentStatement();
                // 调用相应的分析函数
                // 如果遇到其他非终结符的 FIRST 集呢？
            }
            else if(peekedToken.getTokenType() == TokenType.Print){
                analyseOutputStatement();
            }
            //空语句就是只有一个分号的
            else if(peekedToken.getTokenType() == TokenType.Semicolon){
                next();
            }
            else {
                // 都不是，摸了
                break;
            }
        }
    }

    private int analyseConstantExpression() throws CompileError {
        // 常表达式 -> 符号? 无符号整数
        boolean negative = false;
        if (nextIf(TokenType.Plus) != null) {
            negative = false;
        } else if (nextIf(TokenType.Minus) != null) {
            negative = true;
        }

        var token = expect(TokenType.Uint);

        int value = (int) token.getValue();
        if (negative) {
            value = -value;
        }

        return value;
    }

    private void analyseExpression() throws CompileError {
        // 表达式 -> 项 (加法运算符 项)*
        // 项
        analyseItem();

        while (true) {
            // 预读可能是运算符的 token
            var op = peek();
            if (op.getTokenType() != TokenType.Plus && op.getTokenType() != TokenType.Minus) {
                break;
            }

            // 运算符
            next();

            // 项
            analyseItem();

            // 生成代码
            if (op.getTokenType() == TokenType.Plus) {
                instructions.add(new Instruction(Operation.ADD));
            } else if (op.getTokenType() == TokenType.Minus) {
                instructions.add(new Instruction(Operation.SUB));
            }
        }
    }

    private void analyseAssignmentStatement() throws CompileError {
        // 赋值语句 -> 标识符 '=' 表达式 ';'

        // 分析这个语句

        // 标识符是什么？
        Token nameToken = expect(TokenType.Ident);
        String name = (String) nameToken.getValue();
        var symbol = symbolTable.get(name);
        if (symbol == null) {
            // 没定义就想赋值？ 直接报错不bb
            throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ nameToken.getStartPos());
        } else if (symbol.isConstant) {
            // 标识符是常量
            throw new AnalyzeError(ErrorCode.AssignToConstant, /* 当前位置 */ nameToken.getStartPos());
        }
        // 设置符号已初始化
        initializeSymbol(name, nameToken.getStartPos());

        expect(TokenType.Equal);
        analyseExpression();   // 因为每压入一个操作符，之后就是会压入一个操作数？
        expect(TokenType.Semicolon);

        // 把结果保存
        var offset = getOffset(name, nameToken.getStartPos());
        instructions.add(new Instruction(Operation.STO, offset));
    }

    private void analyseOutputStatement() throws CompileError {
        // 输出语句 -> 'print' '(' 表达式 ')' ';'

        expect(TokenType.Print);
        expect(TokenType.LParen);

        analyseExpression();

        expect(TokenType.RParen);
        expect(TokenType.Semicolon);

        instructions.add(new Instruction(Operation.WRT));
    }

    private void analyseItem() throws CompileError {
        // 项 -> 因子 (乘法运算符 因子)*

        // 因子
        analyseFactor();

        while (true) {
            // 预读可能是运算符的 token
            Token op = peek();
            if(op.getTokenType() == TokenType.Mult || op.getTokenType() == TokenType.Div){
                // 运算符
                next();
                // 因子
                analyseFactor();
                // 生成代码
                if (op.getTokenType() == TokenType.Mult) {
                    instructions.add(new Instruction(Operation.MUL));
                } else if (op.getTokenType() == TokenType.Div) {
                    instructions.add(new Instruction(Operation.DIV));
                }
            }
            else{
                break;
            }
        }
    }

    private void analyseFactor() throws CompileError {
        // 因子 -> 符号? (标识符 | 无符号整数 | '(' 表达式 ')')

        boolean negate;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
            // 计算结果需要被 0 减
            instructions.add(new Instruction(Operation.LIT, 0));
        } else {
            nextIf(TokenType.Plus);
            negate = false;
        }

        if (check(TokenType.Ident)) {
            // 是标识符  check是不会往后走一格的

            // 加载标识符的值
            Token nameToken = expect(TokenType.Ident);
            String name = (String) nameToken.getValue();
            var symbol = symbolTable.get(name);
            if (symbol == null) {
                // 没有这个标识符
                throw new AnalyzeError(ErrorCode.NotDeclared, /* 当前位置 */ nameToken.getStartPos());
            } else if (!symbol.isInitialized) {
                // 标识符没初始化
                throw new AnalyzeError(ErrorCode.NotInitialized, /* 当前位置 */ nameToken.getStartPos());
            }
            int offset = getOffset(name, nameToken.getStartPos());
            instructions.add(new Instruction(Operation.LOD, offset));
        } else if (check(TokenType.Uint)) {
            // 是整数
            // 加载整数值
            Token nameToken = expect(TokenType.Uint);
            int value = (int) nameToken.getValue();
            instructions.add(new Instruction(Operation.LIT, value));
        } else if (check(TokenType.LParen)) {
            // 是表达式
            expect(TokenType.LParen);
            analyseExpression();
            expect(TokenType.RParen);
            // 调用相应的处理函数
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
        }

        if (negate) {
            instructions.add(new Instruction(Operation.SUB));
        }
    }
}
