var exec = require('cordova/exec');

exports.open = function(success, error) {
    exec(success, error, 'ScreenToggle', 'open');
};

exports.close = function(success, error) {
    exec(success, error, 'ScreenToggle', 'close');
};

exports.enable = function(arg0, success, error) {
    exec(success, error, 'ScreenToggle', 'enable', [arg0]);
};

exports.disable = function(arg0, success, error) {
    exec(success, error, 'ScreenToggle', 'disable', [arg0]);
};

exports.config = function(opens, closes, success, error) {
    exec(success, error, 'ScreenToggle', 'config', [opens, closes]);
};

exports.sync = function(url, success, error) {
    exec(success, error, 'ScreenToggle', 'sync', [url]);
};