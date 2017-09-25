# README - imperfect-raspikeys

Note: this can be started through `imperfect-raspiplayer` when using
the `--key-shutdown` or `--key-reboot` or `--button-shutdown` or `--button-reboot` switches. 
This program must run
as `sudo` to configure the GPIO pins. It assumes a SunFounder keypad
matrix is attached to the GPIO.

When the shutdown or reboot keys/buttons are pressed, OSC commands
`[/forward, /shutdown]` and `[/forward, /reboot]` are sent to the
assumed control (192.168.0.11, port 57110).