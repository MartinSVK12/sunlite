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
            vm.sunlite.PrintInfo(args[0].Get().ToString());
            return SLNil.Nil;
        }));
    }

    private void RegisterCore(VM vm)
    {
        vm.DefineNative(new SLNativeFunction("str", Type.String, 1, (_, args) => new SLString(args[0].Get().ToString())));
        vm.DefineNative(new SLNativeFunction("clock", Type.Number, 0, (_, args) => new SLNumber(Environment.TickCount / 1000.0)));
    }
}