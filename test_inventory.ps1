$token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTc1MjU1NDU0NywiZXhwIjoxNzUyNTk3NzQ3fQ.zU86_dp8ClFFAvQmmaRHE-ykvCl1Jh_qnt1OSG8IZ2FHoXORi-MFJGoRBDiHTomG7AGnptWvXF89Cnup6nybLQ"

try {
    $headers = @{
        'Content-Type' = 'application/json'
        'Authorization' = "Bearer $token"
    }
    
    $body = @{
        productId = "test-product-docker-123"
        quantity = 100
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/v1/inventory' -Method POST -Headers $headers -Body $body -ErrorAction Stop
    
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Response: $($_.Exception.Response)"
}