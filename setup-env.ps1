# 在当前shell初始化环境变量 


$jsonObject = Get-Content -Raw "env.json" | ConvertFrom-Json -AsHashtable

# Loop through the JSON object and set the environment variables
foreach ($key in $jsonObject.keys ) {
    [Environment]::SetEnvironmentVariable($key, $jsonObject[$key])
}

