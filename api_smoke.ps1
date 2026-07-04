$ErrorActionPreference = 'Continue'
$base = 'http://127.0.0.1:8080'
$jsonHeaders = @{ 'Content-Type' = 'application/json' }

function Hit {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers,
        $Body
    )
    try {
        if ($null -eq $Body) {
            $resp = Invoke-RestMethod -Uri $Url -Method $Method -Headers $Headers
        } else {
            $resp = Invoke-RestMethod -Uri $Url -Method $Method -Headers $Headers -Body ($Body | ConvertTo-Json -Depth 8)
        }
        Write-Output "$Name => code=$($resp.code),msg=$($resp.msg)"
        return $resp
    } catch {
        if ($_.Exception.Response) {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $txt = $sr.ReadToEnd()
            Write-Output "$Name => HTTP_ERR $txt"
        } else {
            Write-Output "$Name => EX $($_.Exception.Message)"
        }
        return $null
    }
}

$phone = '19' + (Get-Random -Minimum 100000000 -Maximum 999999999)
$pwd = 'Aa123456'

$reg = Hit -Name 'user.register' -Method 'Post' -Url "$base/user/register" -Headers $jsonHeaders -Body @{ phone = $phone; username = 'api_all'; password = $pwd }
$login = Hit -Name 'user.login' -Method 'Post' -Url "$base/user/login" -Headers $jsonHeaders -Body @{ phone = $phone; password = $pwd }
if ($null -eq $login -or [string]::IsNullOrWhiteSpace($login.data)) {
    Write-Output 'STOP: login failed'
    exit 0
}

$token = $login.data
$authHeaders = @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' }

$current = Hit -Name 'user.current' -Method 'Get' -Url "$base/user/current" -Headers $authHeaders -Body $null
if ($null -eq $current -or $null -eq $current.data) {
    Write-Output 'STOP: current failed'
    exit 0
}
$uid = $current.data.id

Hit -Name 'user.info' -Method 'Get' -Url "$base/user/info?id=$uid" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'user.follow.stat' -Method 'Get' -Url "$base/user/follow/stat?id=$uid" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'user.batch' -Method 'Get' -Url "$base/user/batch?ids=$uid" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'user.dashboard' -Method 'Get' -Url "$base/user/dashboard" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'trade.getRecommendedBooks' -Method 'Get' -Url "$base/trade/getRecommendedBooks" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'trade.getAllCart' -Method 'Get' -Url "$base/trade/getAllCart" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'trade.address.list' -Method 'Get' -Url "$base/trade/address/list" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'forum.getNewsList' -Method 'Get' -Url "$base/forum/getNewsList?limit=3" -Headers $jsonHeaders -Body $null | Out-Null
Hit -Name 'forum.getNewsDetail' -Method 'Get' -Url "$base/forum/getNewsDetail?id=1" -Headers $jsonHeaders -Body $null | Out-Null
Hit -Name 'forum.post.hot' -Method 'Get' -Url "$base/forum/post/hot?limit=3" -Headers $authHeaders -Body $null | Out-Null
Hit -Name 'forum.post.search' -Method 'Post' -Url "$base/forum/post/search" -Headers $authHeaders -Body @{ keyword = '测试'; pageNum = 1; pageSize = 5 } | Out-Null
Hit -Name 'user.logout' -Method 'Get' -Url "$base/user/logout" -Headers $authHeaders -Body $null | Out-Null
