# cordova-plugin-toggle-screen

control screen with js in cordova and config the times for the screen toggle on timer

## onDeviceReady

```
if (cordova.plugins && cordova.plugins.ScreenToggle) {
cordova.plugins.ScreenToggle.enable();
}
```

# screenToggle check Time Config Per 30 Seconds

```
function screenToggle() {
    if (selectURL.value) {
        var shiftUrl = "http://" + selectURL.value + "/api/Shift/GetList";

        $.ajax({
            url: shiftUrl,
            type: "GET",
            timeout: 60000,
            dataType: "json",
            success: function (data) {
                Logger($("#log"), 'GetList' + JSON.stringify(data));
                var opens = data.Data.map(function (item) { return item.StartHour + ":" + item.StartMinute + ":00"; });
                var closes = data.Data.map(function (item) { return item.EndHour + ":" + item.EndMinute + ":00"; });

                if (cordova.plugins && cordova.plugins.ScreenToggle) {
                    cordova.plugins.ScreenToggle.config(opens, closes);
                }
            }
        });
    }
}
```