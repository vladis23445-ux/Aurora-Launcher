#define AppName "Aurora Launcher"
#define AppVersion "1.1.0"
#define AppPublisher "Aurora Visuals"
#define AppExe "AuroraLauncher.exe"

[Setup]
AppId={{7F3B2C1A-9D44-4E51-8C3A-A1B2C3D4E5F6}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={localappdata}\Programs\AuroraLauncher
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputDir=..\launcher\dist
OutputBaseFilename=AuroraSetup-{#AppVersion}
SetupIconFile=..\launcher\src\app.ico
UninstallDisplayIcon={app}\{#AppExe}
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "russian"; MessagesFile: "compiler:Languages\Russian.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Создать ярлык на рабочем столе"; GroupDescription: "Ярлыки:"; Flags: checkedonce

[Files]
Source: "..\launcher\dist\{#AppExe}"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\launcher\logo.png"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\launcher\src\app.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"; IconFilename: "{app}\app.ico"
Name: "{group}\Удалить {#AppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; IconFilename: "{app}\app.ico"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "Запустить {#AppName}"; Flags: nowait postinstall skipifsilent