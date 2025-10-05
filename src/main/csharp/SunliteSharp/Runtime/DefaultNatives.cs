namespace SunliteSharp.Runtime;

using Type = SunliteSharp.Core.Type;

public record  DefaultNatives : INatives
{
    public void RegisterNatives(VM vm)
    {
        RegisterIO(vm);
        RegisterCore(vm);
    }

    private void RegisterIO(VM vm)
    {
        vm.DefineNative(new SLNativeFunction("print", Type.Nil, 1, (_, args) =>
        {
            vm.Sl.PrintInfo(args[0].Get().ToString());
            return SLNil.Nil;
        }));
    }

    private void RegisterCore(VM vm)
    {
        vm.DefineNative(new SLNativeFunction("str", Type.String, 1, (_, args) => new SLString(args[0].Get().ToString())));
        vm.DefineNative(new SLNativeFunction("clock", Type.Number, 0, (_, _) => new SLNumber(Environment.TickCount / 1000.0)));
        vm.DefineNative(new SLNativeFunction("array", Type.OfArray(Type.NullableAny),1,(_, args)=>
        {
            return new SLArrayObj(new SLArray((int)(args[0] as SLNumber).Value, vm.Sl));
        }));
        vm.DefineNative(new SLNativeFunction("table", Type.OfTable(Type.NullableAny,Type.NullableAny),0,(_, _)=> new SLTableObj(new SLTable(vm.Sl))));
        vm.DefineNative(new SLNativeFunction("launchArgs", Type.OfArray(Type.String), 0, (_, _) =>
        {
            return new SLArrayObj(new SLArray(vm.LaunchArgs.Length, vm.Sl).Overwrite(vm.LaunchArgs.Select(arg => new SLString(arg)).ToArray<AnySLValue>()));
        }));
        vm.DefineNative(new SLNativeFunction("exit", Type.Nil, 1, (_, args) =>
        {
            Environment.Exit((int)(args[0] as SLNumber).Value);
            return SLNil.Nil;
        }));
        vm.DefineNative(new SLNativeFunction("load", new Type.Union([Type.OfFunction("",Type.Nil,[]),Type.Nil]),1, (_, args) =>
        {
            var code = (args[0] as SLString).Value;
            return (AnySLValue?)vm.Load(code) ?? SLNil.Nil;
        }));
    }
}