Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path $PSScriptRoot -Parent
$logsDir = Join-Path $projectRoot 'logs\dev-runner'
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

Add-Type @"
using System;
using System.Diagnostics;
using System.Runtime.InteropServices;

public sealed class JobObject : IDisposable
{
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr CreateJobObject(IntPtr lpJobAttributes, string lpName);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool SetInformationJobObject(IntPtr hJob, int JobObjectInfoClass, IntPtr lpJobObjectInfo, uint cbJobObjectInfoLength);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool CloseHandle(IntPtr hObject);

    [StructLayout(LayoutKind.Sequential)]
    struct JOBOBJECT_BASIC_LIMIT_INFORMATION
    {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSetSize;
        public UIntPtr MaximumWorkingSetSize;
        public uint ActiveProcessLimit;
        public UIntPtr Affinity;
        public uint PriorityClass;
        public uint SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    struct IO_COUNTERS
    {
        public ulong ReadOperationCount;
        public ulong WriteOperationCount;
        public ulong OtherOperationCount;
        public ulong ReadTransferCount;
        public ulong WriteTransferCount;
        public ulong OtherTransferCount;
    }

    [StructLayout(LayoutKind.Sequential)]
    struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
    {
        public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
        public IO_COUNTERS IoInfo;
        public UIntPtr ProcessMemoryLimit;
        public UIntPtr JobMemoryLimit;
        public UIntPtr PeakProcessMemoryUsed;
        public UIntPtr PeakJobMemoryUsed;
    }

    private const int JobObjectExtendedLimitInformation = 9;
    private const uint JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000;

    private IntPtr _handle;

    public JobObject(string name)
    {
        _handle = CreateJobObject(IntPtr.Zero, name);
        if (_handle == IntPtr.Zero)
            throw new InvalidOperationException("CreateJobObject failed.");

        var info = new JOBOBJECT_EXTENDED_LIMIT_INFORMATION();
        info.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        int length = Marshal.SizeOf(info);
        IntPtr ptr = Marshal.AllocHGlobal(length);
        try
        {
            Marshal.StructureToPtr(info, ptr, false);
            if (!SetInformationJobObject(_handle, JobObjectExtendedLimitInformation, ptr, (uint)length))
                throw new InvalidOperationException("SetInformationJobObject failed.");
        }
        finally
        {
            Marshal.FreeHGlobal(ptr);
        }
    }

    public void AddProcess(Process process)
    {
        if (!AssignProcessToJobObject(_handle, process.Handle))
            throw new InvalidOperationException("AssignProcessToJobObject failed.");
    }

    public void Dispose()
    {
        if (_handle != IntPtr.Zero)
        {
            CloseHandle(_handle);
            _handle = IntPtr.Zero;
        }
    }
}
"@

function Write-Info([string]$message) {
    Write-Host "[INFO] $message" -ForegroundColor Cyan
}

function Write-Ok([string]$message) {
    Write-Host "[OK]   $message" -ForegroundColor Green
}

function Write-Warn([string]$message) {
    Write-Host "[WARN] $message" -ForegroundColor Yellow
}

function Resolve-MavenCmd {
    $candidates = New-Object System.Collections.Generic.List[string]

    $fromWhere = @(where.exe mvn.cmd 2>$null)
    if ($fromWhere.Count -gt 0) {
        $candidates.Add($fromWhere[0])
    }

    $candidates.Add('D:\maven\bin\mvn.cmd')
    $candidates.Add((Join-Path $env:USERPROFILE '.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd'))

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if ($candidate -and (Test-Path $candidate)) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw 'Maven not found. Checked PATH, D:\maven\bin\mvn.cmd, and Maven wrapper cache.'
}

function Stop-Port([int]$Port) {
    $pids = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    if ($pids.Count -eq 0) {
        Write-Host "  - Port $Port is free"
        return
    }

    foreach ($procId in $pids) {
        if ($procId -and $procId -ne $PID) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction Stop
                Write-Host "  - Closed PID $procId on port $Port"
            } catch {
                Write-Warn "Failed to close PID $procId on port ${Port}: $($_.Exception.Message)"
            }
        }
    }
}

function Wait-Port([int]$Port, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $listening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($listening) { return $true }
        Start-Sleep -Milliseconds 500
    }
    return $false
}

function Show-LogTail([string]$Title, [string]$Path) {
    if (Test-Path $Path) {
        Write-Host ""
        Write-Host "===== $Title =====" -ForegroundColor Yellow
        Get-Content $Path -Tail 40
        Write-Host "===================" -ForegroundColor Yellow
        Write-Host ""
    }
}

