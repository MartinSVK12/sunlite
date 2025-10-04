using SunliteSharp.Core.Enum;
using SunliteSharp.Core.Modifier;

namespace SunliteSharp.Core.AST;

public abstract record Stmt : Element
{
    public abstract int GetLine();
    public abstract string GetFile();
    
    public interface INamedStmt : Element
    {
        Token GetNameToken();
    }

    public record Expression(Expr Expr) : Stmt
    {
        public override int GetLine()
        {
            return Expr.GetLine();
        }

        public override string GetFile()
        {
            return Expr.GetFile();
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitExprStmt(this);
        }
    }
    
    public record Function(Token Name, FunctionType Type, List<Param> Params, List<Stmt> Body, FunctionModifier Modifier, Type ReturnType, List<Param> TypeParams) : Stmt, INamedStmt
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

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitFunctionStmt(this);
        }
    }

    public record Var(Token Name, Type Type, Expr? Initializer, FieldModifier Modifier) : Stmt, INamedStmt
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

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitVarStmt(this);
        }
    }
    
    public record Block(List<Stmt> Statements, int LineNumber, string CurrentFile) : Stmt, INamedStmt
    {
        public override int GetLine()
        {
            return LineNumber;
        }

        public override string GetFile()
        {
            return CurrentFile;
        }

        public Token GetNameToken()
        {
            return Token.Identifier("<block>", GetFile());
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitBlockStmt(this);
        }
    }
    
    public record If(Expr Condition, Stmt ThenBranch, Stmt? ElseBranch) : Stmt
    {
        public override int GetLine()
        {
            return Condition.GetLine();
        }

        public override string GetFile()
        {
            return Condition.GetFile();
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitIfStmt(this);
        }
    }

    public record While(Expr Condition, Stmt Body) : Stmt
    {
        public override int GetLine()
        {
            return Condition.GetLine();
        }

        public override string GetFile()
        {
            return Condition.GetFile();
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitWhileStmt(this);
        }
    }

    public record Break(Token Keyword) : Stmt
    {
        public override int GetLine()
        {
            return Keyword.Line;
        }

        public override string GetFile()
        {
            return Keyword.File;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitBreakStmt(this);
        }
    }

    public record Continue(Token Keyword) : Stmt
    {
        public override int GetLine()
        {
            return Keyword.Line;
        }

        public override string GetFile()
        {
            return Keyword.File;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitContinueStmt(this);
        }
    }
    
    public record Return(Token Keyword, Expr? Value) : Stmt
    {
        public override int GetLine()
        {
            return Keyword.Line;
        }

        public override string GetFile()
        {
            return Keyword.File;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitReturnStmt(this);
        }
    }

    public record Import(Token Keyword, Token What) : Stmt
    {
        public override int GetLine()
        {
            return Keyword.Line;
        }

        public override string GetFile()
        {
            return Keyword.File;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitImportStmt(this);
        }
    }

    public record Class(
        Token Name,
        List<Function> Methods,
        List<Var> FieldDefaults,
        Expr.Variable? Superclass,
        List<Expr.Variable> Superinterfaces,
        ClassModifier Modifier,
        List<Param> TypeParams) : Stmt, INamedStmt
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

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitClassStmt(this);
        }
    }
    
    public record Interface(Token Name, List<Function> Methods, List<Expr.Variable> Superinterfaces, List<Param> TypeParameters) : Stmt, INamedStmt
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

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitInterfaceStmt(this);
        }
    }
    
    public record TryCatch(Token TryToken, Token CatchToken, Block TryBody, Param CatchVariable, Block CatchBody) : Stmt
    {
        public override int GetLine()
        {
            return TryToken.Line;
        }

        public override string GetFile()
        {
            return TryToken.File;
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitTryCatchStmt(this);
        }
    }

    public record Throw(Expr Expr) : Stmt
    {
        public override int GetLine()
        {
            return Expr.GetLine();
        }

        public override string GetFile()
        {
            return Expr.GetFile();
        }

        public override void Accept(IVisitor visitor)
        {
            visitor.VisitThrowStmt(this);
        }
    }
    
    public interface IVisitor
    {
        void VisitExprStmt(Expression stmt);
        void VisitVarStmt(Var stmt);
        void VisitBlockStmt(Block stmt);
        void VisitIfStmt(If stmt);
        void VisitWhileStmt(While stmt);
        void VisitBreakStmt(Break stmt);
        void VisitContinueStmt(Continue stmt);
        void VisitFunctionStmt(Function stmt);
        void VisitReturnStmt(Return stmt);
        void VisitClassStmt(Class stmt);
        void VisitInterfaceStmt(Interface stmt);
        void VisitImportStmt(Import stmt);
        void VisitTryCatchStmt(TryCatch stmt);
        void VisitThrowStmt(Throw stmt);
    }
    
    public abstract void Accept(IVisitor visitor);
}