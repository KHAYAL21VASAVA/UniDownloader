$git = "C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\bin\git.exe"
$env:PATH = "C:\Program Files\Git\cmd;C:\Program Files\Git\bin;C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\bin;C:\Users\KHAYAL VASAVA\AppData\Roaming\MobaXterm\slash\mx86_64b\usr\bin;" + $env:PATH
& $git add .
& $git commit -m "Remove backend server modal button, streamline mobile navbar, and enhance cloud media extractor"
& $git log --oneline -n 3