function Start-HiddenCmdProcess([string]$Name, [string]$WorkingDirectory, [string]$CommandLine, [JobObject]$Job) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'cmd.exe'
    $psi.Arguments = "/d /s /c `"$CommandLine`""
    $psi.WorkingDirectory = $WorkingDirectory
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $process = [System.Diagnostics.Process]::Start($psi)
    if (-not $process) {
        throw "Failed to start process: $Name"
    }

    $Job.AddProcess($process)
    return $process
}

$mavenCmd = Resolve-MavenCmd
$npmCmd = (where.exe npm.cmd 2>$null | Select-Object -First 1)
if (-not $npmCmd) {
    throw 'npm.cmd not found in PATH.'
}

$backendOut = Join-Path $logsDir 'backend.out.log'
$backendErr = Join-Path $logsDir 'backend.err.log'
$webOut = Join-Path $logsDir 'blog-web.out.log'
$webErr = Join-Path $logsDir 'blog-web.err.log'
$adminOut = Join-Path $logsDir 'blog-admin.out.log'
$adminErr = Join-Path $logsDir 'blog-admin.err.log'
Remove-Item $backendOut,$backendErr,$webOut,$webErr,$adminOut,$adminErr -Force -ErrorAction SilentlyContinue

$job = [JobObject]::new('personal-blog-dev')
$services = @()

try {
    Clear-Host
    Write-Host '========================================' -ForegroundColor White
    Write-Host '  Personal Blog - Dev Starter' -ForegroundColor White
    Write-Host '========================================' -ForegroundColor White
    Write-Host ''

    Write-Info 'Clearing occupied ports...'
    Stop-Port 8088
    Stop-Port 5173
    Stop-Port 5174
    Write-Host ''

    Write-Info "Maven: $mavenCmd"
    Write-Info "npm:   $npmCmd"
    Write-Host ''

    Write-Info 'Starting backend...'
    $backendCmd = ('call "{0}" spring-boot:run 1>> "{1}" 2>> "{2}"' -f $mavenCmd, $backendOut, $backendErr)
    $backend = Start-HiddenCmdProcess -Name 'backend' -WorkingDirectory (Join-Path $projectRoot 'blog-server') -CommandLine $backendCmd -Job $job
    $services += [pscustomobject]@{ Name='backend'; Process=$backend; Port=8088; Out=$backendOut; Err=$backendErr }

    if (-not (Wait-Port -Port 8088 -TimeoutSeconds 90)) {
        Show-LogTail -Title 'backend.err.log' -Path $backendErr
        Show-LogTail -Title 'backend.out.log' -Path $backendOut
        throw 'Backend failed to start on port 8088.'
    }
    Write-Ok 'Backend is up on 8088'

    Write-Info 'Starting blog web...'
    $webCmd = ('call "{0}" run dev 1>> "{1}" 2>> "{2}"' -f $npmCmd, $webOut, $webErr)
    $web = Start-HiddenCmdProcess -Name 'blog-web' -WorkingDirectory (Join-Path $projectRoot 'blog-web') -CommandLine $webCmd -Job $job
    $services += [pscustomobject]@{ Name='blog-web'; Process=$web; Port=5173; Out=$webOut; Err=$webErr }

    if (-not (Wait-Port -Port 5173 -TimeoutSeconds 60)) {
        Show-LogTail -Title 'blog-web.err.log' -Path $webErr
        Show-LogTail -Title 'blog-web.out.log' -Path $webOut
        throw 'Blog web failed to start on port 5173.'
    }
    Write-Ok 'Blog web is up on 5173'

    Write-Info 'Starting admin...'
    $adminCmd = ('call "{0}" run dev 1>> "{1}" 2>> "{2}"' -f $npmCmd, $adminOut, $adminErr)
    $admin = Start-HiddenCmdProcess -Name 'blog-admin' -WorkingDirectory (Join-Path $projectRoot 'blog-admin') -CommandLine $adminCmd -Job $job
    $services += [pscustomobject]@{ Name='blog-admin'; Process=$admin; Port=5174; Out=$adminOut; Err=$adminErr }

    if (-not (Wait-Port -Port 5174 -TimeoutSeconds 60)) {
        Show-LogTail -Title 'blog-admin.err.log' -Path $adminErr
        Show-LogTail -Title 'blog-admin.out.log' -Path $adminOut
        throw 'Blog admin failed to start on port 5174.'
    }
    Write-Ok 'Blog admin is up on 5174'

    Write-Host ''
    Write-Ok 'All services started successfully.'
    Write-Host '  Frontend: http://127.0.0.1:5173'
    Write-Host '  Admin:    http://127.0.0.1:5174'
    Write-Host '  Backend:  http://127.0.0.1:8088'
    Write-Host ''
    Write-Info 'Opening pages in browser...'
    Start-Process 'http://127.0.0.1:5173'
    Start-Sleep -Milliseconds 800
    Start-Process 'http://127.0.0.1:5174'
    Write-Host ''
    Write-Host 'This is the only control window.' -ForegroundColor Green
    Write-Host 'Close this window and all 3 services will stop together.' -ForegroundColor Green
    Write-Host ''
    Write-Host 'Logs:'
    Write-Host "  $logsDir"
    Write-Host ''

    while ($true) {
        Start-Sleep -Seconds 2
        foreach ($service in $services) {
            if ($service.Process.HasExited) {
                Show-LogTail -Title ($service.Name + '.err.log') -Path $service.Err
                Show-LogTail -Title ($service.Name + '.out.log') -Path $service.Out
                throw ("{0} exited unexpectedly with code {1}." -f $service.Name, $service.Process.ExitCode)
            }
        }
    }
}
finally {
    foreach ($service in $services) {
        try {
            if ($service.Process -and -not $service.Process.HasExited) {
                Stop-Process -Id $service.Process.Id -Force -ErrorAction SilentlyContinue
            }
        } catch {}
    }

    if ($job) {
        $job.Dispose()
    }
}
