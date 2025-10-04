namespace SunliteSharp.Core.API;

public interface ConsoleOutputReceiver
{
    void Info(string message);
    void Warn(string message);
    void Error(string message);
}