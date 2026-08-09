$git = "C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\bin\git.exe"
Remove-Item -Force "git_init.ps1" -ErrorAction SilentlyContinue
& $git add .
& $git commit -m "Clean working tree"
& $git log --oneline -n 3
