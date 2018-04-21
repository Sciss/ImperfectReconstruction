# DifferenceProcess

LED:

- black wire to ground; e.g. from "top-left", 5th pin
- red wire is green light, WiringPi pin0; from top-left, 6th pin ("11")
- yellow wire is red light, WiringPi pin1; from top-right, 6th pin ("12")

Key matrix:

?

Buttons:

- brown wire (common) to header 17 (9th pin from top-left)
- yellow wire (green button) to header 16 (8th pin from top-right)
- orange wire (red button) to header 18 (9th pin from top-right)

Press green button to start recording, red button to stop recording.
Both buttons to shuts down.

After boot, LED pulses green to signalise "ready". Press green button. LED pulses red to signalise "recording".
Press red button, LED pulses green to signalise "ready".