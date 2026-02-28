Option Explicit

Dim shell, fso
Set shell = CreateObject("WScript.Shell")
Set fso   = CreateObject("Scripting.FileSystemObject")

' Pasta onde está o VBS
Dim baseDir
baseDir = fso.GetParentFolderName(WScript.ScriptFullName)

' -------------------------
' Localização do Java
' -------------------------
Dim javaExe, javaHome
javaHome = shell.Environment("PROCESS").Item("JAVA_HOME")

If javaHome <> "" Then
    If Right(javaHome, 1) <> "\" Then
        javaHome = javaHome & "\"
    End If
    
    If InStr(LCase(javaHome), "\bin\") = 0 Then
        javaExe = javaHome & "bin\java.exe"
    Else
        javaExe = javaHome & "java.exe"
    End If
Else
    javaExe = "java"
End If

' -------------------------
' Comando Java (igual ao BAT)
' -------------------------
Dim command

command = """" & javaExe & """" & _
          " -splash:images/splashscreen.gif" & _
          " -Dfile.encoding=UTF-8" & _
          " -Djava.util.logging.manager=net.sf.l2jdev.log.AppLogManager" & _
          " -Xms1g -Xmx2g" & _
          " -jar "".\libs\L2FileEdit.jar"" -debug"

' -------------------------
' Executa com console visível
' -------------------------
Dim fullCmd
fullCmd = "cmd /k cd /d """ & baseDir & """ && title L2ClientDat Console && " & command

shell.Run fullCmd, 0, False