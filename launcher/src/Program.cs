using AuroraLauncher.UI;

namespace AuroraLauncher;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.SetUnhandledExceptionMode(UnhandledExceptionMode.CatchException);
        Application.ThreadException += (_, e) => MessageBox.Show(e.Exception.ToString(), "Aurora Launcher",
            MessageBoxButtons.OK, MessageBoxIcon.Error);

        Application.Run(new MainForm());
    }
}
