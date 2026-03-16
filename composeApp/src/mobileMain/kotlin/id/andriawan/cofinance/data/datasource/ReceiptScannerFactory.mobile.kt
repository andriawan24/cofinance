package id.andriawan.cofinance.data.datasource

actual fun createReceiptScanner(): ReceiptScannerService = CactusReceiptScanner()
