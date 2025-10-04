using System.Drawing;

namespace SunliteSharp.Runtime;

public class SLArray(int size, Sunlite sl)
{
    private AnySLValue[] _values = VM.ArrayOfNils(size);

    public AnySLValue this[int index]
    {
        get
        {
            if (index < size) return _values[index];
            sl.Vm.RuntimeError($"Array index {index} is out of bounds for array of size {size}.");
            return SLNil.Nil;
        }
        set
        {
            if (index < size) _values[index] = value;
            sl.Vm.RuntimeError($"Array index {index} is out of bounds for array of size {size}.");
        }
    }
    
    public AnySLValue[] Internal => _values;

    public void Resize(int newSize)
    {
        Array.Resize(ref _values, newSize);
        _values = _values.Concat(VM.ArrayOfNils(newSize - size)).ToArray();
    }

    public SLArray Overwrite(AnySLValue[] arr)
    {
        _values = arr;
        return this;
    }

    public SLArray Copy()
    {
        return new SLArray(size, sl).Overwrite(_values.Select(v => v.Copy()).ToArray());
    }

    public override string ToString()
    {
        return $"<array of size {size}>";
    }
}