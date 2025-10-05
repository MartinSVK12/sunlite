using System.Runtime.InteropServices.JavaScript;
using System.Text;
using SunliteSharp.Core;
using SunliteSharp.Core.Compiler;
using SunliteSharp.Core.API;
using SunliteSharp.Core.AST;
using SunliteSharp.Core.Enum;
using SunliteSharp.Core.Modifier;
using SunliteSharp.Runtime;
using Type = SunliteSharp.Core.Type;

namespace SunliteSharp;

public class Sunlite(string[] args)
{
    public static Sunlite Instance = null!;
    
    public static bool Debug;
    public static bool LogToStdout;
    public static bool Tick;
    public static bool NoTypeChecks;
    public static bool BytecodeDebug;

    public VM? Vm; 
    public TypeCollector? Collector;
    public List<ConsoleOutputReceiver> OutputReceivers = [];
    public List<string> ScriptPaths = [];
    public Dictionary<string,(int,List<Stmt>)> Imports = new();
    public Func<string, string> ReadFunction = File.ReadAllText;
    public INatives Natives = new DefaultNatives();
    
    public bool Uninitialized = true;
    public bool HadError;
    public bool HadRuntimeError = false;
    
    private static void Main(string[] args)
    {
        LogToStdout = true;
        Instance = new Sunlite(args);
        Instance.Vm = Instance.Start();
    }

    public VM? Start()
    {

        Instance = this;
        switch (args.Length)
        {
            case > 4:
                Console.WriteLine("Usage: sunlite [script] (path) (options) (args)");
                Environment.Exit(64);
                return null;
            case 1:
                return RunFile(args[0]);
            case 2:
                if (args[0] == "debug")
                {
                    Console.WriteLine("Enter script name: ");
                    var path = Console.ReadLine();
                    Console.WriteLine();
                    ScriptPaths.AddRange(args[1].Split(';'));
                    Debug = true;
                    LogToStdout = true;
                    return RunFile(args[1].Split(';')[0]+path);
                }
                ScriptPaths.AddRange(args[1].Split(';'));
                return RunFile(args[0]);
            case 3 or 4:
                foreach (var s in args[2].Split(";"))
                {
                    switch (s)
                    {
                        case "debug":
                            Debug = true;
                            break;
                        case "bytecode-debug":
                            BytecodeDebug = true;
                            break;
                        case "stdout":
                            LogToStdout = true;
                            break;
                        case "tick":
                            Tick = true;
                            break;
                        case "no-type-checks":
                            NoTypeChecks = true;
                            break;
                    }
                }
                ScriptPaths.AddRange(args[1].Split(';'));
                return RunFile(args[0]);
            default:
                Console.WriteLine("Usage: sunlite [script] (path) (options) (args)");
                Environment.Exit(64);
                return null;
        }
    }

    private VM? RunFile(string path)
    {
        return RunString(ReadFunction(path), path);
    }

    private VM? RunString(string source, string path)
    {
        return RunVM(source, path);
    }

