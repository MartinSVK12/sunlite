using SunliteSharp.Core.Enum;

namespace SunliteSharp.Core.AST;

public abstract record Expr : Element
{
    public abstract int GetLine();
    public abstract string GetFile();
    public abstract Type GetExprType();
    
    public interface INamedExpr : Element
    {
        Token GetNameToken();
    }
    
    public interface IGenericExpr : Element
    {
        List<Param> GetTypeArguments();
    }

    public record Binary(Expr Left, Token Operator, Expr Right) : Expr {
        public override int GetLine()
        {
            return Operator.Line;
        }

        public override string GetFile()
        {
            return Operator.File;
        }

        public override Type GetExprType()
        {
            if (Operator.Type is TokenType.Star or TokenType.Slash or TokenType.Minus)
            {
                return Type.Number;
            }

            if (Operator.Type is not TokenType.Plus)
            {
                return Type.Boolean;
            }

            if(Left.GetExprType() == Type.Number && Right.GetExprType() == Type.Number)
            {
                return Type.Number;
            }

            return Type.String;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitBinaryExpr(this);
        }
    }

    public record Grouping(Expr Expression) : Expr
    {
        public override int GetLine()
        {
            return Expression.GetLine();
        }

        public override string GetFile()
        {
            return Expression.GetFile();
        }

        public override Type GetExprType()
        {
            return Expression.GetExprType();
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitGroupingExpr(this);
        }
    }

    public record Unary(Token Operator, Expr Right) : Expr
    {
        public override int GetLine()
        {
            return Operator.Line;
        }
        public override string GetFile()
        {
            return Operator.File;
        }
        public override Type GetExprType()
        {
            return Right.GetExprType();
        }
        public override void Accept(IVisitor visitor)
        {
            visitor.VisitUnaryExpr(this);
        }
    }

