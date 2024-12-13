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

## (\*) NOTE: Lưu ý kéo thanh ngang của git khi tham khảo để không sót nội dung.

```sh

1. Plugin sẽ dựa vào thông tin các key-value sau (trong app storage) để hoạt động:

key: MacBluetoothsConnected
value: "[{"mac":"78:02:B7:08:14:51", "deviceName":"K11", "vehicleID":"ABC","status":"on","isAutoConnect":true}]"

+ Trong đó value là 1 mảng các thiết bị cần kiểm tra. List này sẽ do main app set xuống sẵn.
+ Để tiện cho việc test, nếu service kiểm tra không tìm thấy key này thì sẽ add 1 mảng default
với 1 thiết bị test có MAC address là 78:02:B7:08:14:51
+ Lưu ý, String giá trị của MAC address luôn viết hoa.

key: BLEConfigs
value: {"scan_period":5000, "scan_delay":10000, "isTesting": true, "connect_delay":900000}

+ Trong đó scan_period là thời gian 1 lần scan và scan_delay là thời gian giữa các lần scan. Đơn vị miliseconds. isTesting là cờ bật/tắt log console ở native. connect_delay là key set thời gian delay việc connect đến thiết bị
sau khi đã phát hiện ra trạng thái 'on'

key: Vehicle_IsMoving
value: {"Vehicle_IsMoving": true}

+ Trong đó Vehicle_IsMoving là biến có giá trị do main app xác định. Nếu giá trị này false thì plugin sẽ bỏ qua bước scan trong cycle wakeup này.

2. Khi plugin kiểm tra thì sẽ set status on/off lại cho các thiết bị trong list và cập nhật giá trị mới cho key MacBluetoothsConnected. Mỗi khi main app cần thay đổi các giá trị key-value trên thì cần gọi stopService trước. Sau khi update thì startService lại.

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

    b. iOS:
        + Từ xcode, kích hoạt quyền background mode từ Capabilities của main app. Chọn 
        Uses Bluetooth LE accessories + Background processing + Act as a BLE accessories + Location Update + Background fetch
        + Trong info.plist, khai báo các key sau:

            - Required background modes <Array> gồm:
            App communicates using CoreBluetooth
            App processes data in the background
            App shares data using CoreBluetooth
            App registers for location updates

            - Permitted background task scheduler identifiers <Array> gồm:
            com.hnguyen48206.blesrv

            - Privacy - Bluetooth Always Usage Description
            - Privacy - Bluetooth Peripheral Usage Description
            - Privacy - Location Always Usage Description
            - Privacy - Location Always and When In Use Usage Description
            - Privacy - Location When In Use Usage Description

        + Import các files BackgroundTimer.swift và BLEManager.swift vào main App (lưu ý phải dùng tính năng import từ xcode, không phải copy thủ công). Các file này lấy từ git repo của app sample.
        + Điều chỉnh APPDelagate.swift của Main App cho phù hợp với APPDelagate.swift của app sample. 
        Lưu ý merge cả 2 với nhau. Không phải dùng hẳn 1 bên nào. 
        + Sau khi app đã cài trên thiết bị. Bảo đảm app đã được cấp quyền 'Background App Refresh' trong app settings.

4. Sample app tích hợp: https://github.com/hnguyen48206/capcitor-seven-zip-example-app/tree/bleserv (tham khảo cách sử dụng ở đây). (nhánh bleserv-in-app-gps).
Lưu ý sử dụng đúng nhánh. 

5. <IOS only> Scan Log History:
Plugin sẽ lưu lại lịch sử các lần scan thành công vào localstorage của ứng dụng với key "scanHistoryLog"
Sau khi main app get lên thì có thể parse giá trị string thành array để hiển thị, tham khảo nhánh bleserv-in-app-gps
của app sample.
```
