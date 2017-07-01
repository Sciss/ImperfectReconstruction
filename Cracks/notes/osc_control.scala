val t = osc.UDP.Transmitter()
t.connect()
// val target: java.net.SocketAddress = "192.168.0.77" -> 57120
val target: java.net.SocketAddress = "192.168.0.24" -> 57120
t.send(osc.Message("/stage", 1), target)
t.send(osc.Message("/stage", 4), target)

t.send(osc.Message("/invalid", 1), target)

//////////////////

val target: java.net.SocketAddress = "192.168.0.11" -> 57120
val t = osc.UDP.Transmitter(target)
t.connect()
t ! osc.Message("/stage", 1)
t ! osc.Message("/stage", 4)

t ! osc.Message("/shell", "sudo", "shutdown", "now")