    private VM? RunVM(string source, string path)
    {
        var scanner = new Scanner(source, this);
        List<Token> tokens = scanner.ScanTokens(path);

        // Stop if there was a syntax error.
        if (HadError) return null;

        var parser = new Parser(tokens, this);
        List<Stmt> statements = parser.Parse(path);
        
        // Stop if there was a syntax error.
        if (HadError) return null;

        Vm = new VM(this, args.Length == 4 ? args[3].Split(';') : []);
        Uninitialized = false;
        
        Collector = new TypeCollector(this);
        Collector.Collect(statements, path);
        
        // Stop if there was a type collection error.
        if (HadError) return null;
        
        parser = new Parser(tokens, this);
        statements = parser.Parse(path);
        
        // Stop if there was a syntax error.
        if (HadError) return null;
        
        Collector = new TypeCollector(this);
        Collector.Collect(statements, path);
        
        // Stop if there was a type collection error.
        if (HadError) return null;
        
        parser = new Parser(tokens, this, true);
        statements = parser.Parse(path);
        
        // Stop if there was a syntax error.
        if (HadError) return null;
        
        if (Debug)
        {
            PrintInfo("Type Collection:");
            PrintInfo("--------");

            Collector?.TypeScopes?.ForEach(scope => PrintTypeScopes(scope, 0));

            PrintInfo("--------");
            PrintInfo();

            PrintInfo("Type Hierarchy:");
            PrintInfo("--------");

            if (Collector?.TypeHierarchy != null)
            {
                foreach ((var typeName, (string, List<string>, List<string>) value) in Collector.TypeHierarchy)
                {
                    (var superclass, List<string> interfaces, List<string> typeParams) = value;

                    var typeParamsStr = string.Join(", ", typeParams);
                    var interfacesStr = interfaces.Count > 0 ? string.Join(", ", interfaces) : "<nil>";

                    PrintInfo($"{typeName}<{typeParamsStr}> extends {superclass} implements {interfacesStr}");
                }
            }

            PrintInfo("--------");
            PrintInfo("--------");
            PrintInfo();
        }

        if (!NoTypeChecks)
        {
            var checker = new TypeChecker(this, Vm);
            checker.Check(statements, path);
        }
        
        var compiler = new Compiler(this, Vm);

        List<Stmt> allStatements = [];
        foreach (var item in Imports.Values.OrderBy(v => v.Item1))
        {
            allStatements.AddRange(item.Item2);
        }

        allStatements.AddRange(statements);

        var program = compiler.Compile(FunctionType.None, FunctionModifier.Normal, Type.Nil, [], [], allStatements, path);
        
        // Stop if there was a compilation error.
        if (HadError) return null;

        Vm.Call(new SLClosure(program, []),0,0);
        
        if (Tick) return Vm;
        
        try
        {
            Vm.Run();
        }
        catch (UnhandledVmException e)
        {
            Vm.PrintStackTrace(e.Message);
        }
        catch (Exception e)
        {
            Vm.PrintStackTrace($"Internal VM Error: {e}");
        }

        return null;

    }
    
    private void PrintTypeScopes(TypeCollector.Scope scope, int depth = 0)
    {

        var sb = new StringBuilder();
        sb.Append(new string('\t', depth));
        sb.Append($"{scope.Name.Lexeme} {{");

        foreach (var entry in scope.Contents)
        {
            sb.Append('\n');
            sb.Append(new string('\t', depth + 1));
            sb.Append($"{entry.Key.Lexeme}{entry.Value}");
        }

        PrintInfo(sb.ToString());

        foreach (var innerScope in scope.Inner)
        {
            PrintTypeScopes(innerScope, depth + 1);
        }

        sb.Clear();
        sb.Append(new string('\t', depth));
        sb.Append('}');
        PrintInfo(sb.ToString());
    }


    public void Error(string message, int line, string file)
    {
        ReportError(message, "", line, file);
    }

    public void Error(Token token, string message)
    {
        ReportError(message, token.Type == TokenType.Eof ? " at end" : $" at '{token.Lexeme}'", token.Line, token.File);
    }
    
    public void Warn(Token token, string message)
    {
        ReportWarn(message, token.Type == TokenType.Eof ? " at end" : $" at '{token.Lexeme}'", token.Line, token.File);
    }

    private void ReportError(string message, string where, int line, string file)
    {
        PrintErr($"[{file}, line {line}] Error{where}: {message}");
        //PrintErr(Environment.StackTrace);
        HadError = true;
    }

    private void ReportWarn(string message, string where, int line, string file)
    {
        PrintWarn($"[{file}, line {line}] Warn{where}: {message}");
        //PrintWarn(Environment.StackTrace);
        HadError = true;
    }
    
    public void PrintErr(string message = "")
    {
        if(LogToStdout) Console.Error.WriteLine(message);
        OutputReceivers.ForEach(obj => obj.Error(message));
    }
    
    public void PrintInfo(string message = "")
    {
        if(LogToStdout) Console.WriteLine(message);
        OutputReceivers.ForEach(obj => obj.Info(message));
    }
    
    public void PrintWarn(string message = "")
    {
        if(LogToStdout) Console.Error.WriteLine(message);
        OutputReceivers.ForEach(obj => obj.Warn(message));
    }
}