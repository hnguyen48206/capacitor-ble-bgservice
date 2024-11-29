# ble-srv

simple plugin for checing available devices all the time

## Install

```bash
npm install ble-srv
npx cap sync
```

## API

<docgen-index>

* [`startService()`](#startservice)
* [`stopService()`](#stopservice)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### startService()

```typescript
startService() => Promise<void>
```

--------------------


### stopService()

```typescript
stopService() => Promise<void>
```

--------------------

</docgen-api>


## (*) NOTE: các lưu ý khi sử dụng cho nghiệp vụ của walmartApp

```sh

1. Plugin sẽ dựa vào thông tin các key-value sau (trong app storage) để hoạt động:

key: MacBluetoothsConnected
value: "[{"mac":"78:02:B7:08:14:51", "deviceName":"K11", "vehicleID":"ABC","status":"on"}]"

+ Trong đó value là 1 mảng các thiết bị cần kiểm tra. List này sẽ do main app set xuống sẵn. 
+ Để tiện cho việc test, nếu service kiểm tra không tìm thấy key này thì sẽ add 1 mảng default
với 1 thiết bị test có MAC address là 78:02:B7:08:14:51
+ Lưu ý, String giá trị của MAC address luôn viết hoa.

key: BLEConfigs
value: {"scan_period":5000, "scan_delay":10000}

+ Trong đó scan_period là thời gian 1 lần scan và scan_delay là thời gian giữa các lần scan.

key: Vehicle_IsMoving
value: {"Vehicle_IsMoving": true}

+ Trong đó Vehicle_IsMoving là biến có giá trị do main app xác định. Nếu giá trị này false thì plugin sẽ bỏ qua bước scan trong cycle wakeup này. 

+ Mỗi khi main app cần thay đổi các giá trị key-value trên thì cần gọi stopService trước. Sau khi update thì startService lại.

2. Khi plugin kiểm tra thì sẽ set status on/off lại cho các thiết bị trong list và cập nhật giá trị mới cho key MacBluetoothsConnected.

3. Vấn đề về Permissions:

    a. Android:
    + Hỗ trợ Android OS >= 10
    + Mặc định, trong plugin sẽ không request các quyền runtime (danh sách bên dưới). Do đó, main app cần bảo đảm có đủ quyền trước khi gọi method startService. Main app không cần khai báo Manifest do plugin đã đăng ký sẵn. Nếu plugin check không đủ quyền thì đồng nghĩa service bị cancel (có khả năng crash trong bản beta).

        android.permission.BLUETOOTH_SCAN: Required for scanning for Bluetooth devices.
        android.permission.BLUETOOTH_CONNECT: Required for connecting to Bluetooth devices.
        android.permission.ACCESS_FINE_LOCATION: Required for accessing precise location.
        android.permission.ACCESS_COARSE_LOCATION: Required for accessing approximate location.
        (BLUETOOTH_ADMIN và BLUETOOTH cũng cần cho OS<12)

    + Trên một số thiêt bị, có các tuỳ chọn riêng của hãng liên quan đến việc hạn chế hoạt động của các app background. Ví dụ 'Pause App Activity If Unused'. Nên main app cần hướng dẫn người dùng disable tất cả các hạn chế này thủ công trong setting. 
    
    b. iOS <IN PROGRESS - NOT READY>
        + Từ xcode, kích hoạt quyền background mode từ Capabilities của main app. Chọn Uses Bluetooth LE accessories + Background fetch + Background processing
        + ...

4. Sample app tích hợp: https://github.com/hnguyen48206/capcitor-seven-zip-example-app/tree/bleserv (tham khảo cách sử dụng ở đây). Nhánh bleserv.

```