    public record Literal(object? Value, int LineNumber, string CurrentFile, Type Type) : Expr
    {
        public override int GetLine()
        {
            return LineNumber;
        }
        public override string GetFile()
        {
            return CurrentFile;
        }
        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitLiteralExpr(this);
        }
    }

    public record Variable(Token Name, Type Type, bool Constant) : Expr, INamedExpr
    {
        public override int GetLine()
        {
            return Name.Line;
        }

        public override string GetFile()
        {
            return Name.File;
        }

        public Token GetNameToken()
        {
            return Name;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitVariableExpr(this);
        }
    }
    
    public record Assign(Token Name, Expr Value, TokenType Operator, Type Type) : Expr, INamedExpr
    {
        public override int GetLine()
        {
            return Name.Line;
        }

        public override string GetFile()
        {
            return Name.File;
        }

        public Token GetNameToken()
        {
            return Name;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitAssignExpr(this);
        }
    }
    
    public record Logical(Expr Left, Token Operator, Expr Right) : Expr
    {
        public override int GetLine()
        {
            return Operator.Line;
        }

        public override string GetFile()
        {
            return Operator.File;
        }

        public override Type GetExprType()
        {
            return Type.Boolean;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitLogicalExpr(this);
        }
    }

    public record Call(Expr Callee, Token Paren, List<Expr> Arguments, List<Param> TypeArgs) : Expr, IGenericExpr
    {
        public override int GetLine()
        {
            return Paren.Line;
        }

        public override string GetFile()
        {
            return Paren.File;
        }

        public List<Param> GetTypeArguments()
        {
            return TypeArgs;
        }

        public override Type GetExprType()
        {
            var type = Callee.GetExprType();
            if (type is not Type.Reference refType) return Type.Unknown;
            if (refType.Type != PrimitiveType.Class || TypeArgs.Count == 0) return refType.ReturnType;
            var rawType = refType.ReturnType;
            return Type.OfGenericObject(rawType.Name(), TypeArgs);
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitCallExpr(this);
        }
    }

    public record Lambda(Stmt.Function Function) : Expr, INamedExpr
    {
        public override int GetLine()
        {
            return Function.Name.Line;
        }

        public override string GetFile()
        {
            return Function.Name.File;
        }

        public Token GetNameToken()
        {
            return Function.Name;
        }

        public override Type GetExprType()
        {
            return Type.OfFunction(Function.Name.Lexeme, Function.ReturnType, Function.Params);
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitLambdaExpr(this);
        }
    }

    public record Get : Expr, INamedExpr, IGenericExpr
    {

        public Get(Expr obj, Token name, Type type, bool constant, List<Param> typeParams)
        {
            Obj = obj;
            Name = name;
            Type = type;
            Constant = constant;
            List<Param> list = [];
            if (obj.GetExprType() is Type.Reference { Type: PrimitiveType.Object } refType)
            {
                list = refType.TypeParams;
            }
            TypeParams = list;
        }

        public Expr Obj;
        public Token Name;
        public Type Type;
        public bool Constant;
        public List<Param> TypeParams;
        
        public override int GetLine()
        {
            return Name.Line;
        }

        public override string GetFile()
        {
            return Name.File;
        }

        public List<Param> GetTypeArguments()
        {
            return TypeParams;
        }

        public Token GetNameToken()
        {
            return Name;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitGetExpr(this);
        }
    }

    public record ArrayGet(Expr Obj, Expr What, Token Token) : Expr
    {
        public override int GetLine()
        {
            return Token.Line;
        }

        public override string GetFile()
        {
            return Token.File;
        }

        public override Type GetExprType()
        {
            if (Obj.GetExprType() is Type.Reference { Type: PrimitiveType.Array or PrimitiveType.Table} refType)
            {
                return refType.ReturnType;
            }
            return Type.Unknown;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitArrayGetExpr(this);
        }
    }
    
    public record Set(Expr Obj, Token Name, Expr Value, TokenType Operator, Type Type) : Expr, INamedExpr
    {
        public override int GetLine()
        {
            return Name.Line;
        }

        public override string GetFile()
        {
            return Name.File;
        }

        public Token GetNameToken()
        {
            return Name;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitSetExpr(this);
        }
    }
    
    public record ArraySet(Expr Obj, Expr What, Expr Value, Token Token, TokenType Operator, Type Type) : Expr
    {
        public override int GetLine()
        {
            return Token.Line;
        }

        public override string GetFile()
        {
            return Token.File;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitArraySetExpr(this);
        }
    }

    public record This(Token Keyword, Type Type) : Expr
    {
        public override int GetLine()
        {
            return Keyword.Line;
        }

        public override string GetFile()
        {
            return Keyword.File;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitThisExpr(this);
        }
    }

    public record Super(Token Keyword, Token Method, Type Type) : Expr, INamedExpr
    {
        public override int GetLine()
        {
            return Keyword.Line;
        }

        public override string GetFile()
        {
            return Keyword.File;
        }

        public Token GetNameToken()
        {
            return Method;
        }

        public override Type GetExprType()
        {
            return Type;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitSuperExpr(this);
        }
    }

    public record Check(Expr Left, Token Operator, Type Right) : Expr
    {
        public override int GetLine()
        {
            return Operator.Line;
        }

        public override string GetFile()
        {
            return Operator.File;
        }

        public override Type GetExprType()
        {
            return Type.Boolean;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitCheckExpr(this);
        }
    }

    public record Cast(Expr Left, Token Operator, Type Right) : Expr
    {
        public override int GetLine()
        {
            return Operator.Line;
        }

        public override string GetFile()
        {
            return Operator.File;
        }

        public override Type GetExprType()
        {
            return Right;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitCastExpr(this);
        }
    }
    
    public interface IVisitor
    {
        void VisitBinaryExpr(Binary expr);
        void VisitGroupingExpr(Grouping grouping);
        void VisitUnaryExpr(Unary unary);
        void VisitLiteralExpr(Literal expr);
        void VisitVariableExpr(Variable expr);
        void VisitAssignExpr(Assign expr);
        void VisitLogicalExpr(Logical expr);
        void VisitCallExpr(Call expr);
        void VisitLambdaExpr(Lambda expr);
        void VisitGetExpr(Get expr);
        void VisitArrayGetExpr(ArrayGet expr);
        void VisitArraySetExpr(ArraySet expr);
        void VisitSetExpr(Set expr);
        void VisitThisExpr(This expr);
        void VisitSuperExpr(Super expr);
        void VisitCheckExpr(Check expr);
        void VisitCastExpr(Cast expr);
    }
    
    public abstract void Accept(IVisitor visitor);
    
}